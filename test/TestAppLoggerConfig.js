// ============================================================================
// JsPlus-LogReplaced TestAppLoggerConfig - app_logger_config remote level suite
// ----------------------------------------------------------------------------
// Run      : test/TestAppLoggerConfig.bat / .sh (thin wrapper over _RunOne),
//              or inside the JsPlus console:
//              Console.eval(<PROJECT_ROOT>/test/TestAppLoggerConfig.js)
// Harness  : Shared plumbing lives in TestBase.js and is loaded below.
// Output   : <PROJECT_ROOT>/docs/latest/report/TestAppLoggerConfig-Report.html / .md
// Coverage : the DatabaseOut remote-config path
//            - the engine re-reads DatabaseOutConfigFrom at most every
//              DatabaseOutRefreshConfigTimer (code default 60000 ms) and applies
//              the row's "level" through Logger.setLevel, so writing (or updating)
//              this per-host row must flip the probes and gate the real sink.
//            - level=DEBUG   -> probe on, debug reaches logs/app/*.log
//            - level=WARN    -> warn lands, debug filtered
//            - level=INFO    -> restored so every other suite keeps its
//                               Logger.properties Level=INFO assumption
//            The row itself is KEPT: test data is never deleted (project rule).
// PREREQ   : H2 must be listening on :3308 with the logdb database loaded
//            (JsPlus/bin/BG_StartH2.bat, driven by _RunOne.bat Step 1).
//            The hostname is taken from app_logger_msg, so the engine must have
//            persisted at least one log row before this suite runs - that is why
//            TestAll.js places it after the API suites.
// NOTE     : the SQL below is carried over verbatim from TestSlf4j.js Case S15
//            so the behaviour that was already verified does not drift when the
//            case moved out. Timestamps come from the Java side (java.sql.Timestamp)
//            rather than the database's CURRENT_TIMESTAMP, per project rule.
// ============================================================================

var System = Java.type("java.lang.System");
var Thread = Java.type("java.lang.Thread");
var DiskFile = Java.type("com.killcoding.file.DiskFile");
var LogRecord = Java.type("com.killcoding.log.LogRecord");
var Timestamp = Java.type("java.sql.Timestamp");
// JsString (com.killcoding.jsplus.JsString) is the project rule for GraalVM string
// interop: java.lang.String crossing the language boundary becomes a JS primitive,
// so the sink text is always handled through JsString (Java indexOf/split), which
// also keeps every regex operation inside java.util.regex.
var JsString = Java.type("com.killcoding.jsplus.JsString");

// SQL literal for "now" - java.sql.Timestamp.toString() yields
// yyyy-MM-dd HH:mm:ss.SSS which H2 binds straight into a TIMESTAMP column.
function nowTs() {
    return "" + new Timestamp(System.currentTimeMillis());
}

var basePath = System.getProperty("JS_PLUS_PATH");
try {
    if (basePath === null || basePath === undefined || basePath.length === 0) {
        throw "JS_PLUS_PATH is not set - cannot locate TestBase.js";
    }
    load(basePath + "../test/TestBase.js");
} catch (e) {
    print("[ERROR] Failed to load TestBase.js: " + e);
    throw e;
}
beginTest();

var REPORT_NAME = "TestAppLoggerConfig-Report.html";
var REPORT_TITLE = "JsPlus-LogReplaced app_logger_config Test Report";

var LoggerFactory = Java.type("com.killcoding.log.LoggerFactory");

// Returns the newest app-log line that contains the tag, optionally restricted
// to a level prefix ("ERROR " etc.). Polls up to `tries` x 500ms because the
// engine flushes asynchronously (OutLogTimer=1s).
function appLogLine(tag, levelPrefix, tries) {
    var attempts = (tries === undefined || tries === null) ? 12 : tries;
    var logPath = basePath + "../logs/app/" + LogRecord.getLogFileName();
    for (var i = 0; i < attempts; i++) {
        var file = new DiskFile(logPath);
        if (file.exists()) {
            var content = JsString.valueOf(file.readAllString("UTF-8"));
            if (content.indexOf(tag) >= 0) {
                var lines = content.split("\n");
                for (var j = lines.length - 1; j >= 0; j--) {
                    var line = "" + lines[j];
                    if (line.indexOf(tag) < 0) {
                        continue;
                    }
                    if (levelPrefix === null || levelPrefix === undefined || line.startsWith(levelPrefix)) {
                        return line;
                    }
                }
            }
        }
        Thread.sleep(500);
    }
    return null;
}

function appLogContains(text, tries) {
    var attempts = (tries === undefined || tries === null) ? 12 : tries;
    var logPath = basePath + "../logs/app/" + LogRecord.getLogFileName();
    for (var i = 0; i < attempts; i++) {
        var file = new DiskFile(logPath);
        if (file.exists()) {
            if (JsString.valueOf(file.readAllString("UTF-8")).indexOf(text) >= 0) {
                return true;
            }
        }
        Thread.sleep(500);
    }
    return false;
}

function dbUpdate(sql) {
    var conn = null;
    var stmt = null;
    try {
        conn = Console.getDataSource().getConnection();
        stmt = conn.createStatement();
        stmt.executeUpdate(sql);
        return true;
    } catch (e) {
        print("[WARN] dbUpdate failed: " + sql + " -> " + e);
        return false;
    } finally {
        if (stmt !== null && stmt !== undefined) { try { stmt.close(); } catch (e2) {} }
        if (conn !== null && conn !== undefined) { try { conn.close(); } catch (e3) {} }
    }
}

function dbScalar(sql) {
    var conn = null;
    var stmt = null;
    var rs = null;
    try {
        conn = Console.getDataSource().getConnection();
        stmt = conn.createStatement();
        rs = stmt.executeQuery(sql);
        if (rs.next()) {
            var v = rs.getObject(1);
            return v === null || v === undefined ? null : ("" + v);
        }
        return null;
    } catch (e) {
        print("[WARN] dbScalar failed: " + sql + " -> " + e);
        return null;
    } finally {
        if (rs !== null && rs !== undefined) { try { rs.close(); } catch (e2) {} }
        if (stmt !== null && stmt !== undefined) { try { stmt.close(); } catch (e3) {} }
        if (conn !== null && conn !== undefined) { try { conn.close(); } catch (e4) {} }
    }
}

function waitFor(pred, triesMs) {
    // Poll until pred() is true or the window elapses. The remote-config
    // refresh runs at most once per DatabaseOutRefreshConfigTimer (60 s), so
    // the default window covers one full refresh plus flush slack.
    var waited = 0;
    while (waited < triesMs) {
        if (pred()) {
            return true;
        }
        Thread.sleep(500);
        waited += 500;
    }
    return pred();
}

print("=== JsPlus-LogReplaced app_logger_config Test ===");
print("JS_PLUS_PATH: " + System.getProperty("JS_PLUS_PATH"));

var tag = "REPL-CFG-" + System.nanoTime();
var log = LoggerFactory.getLogger("test.replacement.apploggerconfig");

// ---- Resolve the per-host row -------------------------------------------
// The engine persists rows with the runtime hostname, so app_logger_msg is the
// authoritative source for the hostname this suite must address. The row is
// reused when present (idempotent across runs) and created otherwise; it is
// never deleted.
var cfgHost = dbScalar("SELECT DISTINCT hostname FROM app_logger_msg WHERE hostname IS NOT NULL LIMIT 1");
var cfgRow = null;
var cfgSqlRow = "";

// Case C1
if (cfgHost === null || cfgHost === undefined || cfgHost.length === 0) {
    check({
        name: "app_logger_config per-host row is addressable",
        ok: false,
        detail: "no hostname found in app_logger_msg - cannot address the per-host config row",
        objective: "Verify the runtime hostname can be resolved so its app_logger_config row is addressable",
        objectiveZh: "驗證可解析執行期 hostname，從而定位其 app_logger_config 資料行",
        command: "SELECT DISTINCT hostname FROM app_logger_msg"
    });
} else {
    cfgSqlRow = "SELECT hostname, level, deleted FROM app_logger_config WHERE hostname = '" + cfgHost + "'";
    cfgRow = dbScalar("SELECT id FROM app_logger_config WHERE hostname = '" + cfgHost + "' AND deleted = FALSE LIMIT 1");
    var created = false;
    if (cfgRow === null || cfgRow === undefined) {
        cfgRow = ("cfg" + System.nanoTime()).substring(0, 16);
        var now0 = nowTs();
        created = dbUpdate("INSERT INTO app_logger_config (id, hostname, level, created_at, updated_at, deleted) VALUES ('"
            + cfgRow + "', '" + cfgHost + "', 'INFO', '" + now0 + "', '" + now0 + "', FALSE)");
        cfgRow = created ? cfgRow : null;
    }
    dbVerify(function () { return cfgSqlRow; });
    check({
        name: "app_logger_config per-host row is addressable",
        ok: cfgRow !== null && cfgRow !== undefined,
        detail: "host=" + cfgHost + ", cfgRow=" + (cfgRow === null ? "null (insert failed)" : cfgRow)
            + ", reused=" + (!created),
        objective: "Verify an active app_logger_config row exists for the runtime hostname (created once, then reused)",
        objectiveZh: "驗證執行期 hostname 有可用的 app_logger_config 資料行（首次建立、其後重用）",
        command: "SELECT id FROM app_logger_config WHERE hostname = '" + cfgHost + "' AND deleted = FALSE"
    });
}

// Case C2
// Phase A: level=DEBUG must be absorbed by the remote-config refresh.
if (cfgHost !== null && cfgHost !== undefined && cfgHost.length > 0 && cfgRow !== null) {
    dbUpdate("UPDATE app_logger_config SET level = 'DEBUG', updated_at = '" + nowTs() + "', deleted = FALSE "
        + "WHERE hostname = '" + cfgHost + "' AND deleted = FALSE");
    var cfgDebugProbe = waitFor(function () { return log.isDebugEnabled(); }, 70000);
    var cfgDebugLine = null;
    if (cfgDebugProbe) {
        log.debug("CFG " + tag + " remote-debug");
        cfgDebugLine = appLogLine("CFG " + tag, "DEBUG ");
    }
    dbVerify(function () { return cfgSqlRow; });
    check({
        name: "app_logger_config remote level DEBUG is applied",
        ok: cfgDebugProbe && cfgDebugLine !== null,
        detail: "isDebugEnabled=" + cfgDebugProbe + ", debug line=" + (cfgDebugLine === null ? "null" : "written")
            + (cfgDebugProbe ? "" : " (remote refresh not observed within 70s)"),
        objective: "Verify a DEBUG level row in app_logger_config enables debug probing and writing",
        objectiveZh: "驗證 app_logger_config 的 DEBUG 級別令 debug 探測與寫入均生效",
        command: "UPDATE app_logger_config SET level='DEBUG' -> isDebugEnabled() && log.debug() reaches sink"
    });

    // Case C3
    // Phase B: update to WARN -> warn lands, debug is filtered.
    var cfgWarnUpd = dbUpdate("UPDATE app_logger_config SET level = 'WARN', updated_at = '" + nowTs() + "' "
        + "WHERE hostname = '" + cfgHost + "' AND deleted = FALSE");
    var cfgWarnProbe = waitFor(function () { return log.isWarnEnabled() && !log.isInfoEnabled() && !log.isDebugEnabled(); }, 70000);
    var cfgWarnLine = null;
    var cfgLeak = false;
    if (cfgWarnProbe) {
        log.warn("CFG " + tag + " remote-warn");
        cfgWarnLine = appLogLine("CFG " + tag, "WARN ");
        log.debug("CFG " + tag + " remote-debug-filtered");
        cfgLeak = appLogContains("CFG " + tag + " remote-debug-filtered", 6);
    }
    dbVerify(function () { return cfgSqlRow; });
    check({
        name: "app_logger_config level update to WARN is applied",
        ok: cfgWarnUpd && cfgWarnProbe && cfgWarnLine !== null && !cfgLeak,
        detail: "isWarnEnabled=" + cfgWarnProbe + ", warn line=" + (cfgWarnLine === null ? "null" : "written")
            + ", debug leaked=" + cfgLeak + (cfgWarnProbe ? "" : " (remote refresh not observed within 70s)"),
        objective: "Verify updating the row's level to WARN re-gates probes and filtering",
        objectiveZh: "驗證把 level 更新為 WARN 後探測與過濾即時跟隨",
        command: "UPDATE app_logger_config SET level='WARN' -> isWarnEnabled() && debug filtered"
    });

    // Case C4
    // Phase C (always): restore INFO so the suites that follow keep their
    // Level=INFO assumptions. The row stays (never deleted).
    dbUpdate("UPDATE app_logger_config SET level = 'INFO', updated_at = '" + nowTs() + "' "
        + "WHERE hostname = '" + cfgHost + "' AND deleted = FALSE");
    var cfgRestore = waitFor(function () { return log.isInfoEnabled() && !log.isWarnEnabled() && !log.isDebugEnabled(); }, 70000);
    dbVerify(function () { return cfgSqlRow; });
    check({
        name: "app_logger_config level restored to INFO",
        ok: cfgRestore,
        detail: "isInfoEnabled=" + cfgRestore
            + (cfgRestore ? "" : " (restore refresh not observed within 70s - later suites may be affected)"),
        objective: "Verify the config row is restored to INFO (row kept, later suites unaffected)",
        objectiveZh: "驗證 level 已還原為 INFO（資料行保留，不影響後續套件）",
        command: "UPDATE app_logger_config SET level='INFO' -> isInfoEnabled() restored"
    });
} else {
    print("[SKIP] C2-C4 skipped: no addressable app_logger_config row (see C1).");
}

writeReport();
writeMd();
print("TestAppLoggerConfig result: " + passCount + " passed, " + failCount + " failed");

// No System.exit here: the facade-style suites own that switch
// (JSPLUS_FACADE_EXIT=1). This suite always returns normally and lets
// _RunOne.js derive the process exit code from passCount/failCount.
