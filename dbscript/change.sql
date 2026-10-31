-- ============================================================================
-- JsPlus-LogReplaced database initialization script
-- ----------------------------------------------------------------------------
-- Main DB: logdb (jdbc:h2:tcp://localhost:3308/logdb, config/Appenv.properties H2_PORT)
-- Run:     JsPlus/bin/StartH2Console.bat init          (Windows)
--          JsPlus/bin/StartH2Console.sh init           (Linux/macOS)
--          (stop the H2 server first - init opens the DB file directly;
--           fallback.sql -> change.sql, both idempotent, re-running is safe)
-- Tables:  ghost_remote_command - GhostConsole runtime remote-command queue (do NOT drop)
--          app_logger_config  - per-logger configuration (Logger.properties)
--          app_logger_msg     - the log records written into the database
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Application DB account (created if not exists)
-- sample_user / sample_password are the shipped sample credentials
-- (also used by config/DataSource.properties and config/GhostDataSource.properties).
-- ----------------------------------------------------------------------------
CREATE USER IF NOT EXISTS sample_user PASSWORD 'sample_password';

-- Grant all privileges on PUBLIC schema to sample_user
GRANT ALL ON SCHEMA PUBLIC TO sample_user;
-- Promote sample_user to ADMIN
ALTER USER sample_user ADMIN TRUE;

-- ============================================================================
-- Table 0: ghost_remote_command (ghost remote command queue)
-- Purpose: holds remote command rows for ghost consoles (headless runtimes),
--          used for cross-process / remote control.
-- NOTE: IF NOT EXISTS on purpose. This table belongs to the runtime
--       (GhostConsole); tests must NEVER drop/recreate it (a running
--       GhostConsole would hit "table not found" mid-run). init only
--       ensures it exists. Mirrored from JsPlus-Core/dbscript/change.sql.
-- ============================================================================
CREATE TABLE IF NOT EXISTS ghost_remote_command (
    id CHAR(16) PRIMARY KEY,        -- PK: unique command record id
    ghost_name VARCHAR(200),        -- Ghost name: which ghost console owns the command
    request_script VARCHAR(500),    -- Script to be executed by the ghost
    response_message VARCHAR(500),  -- Text returned by the ghost after execution
    processed INT,                  -- Processed flag: 0=pending, 1=processed
    requested INT,                  -- Request counter
    responsed INT,                  -- Response counter
    created_at TIMESTAMP,           -- Creation time
    updated_at TIMESTAMP,           -- Last update time
    deleted_at TIMESTAMP,           -- Soft-delete time (NULL when active)
    deleted SMALLINT                -- Soft-delete flag: 0=active, 1=deleted
);

-- ============================================================================
-- Table 1: app_logger_config (per-host logger overrides)
-- Purpose: the table behind database-out remote config. When DatabaseOut is
--          enabled and DatabaseOutConfigFrom is set, LogRecord.
--          writeLogBatchToDatabase() executes that statement once per
--          DatabaseOutRefreshConfigTimer and reads the FIRST row it returns.
--          The engine accepts exactly three keys from that row:
--            level                    -> Logger.setLevel(level)
--            class_name_enable_regex  -> Logger.setClassNameEnableRegex(...)
--            class_name_disable_regex -> Logger.setClassNameDisableRegex(...)
--          (LogRecord.java, writeLogBatchToDatabase: remoteConfig.containsKey(
--          "level") / "class_name_enable_regex" / "class_name_disable_regex").
--          No other engine component reads this table, so the columns below
--          are the complete supported set - there is no per-logger,
--          per-app or per-output-target override in the engine.
--
--          Column names MUST equal those literal keys. Every result column
--          label is lower-cased on the way out
--          (DriverExecutor.COLUMN_NAME_CASE_MODE = COLUMN_NAME_CASE_LOWER) and
--          the keys are looked up with Map.containsKey("level"), so a column
--          called log_level would be accepted by SQL and silently ignored by
--          the engine.
--
--          ----------------------------------------------------------------
--          OPERATIONAL NOTES (each verified against this logdb, not assumed)
--
--          * level accepts DEBUG / INFO / WARN / ERROR only. Logger.LEVEL_LIST
--            is exactly those four, so level='MARK' fails Logger.checkLevel and
--            is silently ignored - the effective level then falls back to
--            Logger.properties. Verified: a row with level='MARK' produced
--            exactly the same output as having no row at all.
--
--          * level does NOT gate MARK. logger.mark() is a separate branch, so
--            level='ERROR' still let all MARK records through while INFO and
--            WARN were dropped (4 MARK rows landed). To silence MARK, use a
--            class regex, not level.
--
--          * both regex columns are applied with Java String.matches(), i.e.
--            the WHOLE class name has to match the pattern - this is not a
--            substring search. '^com.killcoding.probe' does nothing to
--            'com.killcoding.probe.CfgProbe': verified, 16 of 16 rows still
--            written, no error and no warning. Write
--            '^com\.killcoding\.probe.*' instead (escape the dots, end with
--            .* ), which silenced both the database and the file log completely.
--            A wrongly anchored pattern fails silently, which is the worst
--            failure mode this table has.
--
--          * an empty or NULL level / regex simply leaves the matching
--            Logger property in force.
--
--          Rows are located by hostname: the engine passes :hostname
--          (CommonTools.getHostname(), e.g. HKTRPC001) as the ONLY SQL
--          parameter, so DatabaseOutConfigFrom is expected to look like
--            SELECT level, class_name_enable_regex, class_name_disable_regex
--              FROM app_logger_config WHERE hostname = :hostname AND deleted = FALSE
--          (parameter syntax is ':column_name', see DriverExecutor.first()).
-- ============================================================================
CREATE TABLE IF NOT EXISTS app_logger_config (
    id                      CHAR(16) PRIMARY KEY,  -- PK: unique config record id
    hostname                VARCHAR(100),          -- Host the row applies to (:hostname)
    level                   VARCHAR(20),           -- DEBUG / INFO / WARN / ERROR (NOT MARK, see notes above)
    class_name_enable_regex VARCHAR(500),          -- Only these class names are logged
    class_name_disable_regex VARCHAR(500),         -- These class names are skipped
    created_at              TIMESTAMP,             -- Creation time
    updated_at              TIMESTAMP,             -- Last update time
    deleted_at              TIMESTAMP,             -- Soft-delete time (NULL when active)
    deleted                 BOOLEAN                -- Soft-delete flag: false=active, true=deleted
);
CREATE INDEX IF NOT EXISTS idx_alc_hostname ON app_logger_config (hostname);
CREATE INDEX IF NOT EXISTS idx_alc_deleted  ON app_logger_config (deleted);

-- ============================================================================
-- Table 2: app_logger_msg (log records)
-- Purpose: one row per log event, so a log line can be queried like data.
--          Column names follow the placeholders of MessageFormat
--          (":level :log_time_format [:hostname-:log_thread_name-:log_thread_id]
--            :log_class_name **:message** \r\n:stack_trace"):
--            log_level   <- :level            (DEBUG / INFO / WARN / ERROR / MARK)
--            log_time    <- :log_time_format
--            hostname    <- :hostname
--            thread_name <- :log_thread_name
--            thread_id   <- :log_thread_id
--            logger_name <- :log_class_name
--            message     <- :message          (VARCHAR(2000), capped engine-side)
--            stack_trace <- :stack_trace      (VARCHAR(2000), capped engine-side)
--          message / stack_trace are VARCHAR(2000). H2 rejects anything longer
--          with "Value too long for column ... [22001-240]", and one rejected
--          row aborts the whole JDBC batch, so DatabaseOutInsertExecute caps
--          both values with LEFT(:message, 2000) / LEFT(:stack_trace, 2000) -
--          the first 2000 characters reach the database and the untruncated
--          text stays in the file log. Measured on this logdb: a 2020-character
--          payload already fails, while the load suite writes 8000-character
--          messages and multi-frame stack traces (13335 characters observed).
--          Do not shrink this to fit; either keep the cap in the INSERT or
--          widen BOTH the column and the cap together.
-- ============================================================================
CREATE TABLE IF NOT EXISTS app_logger_msg (
    id              CHAR(16) PRIMARY KEY,  -- PK: unique log record id
    app_name        VARCHAR(200),          -- Application name (SystemName)
    logger_name     VARCHAR(300),          -- Logger / class name (:log_class_name)
    log_level       VARCHAR(20),           -- DEBUG / INFO / WARN / ERROR / MARK
    log_time        TIMESTAMP,             -- Log occurrence time (:log_time_format)
    hostname        VARCHAR(100),          -- Host name (:hostname)
    thread_name     VARCHAR(200),          -- Thread name (:log_thread_name)
    thread_id       VARCHAR(50),           -- Thread id (:log_thread_id)
    message         VARCHAR(2000),         -- Log message body (:message)
    stack_trace     VARCHAR(2000),         -- Throwable stack trace (:stack_trace)
    created_at      TIMESTAMP,             -- Creation time (usually == log_time)
    updated_at      TIMESTAMP,             -- Last update time
    deleted_at      TIMESTAMP,             -- Soft-delete time (NULL when active)
    deleted         BOOLEAN                -- Soft-delete flag: false=active, true=deleted
);
CREATE INDEX IF NOT EXISTS idx_alm_time    ON app_logger_msg (log_time);
CREATE INDEX IF NOT EXISTS idx_alm_level   ON app_logger_msg (log_level);
CREATE INDEX IF NOT EXISTS idx_alm_logger  ON app_logger_msg (logger_name);
CREATE INDEX IF NOT EXISTS idx_alm_app     ON app_logger_msg (app_name);
CREATE INDEX IF NOT EXISTS idx_alm_deleted ON app_logger_msg (deleted);
