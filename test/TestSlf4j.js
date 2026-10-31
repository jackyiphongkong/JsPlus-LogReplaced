// ============================================================================
// JsPlus-LogReplaced TestSlf4j - org.slf4j facade suite (console mode)
// ----------------------------------------------------------------------------
// Run      : test/TestFacades.bat (runs all five facade suites), or
//            Console.eval(<PROJECT_ROOT>/test/TestSlf4j.js)
// Harness  : TestBase.js (shared counters / check() / HTML+MD reports)
// Output   : <PROJECT_ROOT>/docs/latest/report/TestSlf4j-Report.html / .md
// Coverage : the SLF4J 2.0 surface (aligned with slf4j-api 2.0.20) of the
//            replacement layer, asserted against the real sink
//            (<PROJECT_ROOT>/logs/app/<yyyyMMdd>.log):
//              S1  getLogger(name) + getName()
//              S2  isXEnabled() follows the runtime config (Level=INFO)
//              S3  all Marker overloads + isXEnabled(Marker) are accepted
//              S4  Marker becomes the message prefix "<marker> - "
//              S5  "{}" placeholders are expanded with the arguments
//              S6  Throwable is rendered next to the message
//              S7  trace() lands on the engine MARK level
//              S8  rows persisted in app_logger_msg (dbVerify)
//              S9  fluent atInfo() builder writes one event (message + args)
//              S10 fluent marker + key-value prefixes render in order
//              S11 fluent atError() + setCause renders the exception
//              S12 atX() on a disabled level returns the inert NOOP builder
//              S13 atLevel(Level) + isEnabledForLevel(Level) work
//              S14 getILoggerFactory() + makeLoggingEventBuilder(Level)
//              (S15 - the app_logger_config remote-level cases - moved out to
//               test/TestAppLoggerConfig.js so the remote-config path has its
//               own suite; this suite no longer touches that table.)
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

// Case S9
// SLF4J 2.0 fluent API: atInfo() returns a LoggingEventBuilder whose
// setMessage(...) + addArgument(...) + log() writes exactly one event.
var fluentTag = "S9 " + tag + " fluent";
log.atInfo().addArgument("ARG9").setMessage(fluentTag + " {}").log();
var line9 = appLogLine(fluentTag, null);
check({
    name: "slf4j2 fluent atInfo() builder writes one event (message + args)",
    ok: line9 !== null && line9.indexOf(fluentTag) >= 0 && line9.indexOf("ARG9") >= 0,
    detail: line9 === null ? "no log line for S9 after 6s" : ("line=" + line9),
    objective: "Verify the SLF4J 2.0 fluent atInfo() -> setMessage/addArgument/log() routes one event to the engine",
    objectiveZh: "驗證 SLF4J 2.0 流式 atInfo() → setMessage/addArgument/log() 將一個事件寫入引擎",
    command: "log.atInfo().addArgument(\"ARG9\").setMessage(\"S9 ... {}\").log()"
});

// Case S10
// Fluent markers (rendered "<name> - ", decision D3) and key-value pairs
// (rendered "key=value ", official builder semantics) both prefix the message.
var m10 = Slf4jMarkerFactory.getMarker("MK-" + tag);
var kvTag = "S10 " + tag + " kv";
log.atInfo().addMarker(m10).addKeyValue("k1", "v1").addKeyValue("k2", 2).setMessage(kvTag).log();
var line10 = appLogLine(kvTag, null);
var kvExpect = "k1=v1 k2=2 ";
check({
    name: "slf4j2 fluent marker and key-value prefixes render in order",
    ok: line10 !== null && line10.indexOf("MK-" + tag + " - ") >= 0 && line10.indexOf(kvExpect) >= 0,
    detail: line10 === null ? "no log line for S10 after 6s" : ("line=" + line10),
    objective: "Verify addMarker/addKeyValue render as the documented prefixes before the message",
    objectiveZh: "驗證 addMarker/addKeyValue 依記錄的前綴規則呈現在訊息之前",
    command: "log.atInfo().addMarker(m).addKeyValue(\"k1\",\"v1\").addKeyValue(\"k2\",2).setMessage(msg).log()"
});

// Case S11
// Fluent error with an explicit cause: setCause(Throwable) then log().
var cause11 = tag + "-fluent-cause";
log.atError().setCause(new RuntimeException(cause11)).setMessage("S11 " + tag + " fluent cause").log();
var s11found = appLogContains(cause11);
check({
    name: "slf4j2 fluent atError() + setCause renders the exception",
    ok: s11found,
    detail: s11found ? cause11 + " found in app log" : cause11 + " not found after 6s",
    objective: "Verify the fluent builder's cause is written next to the message",
    objectiveZh: "驗證流式 builder 的 cause 與訊息一同寫入日誌",
    command: "log.atError().setCause(new RuntimeException(cause)).setMessage(msg).log()"
});

// Case S12
// Disabled-level gate: isDebugEnabled() == false (Level=INFO), so atDebug()
// and atTrace() must return the inert NOOP builder that logs nothing.
var gatedText = "S12 " + tag + " gated";
var gatedText2 = "S12b " + tag + " gated";
log.atDebug().setMessage(gatedText).log();
log.atTrace().setMessage(gatedText2).log();
Thread.sleep(2500);
var gatedPath = basePath + "../logs/app/" + LogRecord.getLogFileName();
var gatedContent = "";
var gatedFile = new DiskFile(gatedPath);
if (gatedFile.exists()) {
    gatedContent = "" + JsString.valueOf(gatedFile.readAllString("UTF-8"));
}
check({
    name: "slf4j2 atX() on a disabled level returns the inert NOOP builder",
    ok: gatedContent.indexOf(gatedText) < 0 && gatedContent.indexOf(gatedText2) < 0,
    detail: "isDebugEnabled=" + log.isDebugEnabled() + " (config Level=INFO), gated text absent from app log",
    objective: "Verify atDebug()/atTrace() build nothing when the level is disabled",
    objectiveZh: "驗證 atDebug()/atTrace() 在級別停用時不產生任何日誌",
    command: "log.atDebug().setMessage(gated).log() -> nothing written"
});

// Case S13
// atLevel(Level) and isEnabledForLevel(Level) - the new 2.0 entry points.
var Slf4jEventLevel = Java.type("org.slf4j.event.Level");
var s13warn = log.isEnabledForLevel(Slf4jEventLevel.WARN);
var s13debug = log.isEnabledForLevel(Slf4jEventLevel.DEBUG);
var s13Tag = "S13 " + tag + " atLevel";
log.atLevel(Slf4jEventLevel.WARN).setMessage(s13Tag).log();
var line13 = appLogLine(s13Tag, "WARN ");
check({
    name: "slf4j2 atLevel(Level) + isEnabledForLevel(Level) work",
    ok: s13warn === true && s13debug === false && line13 !== null,
    detail: "isEnabledForLevel(WARN)=" + s13warn + ", isEnabledForLevel(DEBUG)=" + s13debug
        + ", atLevel(WARN) line=" + (line13 === null ? "null" : line13),
    objective: "Verify the 2.0 atLevel(Level)/isEnabledForLevel(Level) entry points route like the level overloads",
    objectiveZh: "驗證 2.0 的 atLevel(Level)/isEnabledForLevel(Level) 入口與級別方法一致",
    command: "log.atLevel(Level.WARN).setMessage(msg).log(); log.isEnabledForLevel(Level.DEBUG)"
});

// Case S14
// LoggerFactory.getILoggerFactory() and makeLoggingEventBuilder(Level) -
// the 2.0 entry-class surface.
var s14Logger = Slf4jLoggerFactory.getILoggerFactory().getLogger("test.replacement.slf4j.i");
var s14Name = "" + s14Logger.getName();
var s14Tag = "S14 " + tag + " make";
s14Logger.makeLoggingEventBuilder(Slf4jEventLevel.INFO).setMessage(s14Tag).log();
var line14 = appLogLine(s14Tag, null);
check({
    name: "slf4j2 getILoggerFactory() + makeLoggingEventBuilder(Level) work",
    ok: s14Name === "test.replacement.slf4j.i" && line14 !== null && line14.indexOf(s14Tag) >= 0,
    detail: "ILoggerFactory.getLogger name=" + s14Name + ", line=" + (line14 === null ? "null" : line14),
    objective: "Verify getILoggerFactory().getLogger(String) and makeLoggingEventBuilder(Level) are usable",
    objectiveZh: "驗證 getILoggerFactory().getLogger(String) 與 makeLoggingEventBuilder(Level) 可用",
    command: "LoggerFactory.getILoggerFactory().getLogger(name).makeLoggingEventBuilder(Level.INFO).log()"
});

// Case S8
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
    name: "slf4j rows persisted in app_logger_msg (dbVerify)",
    ok: dbCount > 0,
    detail: "rows matching the suite tag=" + dbCount,
    objective: "Verify the slf4j log calls are persisted into app_logger_msg by the engine's DatabaseOut path",
    objectiveZh: "驗證 slf4j 的日誌呼叫由引擎 DatabaseOut 路徑寫入 app_logger_msg",
    command: "dbVerify(\"" + dbSql + "\")"
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
