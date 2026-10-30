// ============================================================================
// JsPlus-LogReplaced TestSlf4j - org.slf4j facade suite (console mode)
// ----------------------------------------------------------------------------
// Run      : test/TestFacades.bat (runs all five facade suites), or
//            Console.eval(<PROJECT_ROOT>/test/TestSlf4j.js)
// Harness  : TestBase.js (shared counters / check() / HTML+MD reports)
// Output   : <PROJECT_ROOT>/docs/latest/report/TestSlf4j-Report.html / .md
// Coverage : the SLF4J 1.7 surface of the replacement layer, asserted against
//            the real sink (<PROJECT_ROOT>/logs/app/<yyyyMMdd>.log):
//              S1 getLogger(name) + getName()
//              S2 isXEnabled() follows the runtime config (Level=INFO)
//              S3 all Marker overloads + isXEnabled(Marker) are accepted
//              S4 Marker becomes the message prefix "<marker> - "
//              S5 "{}" placeholders are expanded with the arguments
//              S6 Throwable is rendered next to the message
//              S7 trace() lands on the engine MARK level
// Exit     : 0 when every case passed, 1 otherwise (CI can gate on it).
// ============================================================================

var System = Java.type("java.lang.System");
var Thread = Java.type("java.lang.Thread");
var DiskFile = Java.type("com.killcoding.file.DiskFile");
var LogRecord = Java.type("com.killcoding.log.LogRecord");
var RuntimeException = Java.type("java.lang.RuntimeException");
// JsString (com.killcoding.jsplus.JsString) is the project rule for GraalVM string
// interop: java.lang.String crossing the language boundary becomes a JS primitive,
// so the sink text is always handled through JsString (Java indexOf/split), which
// also keeps every regex operation inside java.util.regex.
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

var REPORT_NAME = "TestSlf4j-Report.html";
var REPORT_TITLE = "JsPlus-LogReplaced SLF4J Facade Test Report";

var Slf4jLoggerFactory = Java.type("org.slf4j.LoggerFactory");
var Slf4jMarkerFactory = Java.type("org.slf4j.MarkerFactory");

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

print("=== JsPlus-LogReplaced SLF4J Facade Test ===");
print("JS_PLUS_PATH: " + System.getProperty("JS_PLUS_PATH"));

var tag = "REPL-SLF4J-" + System.nanoTime();
var loggerName = "test.replacement.slf4j";
var log = Slf4jLoggerFactory.getLogger(loggerName);

// Case S1
check({
    name: "slf4j getLogger(name) returns a logger that reports that name",
    ok: log !== null && log !== undefined && ("" + log.getName()) === loggerName,
    detail: "getName=" + (log === null || log === undefined ? "null" : log.getName()),
    objective: "Verify LoggerFactory.getLogger(String) returns a logger bound to that name",
    objectiveZh: "驗證 LoggerFactory.getLogger(String) 回傳綁定該名稱的 logger",
    command: "LoggerFactory.getLogger(\"" + loggerName + "\").getName()"
});

// Case S2
var infoOn = log.isInfoEnabled();
var debugOn = log.isDebugEnabled();
check({
    name: "slf4j isXEnabled() follows the runtime config (Level=INFO)",
    ok: infoOn === true && debugOn === false,
    detail: "isInfoEnabled=" + infoOn + ", isDebugEnabled=" + debugOn + ", isTraceEnabled=" + log.isTraceEnabled(),
    objective: "Verify the probes reflect JsPlus/config/Logger.properties (Level=INFO)",
    objectiveZh: "驗證探測方法反映 JsPlus/config/Logger.properties（Level=INFO）",
    command: "log.isInfoEnabled(); log.isDebugEnabled(); log.isTraceEnabled()"
});

// Case S3
var marker = Slf4jMarkerFactory.getMarker(tag);
var s3ok = true;
var s3err = "";
try {
    marker.add(Slf4jMarkerFactory.getMarker("SEC"));
    marker.hasChildren();
    marker.hasReferences();
    marker.iterator();
    log.trace(marker, "S3 " + tag + " trace");
    log.trace(marker, "S3 {} {}", "a", "b");
    log.debug(marker, "S3 " + tag + " debug");
    log.debug(marker, "S3 {} {}", "a", "b");
    log.info(marker, "S3 " + tag + " info");
    log.info(marker, "S3 {} {}", "a", "b");
    log.warn(marker, "S3 " + tag + " warn");
    log.warn(marker, "S3 {} {}", "a", "b");
    log.error(marker, "S3 " + tag + " error");
    log.error(marker, "S3 {} {}", "a", "b");
    log.error(marker, "S3 " + tag + " error", new RuntimeException(tag));
    log.isTraceEnabled(marker);
    log.isDebugEnabled(marker);
    log.isInfoEnabled(marker);
    log.isWarnEnabled(marker);
    log.isErrorEnabled(marker);
} catch (e) {
    s3ok = false;
    s3err = "" + e;
}
check({
    name: "slf4j Marker overloads (25) and isXEnabled(Marker) (5) are accepted",
    ok: s3ok,
    detail: s3ok ? "all Marker overloads accepted" : s3err,
    objective: "Verify every official Marker-based overload can be called without throwing",
    objectiveZh: "驗證所有官方 Marker 重載均可呼叫且不拋例外",
    command: "log.<level>(marker, message[, args|throwable]); log.is<Level>Enabled(marker)"
});

// Case S4
var markerName = "M-" + tag;
var m4 = Slf4jMarkerFactory.getMarker(markerName);
log.info(m4, "S4 " + tag + " marker prefix");
var line4 = appLogLine("S4 " + tag, null);
check({
    name: "slf4j Marker is rendered as the message prefix",
    ok: line4 !== null && line4.indexOf(markerName + " - ") >= 0,
    detail: line4 === null ? "no log line for S4 after 6s" : ("line=" + line4),
    objective: "Verify the documented mapping: Marker -> \"<markerName> - <message>\"",
    objectiveZh: "驗證已記錄的映射：Marker →「<markerName> - <message>」",
    command: "log.info(MarkerFactory.getMarker(\"" + markerName + "\"), message)"
});

// Case S5
log.info("S5 " + tag + " a={} b={}", "ALPHA", "BETA");
var line5 = appLogLine("S5 " + tag, null);
check({
    name: "slf4j {} placeholders are expanded in the sink",
    ok: line5 !== null && line5.indexOf("a=ALPHA") >= 0 && line5.indexOf("b=BETA") >= 0,
    detail: line5 === null ? "no log line for S5 after 6s" : ("line=" + line5),
    objective: "Verify {} formatting reaches the log file with the actual arguments",
    objectiveZh: "驗證 {} 格式化以實際參數寫入日誌檔",
    command: "log.info(\"a={} b={}\", \"ALPHA\", \"BETA\")"
});

// Case S6
var causeText = tag + "-cause";
log.error("S6 " + tag + " boom", new RuntimeException(causeText));
var s6found = appLogContains(causeText);
check({
    name: "slf4j Throwable is rendered next to the message",
    ok: s6found,
    detail: s6found ? causeText + " found in app log" : causeText + " not found after 6s",
    objective: "Verify error(message, Throwable) persists the exception text",
    objectiveZh: "驗證 error(message, Throwable) 將例外文字寫入日誌",
    command: "log.error(message, new RuntimeException(causeText))"
});

// Case S7
log.trace("S7 " + tag + " trace to mark");
var line7 = appLogLine("S7 " + tag, "MARK ");
check({
    name: "slf4j trace() lands on the engine MARK level",
    ok: line7 !== null,
    detail: line7 === null ? "no MARK line for S7 after 6s" : ("line=" + line7),
    objective: "Verify the documented mapping: slf4j trace -> JsPlus MARK (the engine has no TRACE level)",
    objectiveZh: "驗證已記錄的映射：slf4j trace → JsPlus MARK（引擎無 TRACE 級別）",
    command: "log.trace(message) -> app log line starting with \"MARK \""
});

writeReport();
writeMd();
print("TestSlf4j result: " + passCount + " passed, " + failCount + " failed");

// Exit only when this suite is driven directly by TestFacades.bat / .sh
// (JSPLUS_FACADE_EXIT=1). A run loaded by TestAll.js must return normally so
// the aggregate run can continue and include this suite's counters.
var facadeExit = System.getenv("JSPLUS_FACADE_EXIT");
if (facadeExit !== null && facadeExit !== undefined && ("" + facadeExit) === "1") {
    System.exit(failCount > 0 ? 1 : 0);
}
