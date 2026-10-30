// ============================================================================
// JsPlus-LogReplaced LoadTest - load / throughput suite for the log layer
// ----------------------------------------------------------------------------
// Run      : test/TestAll.bat / test/TestAll.sh (registered as the last suite,
//              so the load runs AFTER every correctness suite and its lines
//              cannot disturb their assertions), or standalone:
//              test/LoadTest.bat / test/LoadTest.sh
// Output   : <PROJECT_ROOT>/docs/latest/report/LoadTest-Report.html / .md
// Coverage : com.killcoding.log under sustained load (single JVM thread):
//            - L1 burst write throughput (caller must not be blocked by I/O)
//            - L2 zero line loss: every enqueued line reaches
//              <PROJECT_ROOT>/logs/app/<yyyyMMdd>.log (async LogRecord flush,
//              OutLogTimer=1s)
//            - L3 large payload integrity (8 KB messages written in full)
//            - L4 throwable load (one event line per exception, stack below)
//            - L5 multi-logger interleave (5 loggers x 1000 lines, none lost)
//            - L6 level filtering under load (INFO suppressed at WARN, WARN kept)
// NOTE     : GraalVM polyglot forbids entering the JS context from another
//            thread ("Multi threaded access requested ... not allowed for
//            language(s) js"), and this build cannot instantiate a Java
//            interface from JS (`new Runnable({run:fn})` -> "Message not
//            supported"), so the load is driven on the single JS thread. That
//            still exercises the engine's real bottleneck: the async writer +
//            queue, which is what a log layer has to survive.
// NOTE     : every assertion filters on a per-run tag (LOAD-<id>), so lines
//            written by the suites that ran before this one never count.
// ============================================================================

var System = Java.type("java.lang.System");
var Thread = Java.type("java.lang.Thread");
// JsString is the project rule for GraalVM string interop: it keeps the
// java.lang.String methods callable from JS and pushes regex work into
// java.util.regex, so this suite never touches the JS regex engine (no TRegex
// in this GraalVM build - a JS regex literal throws "No language for id regex").
var JsString = Java.type("com.killcoding.jsplus.JsString");
var LoggerFactory = Java.type("com.killcoding.log.LoggerFactory");
var Logger = Java.type("com.killcoding.log.Logger");
var LogRecord = Java.type("com.killcoding.log.LogRecord");
var CommonTools = Java.type("com.killcoding.tool.CommonTools");
var RuntimeException = Java.type("java.lang.RuntimeException");

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

var REPORT_NAME = "LoadTest-Report.html";
var REPORT_TITLE = "JsPlus-LogReplaced Load Test Report";

print("=== JsPlus-LogReplaced Load Test ===");
print("JS_PLUS_PATH: " + System.getProperty("JS_PLUS_PATH"));

// ---- load shape ---------------------------------------------------------
var N_BURST = 10000;      // L1/L2 burst size
var N_BIG = 100;          // L3 large-payload count
var BIG_SIZE = 8000;      // L3 payload characters
var N_EX = 200;           // L4 exception count
var N_LOGGERS = 5;        // L5 logger instances
var N_PER_LOGGER = 1000;  // L5 lines per logger
var N_FILTER = 1000;      // L6 INFO lines written while level=WARN
var N_WARN = 200;         // L6 WARN lines written while level=WARN
var MIN_LINES_PER_SEC = 1000;  // L1 floor: the caller must not block on I/O
var WAIT_MS = 30000;      // per-case flush deadline (async writer, 1s timer)

var runTag = "LOAD-" + CommonTools.generateId(8);
print("Run tag: " + runTag + " (every assertion below filters on it)");

var log = LoggerFactory.getLogger("com.killcoding.test.LoadTest");

// ---- app-log helpers ----------------------------------------------------
function appLogPath() {
    return basePath + "../logs/app/" + LogRecord.getLogFileName();
}

function readAppLog() {
    var file = new DiskFile(appLogPath());
    if (!file.exists()) {
        return "";
    }
    var content = file.readAllString("UTF-8");
    if (content === null || content === undefined) {
        return "";
    }
    return "" + content;
}

// Counts every app-log line that contains `token`. Runs through JsString so the
// split is java.util.regex based, never the (missing) JS regex engine.
function countLines(token) {
    var content = readAppLog();
    if (content.length === 0) {
        return 0;
    }
    var lines = JsString.valueOf(content).split("\n");
    var count = 0;
    for (var i = 0; i < lines.length; i++) {
        if (lines[i].indexOf(token) >= 0) {
            count++;
        }
    }
    return count;
}

// Polls the app log until `token` appears on at least `expected` lines.
// Returns the observed count (the caller asserts on it).
function waitLines(token, expected, label) {
    var deadline = Date.now() + WAIT_MS;
    var count = countLines(token);
    while (count < expected && Date.now() < deadline) {
        print("  ... waiting for " + label + ": " + count + "/" + expected);
        Thread.sleep(500);
        count = countLines(token);
    }
    return count;
}

// ============================================================================
// L1 - burst write throughput: the log call must return without doing the I/O
// ============================================================================
var burstStart = System.nanoTime();
for (var b = 0; b < N_BURST; b++) {
    log.info("L1 " + runTag + "#" + b);
}
var burstMs = (System.nanoTime() - burstStart) / 1000000.0;
var burstRate = (burstMs > 0) ? (N_BURST * 1000.0 / burstMs) : 0;

check({
    name: "Burst write throughput: " + N_BURST + " INFO lines queue without blocking the caller",
    ok: burstRate >= MIN_LINES_PER_SEC,
    detail: burstMs.toFixed(1) + " ms for " + N_BURST + " lines = "
        + burstRate.toFixed(0) + " lines/s (floor " + MIN_LINES_PER_SEC + " lines/s)",
    objective: "Verify log.info() returns immediately under load - the engine writes asynchronously",
    objectiveZh: "驗證負載下 log.info() 立即返回——引擎為異步寫入",
    command: "for (i in 0.." + N_BURST + ") log.info(\"L1 TAG#i\")"
});

// ============================================================================
// L2 - zero line loss: every enqueued line must reach the app log
// ============================================================================
var l2Found = waitLines("L1 " + runTag + "#", N_BURST, "L2 burst lines");
check({
    name: "Zero line loss: all " + N_BURST + " burst lines reach logs/app/<yyyyMMdd>.log",
    ok: l2Found === N_BURST,
    detail: l2Found + "/" + N_BURST + " lines written (async flush, deadline " + WAIT_MS + " ms)",
    objective: "Verify the async LogRecord writer flushes every queued line - no loss under burst",
    objectiveZh: "驗證異步 LogRecord 寫入器把每一條排隊行刷盤——突發下不丟行",
    command: "countLines(\"L1 TAG#\") === " + N_BURST
});

// ============================================================================
// L3 - large payload integrity
// ============================================================================
var payload = "";
for (var p = 0; p < BIG_SIZE; p++) {
    payload += "x";
}
for (var g = 0; g < N_BIG; g++) {
    log.info("L3 " + runTag + "#" + g + " " + payload);
}
var l3Found = waitLines("L3 " + runTag + "#", N_BIG, "L3 large payload lines");
// One line inspected in full: the message must be intact, not truncated.
var l3LineLen = 0;
try {
    var content3 = readAppLog();
    var lines3 = JsString.valueOf(content3).split("\n");
    for (var i3 = 0; i3 < lines3.length; i3++) {
        if (lines3[i3].indexOf("L3 " + runTag + "#0 ") >= 0) {
            l3LineLen = ("" + lines3[i3]).length;
            break;
        }
    }
} catch (e3) {
    l3LineLen = -1;
}
check({
    name: "Large payload integrity: " + N_BIG + " x " + BIG_SIZE + "-char messages written in full",
    ok: l3Found === N_BIG && l3LineLen >= BIG_SIZE,
    detail: l3Found + "/" + N_BIG + " lines, first line length " + l3LineLen + " chars (payload " + BIG_SIZE + ")",
    objective: "Verify long messages are neither dropped nor truncated under load",
    objectiveZh: "驗證負載下長訊息既不丟失也不截斷",
    command: "log.info(\"L3 TAG#0 " + BIG_SIZE + " chars\"); line length >= " + BIG_SIZE
});

// ============================================================================
// L4 - throwable load: one event line per exception, stack rendered below
// ============================================================================
var exOk = true;
var exErr = "";
try {
    for (var x = 0; x < N_EX; x++) {
        log.error("L4 " + runTag + "#" + x, new RuntimeException("LOAD-EX-" + runTag + "#" + x));
    }
} catch (e) {
    exOk = false;
    exErr = "" + e;
}
var l4Found = waitLines("L4 " + runTag + "#", N_EX, "L4 exception event lines");
var l4Stack = countLines("LOAD-EX-" + runTag + "#");
check({
    name: "Throwable load: " + N_EX + " exceptions each record an event line plus a stack",
    ok: exOk && l4Found === N_EX && l4Stack >= N_EX,
    detail: exOk ? (l4Found + "/" + N_EX + " event lines, " + l4Stack + " stack references")
                 : ("threw: " + exErr),
    objective: "Verify exception logging stays complete under load (event line + stack trace)",
    objectiveZh: "驗證負載下異常日誌完整（事件行 + 堆疊）",
    command: "for (i in 0.." + N_EX + ") log.error(\"L4 TAG#i\", RuntimeException)"
});

// ============================================================================
// L5 - multi-logger interleave: 5 loggers x 1000 lines, none lost
// ============================================================================
var loggers = [];
for (var m = 0; m < N_LOGGERS; m++) {
    loggers.push(LoggerFactory.getLogger("com.killcoding.test.LoadTest.M" + m));
}
for (var r = 0; r < N_PER_LOGGER; r++) {
    for (var m2 = 0; m2 < N_LOGGERS; m2++) {
        loggers[m2].info("L5 " + runTag + "#" + m2 + "#" + r);
    }
}
var l5PerLogger = [];
var l5Ok = true;
for (var m3 = 0; m3 < N_LOGGERS; m3++) {
    var got = waitLines("L5 " + runTag + "#" + m3 + "#", N_PER_LOGGER, "L5 logger " + m3);
    l5PerLogger.push(got);
    if (got !== N_PER_LOGGER) {
        l5Ok = false;
    }
}
check({
    name: "Multi-logger interleave: " + N_LOGGERS + " loggers x " + N_PER_LOGGER + " lines, none lost",
    ok: l5Ok,
    detail: l5PerLogger.join("/") + " lines per logger (expected " + N_PER_LOGGER + " each)",
    objective: "Verify per-logger routing is exact when several loggers share the writer",
    objectiveZh: "驗證多個 logger 共用寫入器時每路 logger 的行數精確",
    command: "5 x (logger.info(\"L5 TAG#idx#i\") for i in 0.." + N_PER_LOGGER + ")"
});

// ============================================================================
// L6 - level filtering under load
// ============================================================================
var filterOk = true;
var filterErr = "";
try {
    Logger.setLevel("WARN");
    for (var f = 0; f < N_FILTER; f++) {
        log.info("L6I " + runTag + "#" + f);      // must NOT reach the file
    }
    for (var w = 0; w < N_WARN; w++) {
        log.warn("L6W " + runTag + "#" + w);      // must reach the file
    }
} catch (e) {
    filterOk = false;
    filterErr = "" + e;
} finally {
    Logger.setLevel("INFO");
}
var l6Warn = waitLines("L6W " + runTag + "#", N_WARN, "L6 WARN lines");
var l6Info = countLines("L6I " + runTag + "#");
check({
    name: "Level filtering under load: " + N_FILTER + " INFO suppressed at WARN, " + N_WARN + " WARN kept",
    ok: filterOk && l6Warn === N_WARN && l6Info === 0,
    detail: filterOk ? ("WARN " + l6Warn + "/" + N_WARN + ", INFO leaked " + l6Info)
                     : ("threw: " + filterErr),
    objective: "Verify the level gate stays exact while a burst passes through it",
    objectiveZh: "驗證突發流量經過時級別閘門仍然精確",
    command: "Logger.setLevel(\"WARN\"); " + N_FILTER + " x log.info(...); " + N_WARN + " x log.warn(...)"
});

print("");
print("Load shape: " + (N_BURST + N_BIG + N_EX + N_LOGGERS * N_PER_LOGGER + N_FILTER + N_WARN)
    + " lines enqueued (" + (N_BIG * BIG_SIZE / 1024) + " KB payload in L3)");
print("App log: " + appLogPath());

writeReport();
writeMd();

// test/LoadTest.bat / .sh set JSPLUS_LOADTEST_EXIT=1 so a standalone run is
// CI-usable (exit 1 when a case fails). Inside TestAll this stays unset, so the
// suite returns to the aggregate runner instead of killing the JVM.
var loadExit = System.getenv("JSPLUS_LOADTEST_EXIT");
if (loadExit !== null && loadExit !== undefined && ("" + loadExit) === "1") {
    System.exit(failCount > 0 ? 1 : 0);
}
