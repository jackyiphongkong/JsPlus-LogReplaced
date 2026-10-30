// ============================================================================
// JsPlus-LogReplaced TestCommonsLogging - org.apache.commons.logging suite
// ----------------------------------------------------------------------------
// Run      : test/TestFacades.bat (runs all five facade suites), or
//            Console.eval(<PROJECT_ROOT>/test/TestCommonsLogging.js)
// Harness  : TestBase.js (shared counters / check() / HTML+MD reports)
// Output   : <PROJECT_ROOT>/docs/latest/report/TestCommonsLogging-Report.html / .md
// Coverage : the Commons Logging 1.2 surface (18 methods), asserted against the
//            real sink (<PROJECT_ROOT>/logs/app/<yyyyMMdd>.log):
//              C1 getLog(String) returns a logger
//              C2 fatal(Object) is the only level the official interface adds
//                 on top of the shared six -> lands on engine ERROR
//              C3 fatal(Object, Throwable) persists the exception text
//              C4 isFatalEnabled() agrees with isErrorEnabled()
//              C5 the inherited five levels are accepted, info reaches the sink
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

var REPORT_NAME = "TestCommonsLogging-Report.html";
var REPORT_TITLE = "JsPlus-LogReplaced Commons Logging Facade Test Report";

var CommonsLogFactory = Java.type("org.apache.commons.logging.LogFactory");

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

print("=== JsPlus-LogReplaced Commons Logging Facade Test ===");
print("JS_PLUS_PATH: " + System.getProperty("JS_PLUS_PATH"));

var tag = "REPL-COMMONS-" + System.nanoTime();
var loggerName = "test.replacement.commons";
var log = CommonsLogFactory.getLog(loggerName);

// Case C1
check({
    name: "commons getLog(String) returns a logger",
    ok: log !== null && log !== undefined,
    detail: "logger=" + log,
    objective: "Verify LogFactory.getLog(String) returns a usable Log",
    objectiveZh: "驗證 LogFactory.getLog(String) 回傳可用的 Log",
    command: "LogFactory.getLog(\"" + loggerName + "\")"
});

// Case C2
log.fatal("C2 " + tag + " fatal");
var line2 = appLogLine("C2 " + tag, "ERROR ");
check({
    name: "commons fatal() lands on the engine ERROR level",
    ok: line2 !== null,
    detail: line2 === null ? "no ERROR line for C2 after 6s" : ("line=" + line2),
    objective: "Verify the documented mapping: commons fatal -> JsPlus error (no FATAL level exists)",
    objectiveZh: "驗證已記錄的映射：commons fatal → JsPlus error（引擎無 FATAL 級別）",
    command: "log.fatal(message) -> app log line starting with \"ERROR \""
});

// Case C3
var causeText = tag + "-cause";
log.fatal("C3 " + tag + " fatal with cause", new RuntimeException(causeText));
var c3found = appLogContains(causeText);
check({
    name: "commons fatal(Object, Throwable) persists the exception text",
    ok: c3found,
    detail: c3found ? causeText + " found in app log" : causeText + " not found after 6s",
    objective: "Verify fatal(Object, Throwable) keeps both message and cause",
    objectiveZh: "驗證 fatal(Object, Throwable) 同時保留訊息與原因",
    command: "log.fatal(message, new RuntimeException(causeText))"
});

// Case C4
var fatalOn = log.isFatalEnabled();
var errorOn = log.isErrorEnabled();
check({
    name: "commons isFatalEnabled() agrees with isErrorEnabled()",
    ok: fatalOn === errorOn && (fatalOn === true || fatalOn === false),
    detail: "isFatalEnabled=" + fatalOn + ", isErrorEnabled=" + errorOn,
    objective: "Verify fatal has no separate level, so its probe mirrors error",
    objectiveZh: "驗證 fatal 沒有獨立級別，其探測與 error 一致",
    command: "log.isFatalEnabled(); log.isErrorEnabled()"
});

// Case C5
var c5ok = true;
var c5err = "";
try {
    log.trace("C5 " + tag + " trace");
    log.debug("C5 " + tag + " debug");
    log.info("C5 " + tag + " info");
    log.warn("C5 " + tag + " warn");
    log.error("C5 " + tag + " error");
    log.warn("C5 " + tag + " warn", new RuntimeException(tag));
} catch (e) {
    c5ok = false;
    c5err = "" + e;
}
var c5line = c5ok ? appLogLine("C5 " + tag + " info", "INFO ") : null;
check({
    name: "commons inherited five levels are accepted and info reaches the sink",
    ok: c5ok && c5line !== null,
    detail: c5ok ? (c5line === null ? "no INFO line for C5 after 6s" : ("line=" + c5line)) : c5err,
    objective: "Verify the five shared levels stay usable through the commons surface",
    objectiveZh: "驗證五個共用級別透過 commons 介面仍可使用",
    command: "log.trace/debug/info/warn/error(...) -> INFO line in app log"
});

// Case C6
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
    name: "commons-logging rows persisted in app_logger_msg (dbVerify)",
    ok: dbCount > 0,
    detail: "rows matching the suite tag=" + dbCount,
    objective: "Verify the commons-logging log calls are persisted into app_logger_msg by the engine's DatabaseOut path",
    objectiveZh: "驗證 commons-logging 的日誌呼叫由引擎 DatabaseOut 路徑寫入 app_logger_msg",
    command: "dbVerify(\"" + dbSql + "\")"
});
writeReport();
writeMd();
print("TestCommonsLogging result: " + passCount + " passed, " + failCount + " failed");

// Exit only when this suite is driven directly by TestFacades.bat / .sh
// (JSPLUS_FACADE_EXIT=1). A run loaded by TestAll.js must return normally so
// the aggregate run can continue and include this suite's counters.
var facadeExit = System.getenv("JSPLUS_FACADE_EXIT");
if (facadeExit !== null && facadeExit !== undefined && ("" + facadeExit) === "1") {
    System.exit(failCount > 0 ? 1 : 0);
}
