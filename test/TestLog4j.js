// ============================================================================
// JsPlus-LogReplaced TestLog4j - org.apache.log4j (1.2) facade suite
// ----------------------------------------------------------------------------
// Run      : test/TestFacades.bat (runs all five facade suites), or
//            Console.eval(<PROJECT_ROOT>/test/TestLog4j.js)
// Harness  : TestBase.js (shared counters / check() / HTML+MD reports)
// Output   : <PROJECT_ROOT>/docs/latest/report/TestLog4j-Report.html / .md
// Coverage : the Log4j 1.2 surface (Logger 7 + Category 48 = 55 methods),
//            asserted against the real sink:
//              L1 Logger.getLogger(name) + getName()
//              L2 a filtered debug call never reaches the sink (Level=INFO)
//              L3 setLevel(DEBUG) makes debug reach the sink, then restores INFO
//              L4 fatal() lands on engine ERROR
//              L5 log(Priority, Object) maps the priority (WARN -> WARN line)
//              L6 l7dlog / appender bookkeeping are safe no-ops, level accessors work
// Exit     : 0 when every case passed, 1 otherwise (CI can gate on it).
// ============================================================================

var System = Java.type("java.lang.System");
var Thread = Java.type("java.lang.Thread");
var DiskFile = Java.type("com.killcoding.file.DiskFile");
var LogRecord = Java.type("com.killcoding.log.LogRecord");
var RuntimeException = Java.type("java.lang.RuntimeException");
// JsString (com.killcoding.jsplus.JsString) is the project rule for GraalVM string
// interop: the sink text is handled through JsString (Java indexOf/split) instead of
// relying on java.lang.String method calls across the language boundary.
var JsString = Java.type("com.killcoding.jsplus.JsString");

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

var REPORT_NAME = "TestLog4j-Report.html";
var REPORT_TITLE = "JsPlus-LogReplaced Log4j 1.2 Facade Test Report";

var Log4j1Logger = Java.type("org.apache.log4j.Logger");
var Log4j1Level = Java.type("org.apache.log4j.Level");

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

print("=== JsPlus-LogReplaced Log4j 1.2 Facade Test ===");
print("JS_PLUS_PATH: " + System.getProperty("JS_PLUS_PATH"));

var tag = "REPL-LOG4J1-" + System.nanoTime();
var loggerName = "test.replacement.log4j1";
var log = Log4j1Logger.getLogger(loggerName);

// Case L1
check({
    name: "log4j1 getLogger(name) returns a Logger with that name",
    ok: log !== null && log !== undefined && ("" + log.getName()) === loggerName,
    detail: "getName=" + (log === null || log === undefined ? "null" : log.getName()) + ", class=" + log.getClass().getName(),
    objective: "Verify Logger.getLogger(String) returns the Category-based logger (official hierarchy)",
    objectiveZh: "驗證 Logger.getLogger(String) 回傳基於 Category 的 logger（官方層級）",
    command: "Logger.getLogger(\"" + loggerName + "\").getName()"
});

// Case L2
log.info("L2 " + tag + " info proves flushing");
var flushLine = appLogLine("L2 " + tag + " info", "INFO ");
log.debug("L2 " + tag + " debug must be filtered");
var l2debugSeen = appLogContains("L2 " + tag + " debug must be filtered", 2);
check({
    name: "log4j1 debug is filtered while the level is INFO",
    ok: flushLine !== null && l2debugSeen === false,
    detail: "info line=" + (flushLine === null ? "missing" : "present")
        + ", debug line seen=" + l2debugSeen + " (Level=INFO)",
    objective: "Verify the logger honours the INFO level from Logger.properties",
    objectiveZh: "驗證 logger 遵守 Logger.properties 的 INFO 級別",
    command: "log.debug(message) while Level=INFO -> line must NOT exist"
});

// Case L3
log.setLevel(Log4j1Level.DEBUG);
log.debug("L3 " + tag + " debug enabled");
var l3line = appLogLine("L3 " + tag, "DEBUG ");
log.setLevel(Log4j1Level.INFO);
check({
    name: "log4j1 setLevel(DEBUG) makes debug reach the sink, then restores INFO",
    ok: l3line !== null,
    detail: l3line === null ? "no DEBUG line for L3 after 6s" : ("line=" + l3line),
    objective: "Verify Level.DEBUG flips the effective level and the level can be restored",
    objectiveZh: "驗證 Level.DEBUG 切換有效級別且可還原",
    command: "log.setLevel(Level.DEBUG); log.debug(message); log.setLevel(Level.INFO)"
});

// Case L4
log.fatal("L4 " + tag + " fatal");
var l4line = appLogLine("L4 " + tag, "ERROR ");
check({
    name: "log4j1 fatal() lands on the engine ERROR level",
    ok: l4line !== null,
    detail: l4line === null ? "no ERROR line for L4 after 6s" : ("line=" + l4line),
    objective: "Verify the documented mapping: log4j1 fatal -> JsPlus error",
    objectiveZh: "驗證已記錄的映射：log4j1 fatal → JsPlus error",
    command: "log.fatal(message) -> app log line starting with \"ERROR \""
});

// Case L5
log.log(Log4j1Level.WARN, "L5 " + tag + " priority warn");
var l5line = appLogLine("L5 " + tag, "WARN ");
check({
    name: "log4j1 log(Priority, Object) maps the priority to the engine level",
    ok: l5line !== null,
    detail: l5line === null ? "no WARN line for L5 after 6s" : ("line=" + l5line),
    objective: "Verify Priority.toInt() drives the engine level (WARN stays WARN)",
    objectiveZh: "驗證 Priority.toInt() 驅動引擎級別（WARN 仍為 WARN）",
    command: "log.log(Level.WARN, message) -> app log line starting with \"WARN \""
});

// Case L6
var l6ok = true;
var l6err = "";
var l6level = null;
var l6priority = null;
var l6effective = null;
var l6chained = null;
try {
    log.l7dlog(Log4j1Level.INFO, "L6 " + tag + " l7dlog", new RuntimeException(tag));
    log.l7dlog(Log4j1Level.INFO, "L6 {0}", ["param"], null);
    log.assertLog(true, "assert ok");
    log.getAllAppenders();
    log.getAppender("none");
    log.removeAllAppenders();
    log.removeAppender("none");
    log.isAttached(null);
    log.setAdditivity(true);
    log.getAdditivity();
    log.setResourceBundle(null);
    log.getResourceBundle();
    log.getLoggerRepository();
    log.getHierarchy();
    l6level = log.getLevel();
    l6priority = log.getPriority();
    l6effective = log.getEffectiveLevel();
    l6chained = log.getChainedPriority();
} catch (e) {
    l6ok = false;
    l6err = "" + e;
}
check({
    name: "log4j1 l7dlog/appender bookkeeping are safe no-ops and level accessors work",
    ok: l6ok && l6level !== null && l6priority !== null && l6effective !== null,
    detail: l6ok
        ? ("level=" + l6level + ", effective=" + l6effective + ", priority=" + l6priority)
        : l6err,
    objective: "Verify the documented degradation (appenders recorded, never dispatched) stays non-breaking",
    objectiveZh: "驗證已記錄的降級（appender 僅記錄、從不派發）不影響可用性",
    command: "log.l7dlog(...); log.getAllAppenders(); log.removeAllAppenders(); log.getLevel()"
});

// Case L7
// dbVerify on the persisted state: the engine's DatabaseOut path writes these
// rows into logdb.app_logger_msg (async flush, OutLogTimer=1s), so poll until they
// appear instead of assuming a fixed delay. Console.getDataSource() is the
// datasource the engine writes through (config/DataSource.properties -> logdb).
var dbSql = "select count(*) as TOTAL from app_logger_msg where message like '%" + tag + "%'";
var dbCount = -1;
for (var dbTry = 0; dbTry < 10; dbTry++) {
    dbVerify(function () { return dbSql; });
    dbCount = (dbvResult !== null && dbvResult.error === undefined && dbvResult.rows !== undefined
        && dbvResult.rows.length > 0) ? parseInt(dbvResult.rows[0][0], 10) : -1;
    if (dbCount > 0) {
        break;
    }
    Thread.sleep(500);
}
check({
    name: "log4j 1.2 rows persisted in app_logger_msg (dbVerify)",
    ok: dbCount > 0,
    detail: "rows matching the suite tag=" + dbCount,
    objective: "Verify the log4j 1.2 log calls are persisted into app_logger_msg by the engine's DatabaseOut path",
    objectiveZh: "驗證 log4j 1.2 的日誌呼叫由引擎 DatabaseOut 路徑寫入 app_logger_msg",
    command: "dbVerify(\"" + dbSql + "\")"
});
writeReport();
writeMd();
print("TestLog4j result: " + passCount + " passed, " + failCount + " failed");

// Exit only when this suite is driven directly by TestFacades.bat / .sh
// (JSPLUS_FACADE_EXIT=1). A run loaded by TestAll.js must return normally so
// the aggregate run can continue and include this suite's counters.
var facadeExit = System.getenv("JSPLUS_FACADE_EXIT");
if (facadeExit !== null && facadeExit !== undefined && ("" + facadeExit) === "1") {
    System.exit(failCount > 0 ? 1 : 0);
}
