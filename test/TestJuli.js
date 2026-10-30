// ============================================================================
// JsPlus-LogReplaced TestJuli - org.apache.juli.logging facade suite
// ----------------------------------------------------------------------------
// Run      : test/TestFacades.bat (runs all five facade suites), or
//            Console.eval(<PROJECT_ROOT>/test/TestJuli.js)
// Harness  : TestBase.js (shared counters / check() / HTML+MD reports)
// Output   : <PROJECT_ROOT>/docs/latest/report/TestJuli-Report.html / .md
// Coverage : the Tomcat JULI surface (18 methods) - the one facade that was
//            already complete before the interface work, kept as a regression
//            guard (it is the only family with a dedicated adapter, LogCustom):
//              J1 getLog(String) returns a Log
//              J2 all 18 methods are accepted (with/without Throwable)
//              J3 info reaches the sink
//              J4 fatal lands on engine ERROR
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

var REPORT_NAME = "TestJuli-Report.html";
var REPORT_TITLE = "JsPlus-LogReplaced JULI Facade Test Report";

var JuliLogFactory = Java.type("org.apache.juli.logging.LogFactory");

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

print("=== JsPlus-LogReplaced JULI Facade Test ===");
print("JS_PLUS_PATH: " + System.getProperty("JS_PLUS_PATH"));

var tag = "REPL-JULI-" + System.nanoTime();
var loggerName = "test.replacement.juli";
var log = JuliLogFactory.getLog(loggerName);

// Case J1
check({
    name: "juli getLog(String) returns a Log",
    ok: log !== null && log !== undefined,
    detail: "logger=" + log,
    objective: "Verify org.apache.juli.logging.LogFactory.getLog(String) returns a usable Log",
    objectiveZh: "驗證 org.apache.juli.logging.LogFactory.getLog(String) 回傳可用的 Log",
    command: "LogFactory.getLog(\"" + loggerName + "\")"
});

// Case J2
var j2ok = true;
var j2err = "";
try {
    log.trace("J2 " + tag + " trace");
    log.trace("J2 " + tag + " trace", new RuntimeException(tag));
    log.debug("J2 " + tag + " debug");
    log.debug("J2 " + tag + " debug", new RuntimeException(tag));
    log.info("J2 " + tag + " info");
    log.info("J2 " + tag + " info", new RuntimeException(tag));
    log.warn("J2 " + tag + " warn");
    log.warn("J2 " + tag + " warn", new RuntimeException(tag));
    log.error("J2 " + tag + " error");
    log.error("J2 " + tag + " error", new RuntimeException(tag));
    log.fatal("J2 " + tag + " fatal");
    log.fatal("J2 " + tag + " fatal", new RuntimeException(tag));
    log.isTraceEnabled();
    log.isDebugEnabled();
    log.isInfoEnabled();
    log.isWarnEnabled();
    log.isErrorEnabled();
    log.isFatalEnabled();
} catch (e) {
    j2ok = false;
    j2err = "" + e;
}
check({
    name: "juli all 18 official methods are accepted",
    ok: j2ok,
    detail: j2ok ? "6 levels x (message, message+Throwable) + 6 probes accepted" : j2err,
    objective: "Verify the complete official Log surface (18 methods) can be called without throwing",
    objectiveZh: "驗證完整官方 Log 介面（18 方法）可呼叫且不拋例外",
    command: "log.trace/debug/info/warn/error/fatal × (Object, Object+Throwable); log.is<Level>Enabled()"
});

// Case J3
log.info("J3 " + tag + " info reaches the sink");
var line3 = appLogLine("J3 " + tag, "INFO ");
check({
    name: "juli info() reaches the real sink",
    ok: line3 !== null,
    detail: line3 === null ? "no INFO line for J3 after 6s" : ("line=" + line3),
    objective: "Verify the JULI adapter (LogCustom) writes through to logs/app/<yyyyMMdd>.log",
    objectiveZh: "驗證 JULI 適配器（LogCustom）寫入 logs/app/<yyyyMMdd>.log",
    command: "log.info(message) -> app log line starting with \"INFO \""
});

// Case J4
log.fatal("J4 " + tag + " fatal");
var line4 = appLogLine("J4 " + tag, "ERROR ");
check({
    name: "juli fatal() lands on the engine ERROR level",
    ok: line4 !== null,
    detail: line4 === null ? "no ERROR line for J4 after 6s" : ("line=" + line4),
    objective: "Verify the documented mapping: juli fatal -> JsPlus error",
    objectiveZh: "驗證已記錄的映射：juli fatal → JsPlus error",
    command: "log.fatal(message) -> app log line starting with \"ERROR \""
});

writeReport();
writeMd();
print("TestJuli result: " + passCount + " passed, " + failCount + " failed");

// Exit only when this suite is driven directly by TestFacades.bat / .sh
// (JSPLUS_FACADE_EXIT=1). A run loaded by TestAll.js must return normally so
// the aggregate run can continue and include this suite's counters.
var facadeExit = System.getenv("JSPLUS_FACADE_EXIT");
if (facadeExit !== null && facadeExit !== undefined && ("" + facadeExit) === "1") {
    System.exit(failCount > 0 ? 1 : 0);
}
