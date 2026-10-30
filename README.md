# JsPlus-LogReplaced

JsPlus-LogReplaced is the **log replacement layer of JsPlus** — compatible replacements for five logging front-ends (`org.apache.commons.logging`, `org.apache.juli.logging`, `org.apache.log4j`, `org.apache.logging.log4j`, `org.slf4j`) that keep framework code calling the familiar APIs while routing every log call into the JsPlus logging engine (`com.killcoding.log.Logger` / `LoggerFactory`, shipped by `jsplus-core-<version>.jar`).

There is no third-party logging jar and no logging configuration file: a container or framework that expects Apache Commons Logging / SLF4J / JULI transparently writes into the JsPlus application log (`logs/app/yyyyMMdd.log`, configured by `JsPlus/config/Logger.properties`).

## Quick Start (3 steps)

The toolchain is bundled with this project (no Java / Ant installation, no `JAVA_HOME` required):

| Step | Command | Output |
| --- | --- | --- |
| 1. Build | `ant.bat` (Linux/macOS: `ant.sh`) | `LibHome\jsplus\default\jsplus-logreplaced-<version>.jar` |
| 2. Test | `test\TestAll.bat` (Linux/macOS: `test\TestAll.sh`) | `docs/latest/report\*` (HTML / MD / CI summary), archived per run to `docs/reports\<version>.jar` |
| 3. Slim runtime | `packJdk.bat` (Linux/macOS: `packJdk.sh`) | `Java\runtime` (jlink cut-down) |

Baseline: `ant build` → BUILD SUCCESSFUL; `TestAll.bat` → **50 passed / 0 failed, exit 0** (8 JS suites: 2 engine-side — TestConsole (test-environment prerequisite) and TestLog (`com.killcoding.log`) — plus the 5 replacement-layer vendor suites TestSlf4j / TestCommonsLogging / TestJuli / TestLog4j / TestLog4j2, plus LoadTest for sustained-load behaviour). `test\TestFacades.bat` runs the 5 vendor suites alone (**29/29, exit 0**) as a dedicated facade gate; `test\LoadTest.bat` runs the load suite alone (**6/6, exit 0**).

## Engineering Standard (aligned with JsPlus-Core)

| Item | JsPlus-LogReplaced status |
| --- | --- |
| `build.xml` | Same target standard as JsPlus-Core: `version-init` → `version-flags` → bump cases → `write-changelog` → `changelog-diff` → `replace-version` → `clear` → `compile` → `package` → `build` (= `package,javadoc`) |
| Versioning | `version.properties` (`jr.major` / `jr.minor` / `jr.patch`) auto-increments **patch +1** on every `ant build` (carry at 999, semver); the pre-bump values are reloaded with the `jr.old.` prefix so the jar name and `version.properties` never drift; jar = `jsplus-logreplaced-${jr.version}.jar`; the previous `jsplus-logreplaced-*.jar` is removed before packaging |
| Version stamp | `Version.VERSION` carries the placeholder `${BuildVersion.VERSION}(${BuildVersion.BUILD_DATE})`, substituted before compile and restored afterwards |
| Changelog | Each build appends one entry (UTF-8) to **two** files: `changes.log` (English) and `changes_zh-HK.log` (Traditional Chinese), headed `## <version> (<builddate> <buildtime>)`; priority per language: `-Dchange.en` / `-Dchange.zh` → `build\changes-pending-en.txt` / `build\changes-pending-zh-HK.txt` (consumed and deleted) → default `- build: automatic version bump` / `- build: 自動版本遞增` |
| Source diff | Each build diffs `src` against the sibling mirror (`jr.diffroot` = `D:\GitHubRepos\JsPlus-LogReplaced\src`, silently skipped when absent) using `build/tools/DiffScanner.java` and writes `- found diffed: <path> (A\|M\|D)` into both changelogs |
| Javadoc | Each build regenerates **`docs/latest/api/`** (versioned window title, source links, `-Xdoclint:none`) and archives it as **`docs/apis/<version>.jar`**; skip with `-Djavadoc.skip=true` |
| CI | `.github/workflows/ci.yml`: `ant build` → jar/`version.properties` consistency check → `TestAll.sh` → CI summary → artifact (test reports, built jars, javadoc site and archived javadoc jar) |

## Directory Layout

| Path | Purpose |
| --- | --- |
| `src/` | Replacement sources — 48 `.java` files (~4,665 lines): the five facade packages plus the official companion types they require (`Marker`, `Level`/`Priority`, `LogBuilder`, `Message`, `Appender`, `spi` interfaces) |
| `src/com/killcoding/logreplaced/Version.java` | Build version stamp (`Version.VERSION`) |
| `src/org/...` | The five logging front-ends and their companion types (see “What the Replacement Layer Covers”) |
| `LibHome/` | Runtime dependencies (`graalvm\default` script engine, `h2\default` JDBC driver, `nashorn\default`) and `jsplus\default` holding `jsplus-core-<version>.jar` (engine) + the built `jsplus-logreplaced-<version>.jar` |
| `Java/default` | Bundled JDK (Oracle GraalVM 25.0.3) |
| `Java/runtime` | `packJdk`-generated cut-down runtime (can be deleted and rebuilt) |
| `ant/default` | Bundled Apache Ant |
| `JsPlus/bin` | Console launchers: `StartLogRp.bat` / `.sh` (classpath auto-assembled from `LibHome`, including the **JsPlus-Core engine**), `BG_StartLogRp`, `Stop`, `StopLogRp`; H2 launchers: `StartH2`, `BG_StartH2`, `StopH2`, `StartH2Console` (shell / `init` rebuild); `StopAll` kills every JsPlus JVM of this checkout |
| `JsPlus/script/main.js` | Minimal start task run by `StartLogRp` |
| `JsPlus/script/StartH2.js` + `H2Server.js` | H2 TCP server bootstrap (three child consoles for start / stop / shutdown) — same shape as JsPlus-Core |
| `JsPlus/config/Logger.properties` | Runtime logging config anchor loaded by the console at startup |
| `JsPlus/config/Appenv.properties` | `H2_PORT=${JsPlus_H2_PORT:3308}`, `H2_BASE_DIR=${JS_PLUS_PATH}../Database/` |
| `JsPlus/config/DataSource.properties` | H2 datasource anchor: `jdbc:h2:tcp://localhost:${H2_PORT}/logdb` (`sample_user` / `sample_password`) + pool limits |
| `Database/` | H2 data directory (`H2_BASE_DIR`), created by `StartH2`, holds `logdb.mv.db`, git-ignored |
| `JsPlus/config/GhostDataSource.properties` | Ghost console datasource (`GhostTimer=500ms`, H2 connection). Kept Core-identical, but this project's schema ships no `ghost_remote_command` queue, so the ghost console retries without a queue while the H2 server keeps working |
| `dbscript/change.sql` | Idempotent DDL for `logdb`: `sample_user` + `app_logger_config` (per-host remote overrides: `level` / `class_name_enable_regex` / `class_name_disable_regex`) + `app_logger_msg` (log records) |
| `dbscript/fallback.sql` | Reset script (drops test tables only; `ghost_remote_command` is never dropped) - the first stage of `StartH2Console init` |
| `JsPlus/.tmp` | Console working directory |
| `test/` | `TestBase.js` harness, `TestAll.js` entry, `TestAll.bat` / `TestAll.sh`, the 2 engine-side suites (`TestConsole.js` / `TestLog.js`), **one suite per logging vendor** (`TestSlf4j.js` / `TestCommonsLogging.js` / `TestJuli.js` / `TestLog4j.js` / `TestLog4j2.js`, registered in `TestAll.js` and also runnable alone via `TestFacades.bat` / `TestFacades.sh`), the load suite (`LoadTest.js` + `LoadTest.bat` / `LoadTest.sh`), plus `iface/refapi/` (dual-compile sample) |
| `docs/latest/report/` | Reports of the newest run (`TestAll-Report.html` / `.md`, `TestAll-CI-Summary.md`, per-suite reports) |
| `docs/reports/<version>.jar` | Archived test-report jar per run (one jar per version, newest 10 kept) |
| `docs/latest/api/` | Javadoc site of the newest release — open `docs/latest/api/index.html` |
| `docs/apis/<version>.jar` | Archived javadoc jar per release (one jar per version) |
| `build/classes/` | Intermediate compilation output (build-time only) |
| `build/tools/DiffScanner.java` | Source-diff tool used by `changelog-diff` |
| `version.properties` | Build version state (auto-incremented) |
| `changes.log` / `changes_zh-HK.log` | Bilingual build changelog (EN / 繁中) |

## What the Replacement Layer Covers

All families resolve to the same engine — `com.killcoding.log.Logger` via `org.slf4j.LoggerClass` (or, for JULI, a dedicated `LogCustom`). **Each family's official interface is implemented in full**, and the coverage is verified method-by-method against the official jars (see “Interface completeness gate”):

| Front-end | Official API aligned | Declared methods (ours) | Entry point | Implementation |
| --- | --- | --- | --- | --- |
| SLF4J 1.7 | `org.slf4j.Logger` (61) + `Marker` / `IMarkerFactory` / `MarkerFactory` | 81 = 61 official + 20 JsPlus extensions | `org.slf4j.LoggerFactory.getLogger(Class/String)` | `org.slf4j.LoggerClass` |
| Apache Commons Logging 1.2 | `org.apache.commons.logging.Log` (18) | 84 (all 18 covered, rest inherited) | `org.apache.commons.logging.LogFactory.getLog(Class/String)` | `org.apache.commons.logging.LoggerClass` |
| Apache Log4j 1.2 | `Logger` (7) + `Category` (48) = 55, plus `Priority` / `Level` / `Appender` / `Layout` / `spi.*` | 122 | `org.apache.log4j.Logger.getLogger(...)` / `new Logger(...)` | `org.apache.log4j.Category` (base) + `Logger` |
| Apache Log4j 2.20 | `Logger` (384) + `Level` / `Marker` / `LogBuilder` / `LogManager` / `message.*` / `util.Supplier` | 424 | `org.apache.logging.log4j.LogManager.getLogger(Class/String)` | `org.apache.logging.log4j.LoggerClass` (generated) |
| Apache JULI 9.0 | `org.apache.juli.logging.Log` (18) | 18 | `org.apache.juli.logging.LogFactory.getLog(Class/String)` | `org.apache.juli.logging.LogCustom` |

`LogFactory` / `LoggerFactory` / `LogManager` are the only construction points; each returns a per-class or per-name logger. `release(...)` methods are no-ops (nothing to release).

### Behavioural mapping (deliberate, documented)

- **Marker** → message prefix `"<markerName> - " + message`, no filtering (`Marker` also keeps its `add/remove/hasChildren/iterator` semantics).
- **`fatal`** → engine `error` — the engine has no FATAL level. Same for log4j2 `FATAL`.
- **`trace`** (log4j2) and **`TRACE`** (log4j1) → engine `mark`.
- **log4j1 appenders / hierarchy** are recorded on the logger but never dispatched (`LoggingEvent#callAppenders` is a no-op); levels come from `Priority.toInt()`.
- **log4j1 `setLevel` / `setPriority`** propagate to the engine (`Category.engine().setLevel(...)`) so `setLevel(Level.DEBUG)` really makes `debug()` reach the sink; the two priorities the engine lacks are folded onto the nearest one: `FATAL` → `ERROR`, `TRACE` → `DEBUG`, `ALL` → `DEBUG`, `OFF` → `ERROR`.
- **log4j2 `Supplier` / `MessageSupplier`** are evaluated eagerly so that no call is silently dropped (no lazy filtering).
- **log4j2 `Level`** is an enum (keeps `values()`/`valueOf()/name()` semantics). `LogManager.getContext()/setFactory()/getFactory()` are intentionally absent — this layer has no pluggable LoggerContext.
- The log4j2 `Logger` and its implementation (384 methods) are **generated** from the official `javap` dump by `build/tools/genLog4j2.py`; edit the generator, never the generated files (`src/org/apache/logging/log4j/Logger.java`, `LoggerClass.java`).

### Interface completeness gate

Three checks, all runnable locally before a release:

| Check | Command | Last result |
| --- | --- | --- |
| Method-by-method diff against the five official API jars | `python build/iface-audit/compare.py` | 0 missing in every family (61/81, 18/84, 18/18, 55/122, 384/424) |
| Dual compile of one official-API sample | `test\iface\refapi\CompileApiSample.bat` | the same `ApiUsageSample.java` compiles against the official jars **and** against `jsplus-logreplaced` |
| Runtime smoke of the five facades | `test\TestFacades.bat` | 29/29 across 5 vendor suites (7 + 5 + 4 + 6 + 7), exit 0 — every facade call reaches `logs/app/<yyyyMMdd>.log` |

Reference jars sit in `build/iface-audit/ref/` (slf4j-api 1.7.36, commons-logging 1.2, tomcat-juli 9.0.85, log4j 1.2.17, log4j-api 2.20.0); the plan and decision log is `INTERFACE-PLAN.md`. The replacement layer cannot coexist with the official logging jars on the same class path (same package names).

## Test Suite

### Framework signature: JavaScript verifies Java

Every suite is a JavaScript file executed inside the bundled JsPlus console engine — the same engine production code embeds. The engine-side suites (`TestConsole` / `TestLog`) cover the console environment the harness itself stands on and the `com.killcoding.log` core; the vendor suites call the real replacement classes (`LogFactory.getLog(...)` / `LoggerFactory.getLogger(...)` / `LogManager.getLogger(...)`), assert on live results, and all of them render GitHub-style HTML/MD reports. There is no JUnit and no separate test framework: `test/*.js` + `test/TestBase.js` is the whole harness.

### How to run

| Want | Command |
| --- | --- |
| Full run: all 8 suites + aggregate report | `test\TestAll.bat` (Linux/macOS: `test\TestAll.sh`) |
| Only the 5 vendor suites (per-vendor exit code = CI gate) | `test\TestFacades.bat` (Linux/macOS: `test\TestFacades.sh`) |
| Only the load suite (6 load cases, exit code = CI gate) | `test\LoadTest.bat` (Linux/macOS: `test\LoadTest.sh`) |
| A single vendor suite | `TestFacades.bat` runs five separate consoles; to run one, use the same launch line with `-Task test/TestLog4j.js` etc. |
| Dual-compile conformance sample | `test\iface\refapi\CompileApiSample.bat` |
| Any single suite | copy the `-Task test/TestXxx.js` line from `TestAll.bat`; every suite is a plain console task and runs standalone too |

Reports land in `docs/latest/report\` (HTML + MD + CI summary) and are archived per run to `docs/reports\<version>.jar` — version taken from the project jar name, newest 10 jars kept, archive failure is WARN-only and never changes the exit code (same layout as JsPlus-Core).

Before any suite runs, `TestAll.bat` / `TestAll.sh` clean `JsPlus\.tmp` (the `evalJavaSource` / `evalJavaFile` scratch tree — same as JsPlus-Core Step 0b) and `logs\*` (the engine output tree, `logs\app\<yyyyMMdd>.log`). Every run therefore starts from an empty log, so the suites only ever see lines this run wrote — a stale line from an earlier run can no longer satisfy a check by accident.

### The suites

| Suite | Cases | Focus |
| --- | --- | --- |
| TestConsole | 9 | Console environment the other suites rely on: `JS_PLUS_PATH`, global `Console`, `evalScript` / `load` / `print`, Java interop (`Java.type` / `ArrayList` / `HashMap`), `DiskFile` roundtrip |
| TestLog | 5 | `com.killcoding.log`: `LoggerFactory.getLogger`, level state from `Logger.properties`, info/warn/error/mark/debug do not throw, `FileOut` writes `logs/app/<yyyyMMdd>.log`, `setLevel("DEBUG")` flips `isDebugEnabled` |
| LoadTest | 6 | Sustained load against `com.killcoding.log`: 10 000-line burst throughput (caller must not block), zero line loss after async flush, 8 KB payload integrity, 200 throwables, 5 loggers × 1000 interleaved lines, level filtering under load. Runs **last** so its ~16.5k lines cannot disturb the suites above |

> Removed because they exercise engine packages this replacement layer does not ship: `TestDatasource.js` (false positive — a scope note that always passed, Clock getters only), `TestCache.js` (`com.killcoding.cache`), `TestFile.js` (`com.killcoding.file`), `TestTool.js` (`com.killcoding.tool`). Their reports were deleted with them.

**Vendor suites** — one file per logging family, each independently runnable, each asserting against the real sink (`logs/app/<yyyyMMdd>.log`) rather than only "does not throw". They are registered in `TestAll.js` like the engine-side suites (so the aggregate report covers all 8), and `TestFacades.bat` / `.sh` can also drive them alone with a per-vendor exit code:

| Suite | Cases | Focus |
| --- | --- | --- |
| TestSlf4j | 7 | `org.slf4j`: `getName()`, `isXEnabled()` vs the configured level, all 25 Marker overloads + 5 `isXEnabled(Marker)`, Marker → `"<marker> - "` prefix, `{}` expansion, Throwable persistence, `trace()` → `MARK` |
| TestCommonsLogging | 5 | `org.apache.commons.logging`: `getLog()`, `fatal()` → `ERROR`, `fatal(Object, Throwable)` keeps the cause, `isFatalEnabled()` == `isErrorEnabled()`, the five shared levels |
| TestJuli | 4 | `org.apache.juli.logging`: `getLog()` → `LogCustom`, all 18 official methods accepted, `info()` reaches the sink, `fatal()` → `ERROR` |
| TestLog4j | 6 | `org.apache.log4j` (1.2): `getName()`, debug filtered at `Level=INFO`, `setLevel(DEBUG)` then debug reaches the sink (level restored), `fatal()` → `ERROR`, `log(Priority, ...)` maps the priority, `l7dlog` / appender bookkeeping are safe no-ops |
| TestLog4j2 | 7 | `org.apache.logging.log4j` (2.20): `getName()`, `atInfo().withMarker().withThrowable().log()` carries both attachments, `fatal()` → `ERROR`, `printf()` expands, `trace()` → `MARK`, `throwing(T)` returns the instance, `Level` order/range/`forName` + `isXEnabled(Marker)` agreement |

### Mandatory test conventions

- The runtime config anchor `JsPlus/config/Logger.properties` must exist; `TestAll.js` runs a config self-check first and records it in the aggregate report;
- Suites **never call `System.exit()`** — `TestAll.js` owns the JVM exit (`JSPLUS_TEST_EXIT=1`, set by `TestAll.bat` / `TestAll.sh`), so an interactive `Console.eval(...)` run finishes without killing the console;
- Each suite calls `beginTest()` and reports its own counters; `TestAll.js` captures them right after each suite and aggregates into `TestAll-Report.md` / `.html` + `TestAll-CI-Summary.md`;
- **Exception — the vendor suites** (`TestSlf4j` … `TestLog4j2`): they are ordinary suites inside `TestAll.js` (no `System.exit()` there), but are *also* runnable standalone by `TestFacades.bat`, where they own their exit: `System.exit(failCount > 0 ? 1 : 0)` fires only when `JSPLUS_FACADE_EXIT=1`. So a `TestAll` run aggregates their counters, a `TestFacades` run gets a CI-usable exit code, and an interactive `Console.eval(...)` run does neither;
- **`LoadTest` follows the same pattern** with its own switch `JSPLUS_LOADTEST_EXIT=1` (set by `test\LoadTest.bat` / `.sh`), so the load suite is CI-usable standalone without exiting inside `TestAll`;
- **The load suite is single-threaded by necessity**: this GraalVM build refuses to enter the JS context from another thread (`Multi threaded access requested ... not allowed for language(s) js`) and cannot instantiate a Java interface from JS (`new Runnable({run: fn})` → `Message not supported`). The load therefore drives the engine's real bottleneck — the async writer queue — from the JS thread: measured ~5 000 lines/s enqueue rate and 0 lost lines out of 10 000;
- The engine renders a Throwable on the lines **below** the event line (`**message** ` then the stack trace), so an assertion about a cause must look at the whole event block (see `appLogEventBlock` in `TestLog4j2.js`) — a single-line check will miss it;
- **`JsString` is mandatory for sink text**: every suite reads the log through `com.killcoding.jsplus.JsString` (`JsString.valueOf(file.readAllString("UTF-8"))` + Java `indexOf` / `split` / `startsWith`). The bundled GraalVM ships **without TRegex**, so a JS regex literal fails with `No language for id regex found`; JsString keeps regex work inside `java.util.regex` and keeps `java.lang.String` methods callable across the language boundary;
- Fixtures live under `test/.core-test-tmp/` and are removed at the end of the run;
- A throwing suite is recorded as a failed suite and does not abort the remaining suites.

### How to write a new suite

1. Copy the shape of `test/TestLog.js`.
2. Load the harness — `load(System.getProperty("JS_PLUS_PATH") + "../test/TestBase.js")` — and set `REPORT_TITLE` / `REPORT_NAME`.
3. Call `beginTest()` to reset counters, then one `check({caseId, name, ok, detail, objective, objectiveZh, command})` per assertion.
4. Finish with `writeReport()`; **never call `System.exit()`** (a vendor suite may exit only under `JSPLUS_FACADE_EXIT=1`, which `TestFacades.bat` sets).
5. Register it in `test/TestAll.js` (the `suites` array **and** the one-line suites print). A vendor suite additionally goes into the list inside `TestFacades.bat` / `.sh` for the standalone gate.

## Build & Versioning

- `version.properties` (`jr.major` / `jr.minor` / `jr.patch`) auto-increments **patch +1** on every `ant build` (carry at 999, semver).
- The version is written into `Version.VERSION` via the placeholder `${BuildVersion.VERSION}(${BuildVersion.BUILD_DATE})`, substituted before compile and restored after.
- Each build removes the old `jsplus-logreplaced-*.jar` and outputs the versioned jar into `LibHome/jsplus/default/`.
- Each build appends one history entry to `changes.log` (EN) and `changes_zh-HK.log` (繁中); see the table above for the entry priority.
- Each build regenerates the API site into `docs/latest/api/` and archives it as `docs/apis/<version>.jar`.
- On low-virtual-memory hosts the forked javac may fail to attach to JVMCI: export `ANT_OPTS=-Xms16m -Xmx128m -XX:+UseSerialGC -XX:-UseJVMCICompiler` and the same flags in `JAVA_TOOL_OPTIONS` (the fork inherits them).

## Environment Overrides (same naming convention as the server edition)

| Variable | Default | Purpose |
| --- | --- | --- |
| `JsPlus_JavaDir_JsPlus-LogReplaced` | `default` | JDK directory name under `Java\` |
| `JsPlus_JvmParams_JsPlus-LogReplaced` | `-Xms256m -Xmx1024m -Xss256k` | JVM parameters |
| `SCRIPT_ENGINE` (script arg %1) | `graalvm.js` | Script engine |

## Release Helper

`release.bat` (Linux/macOS: `release.sh`) copies the newest built `jsplus-logreplaced-*.jar` into every sibling project under the scan root (default: the parent folder of this project, override via `JS_PLUS_RELEASE_ROOT`) that contains a `LibHome\jsplus\default` folder. The previous `jsplus-logreplaced-*.jar` there is renamed to `*.jar.bak` (not deleted); other jars are left untouched; the source project itself is skipped. Exit code: `0` = success, `1` = failure.

## Running

```bat
:: Build + full test
ant.bat build
test\TestAll.bat
```

To launch the console directly: use the bundled launcher `JsPlus\bin\StartLogRp.bat` (Linux/macOS: `JsPlus/bin/StartLogRp.sh`) — it assembles the classpath from every JAR under `LibHome` (including the JsPlus-Core engine and this project's jar) and runs `com.killcoding.jsplus.Main -Env script -ScriptEngine graalvm.js -Task script/main.js`.

## H2 database (local anchor)

The replacement layer needs no database, but the runtime anchor ships the same local H2
TCP server as JsPlus-Core, so anything that expects a `jdbc:h2:tcp://localhost:3308/logdb`
endpoint finds one.

| Want | Command |
| --- | --- |
| Start the H2 server (foreground) | `JsPlus\bin\StartH2.bat` (Linux/macOS: `JsPlus/bin/StartH2.sh`) |
| Start the H2 server in the background | `JsPlus\bin\BG_StartH2.bat` (Linux/macOS: `JsPlus/bin/BG_StartH2.sh`) |
| Stop the H2 server | `JsPlus\bin\StopH2.bat` (Linux/macOS: `JsPlus/bin/StopH2.sh`) |
| Open the H2 shell / rebuild the schema | `JsPlus\bin\StartH2Console.bat` (`init` → fallback.sql → change.sql; stop `StopH2` first, the init path opens the DB file directly) |
| Stop every JsPlus JVM started from this checkout | `JsPlus\bin\StopAll.bat` (Linux/macOS: `JsPlus/bin/StopAll.sh`) |

- Port: `H2_PORT=${JsPlus_H2_PORT:3308}` — export `JsPlus_H2_PORT` to move it off 3308;
- Data files: `H2_BASE_DIR=${JS_PLUS_PATH}../Database/` (`Database/logdb.mv.db`, git-ignored,
  created by `StartH2` on first connection with `-ifNotExists`);
- Connection: `jdbc:h2:tcp://localhost:3308/logdb`, user `sample_user`, password `sample_password`
  (`JsPlus/config/DataSource.properties`), driver `org.h2.Driver` from `LibHome/h2/default/h2-2.4.240.jar`;
- `-GhostName H2Ghost` keeps the server JVM alive after the task returns — that ghost is what
  `StopH2` matches (`Stop.bat StartH2.bat` → keyword `StartH2`).

### Log tables (`app_logger_config` / `app_logger_msg`)

`dbscript/change.sql` creates two tables that make a log line queryable as data:

| Table | Grain | Columns (mapped to `Logger.properties` / `MessageFormat`) |
| --- | --- | --- |
| `app_logger_config` | one row per host — the row `DatabaseOutConfigFrom` reads | `hostname` (the `:hostname` lookup key), `level`, `class_name_enable_regex`, `class_name_disable_regex` — the only three keys `LogRecord.writeLogBatchToDatabase` accepts |
| `app_logger_msg` | one row per log event | `app_name`, `logger_name` (`:log_class_name`), `log_level` (`:level`), `log_time` (`:log_time_format`), `hostname`, `thread_name` (`:log_thread_name`), `thread_id` (`:log_thread_id`), `message` (CLOB, `:message`), `stack_trace` (CLOB, `:stack_trace`) |

Both tables use the project convention `id CHAR(16) PRIMARY KEY` + `created_at` / `updated_at` / `deleted_at` / `deleted` (soft delete). `message` and `stack_trace` are `CLOB` on purpose - the load suite writes 8000-character payloads and multi-frame stack traces.

The Core-inherited `ghost_remote_command` queue is **not** created here: this layer's job is the unified log path, not remote command dispatch. `JsPlus/config/GhostDataSource.properties` stays Core-identical for future use, and the `-GhostName H2Ghost` console then retries without a queue (H2 itself is unaffected).
