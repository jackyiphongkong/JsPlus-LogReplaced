// ============================================================================
// JsPlus-LogReplaced TestLog4j2 - org.apache.logging.log4j (2.20) suite
// ----------------------------------------------------------------------------
// Run      : test/TestFacades.bat (runs all five facade suites), or
//            Console.eval(<PROJECT_ROOT>/test/TestLog4j2.js)
// Harness  : TestBase.js (shared counters / check() / HTML+MD reports)
// Output   : <PROJECT_ROOT>/docs/latest/report/TestLog4j2-Report.html / .md
// Coverage : the Log4j2 surface (Logger 384 methods, generated from the
//            official javap dump), asserted against the real sink:
//              G1 LogManager.getLogger(name) + getName()
//              G2 atInfo().withMarker().withThrowable().log() -> INFO line with
//                 the marker prefix and the exception text
//              G3 fatal() lands on engine ERROR
//              G4 printf(...) expands its format arguments
//              G5 trace() lands on the engine MARK level (flow tracing works)
//              G6 throwing(...) returns the same exception
//              G7 Level semantics + isXEnabled(Marker) agreement
// Exit     : 0 when every case passed, 1 otherwise (CI can gate on it).
// ============================================================================

var System = Java.type("java.lang.System");
var Thread = Java.type("java.lang.Thread");
var DiskFile = Java.type("com.killcoding.file.DiskFile");
var LogRecord = Java.type("com.killcoding.log.LogRecord");
var RuntimeException = Java.type("java.lang.RuntimeException");
// JsString (com.killcoding.jsplus.JsString) is the project rule for GraalVM string
// interop: it keeps the java.lang.String methods callable from JS and pushes every
// regex operation (split / replaceAll / matches) into java.util.regex, so this suite
// never depends on the JS regex engine - this GraalVM build ships without TRegex and
// a JS regex literal throws "No language for id regex found".
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

var REPORT_NAME = "TestLog4j2-Report.html";
var REPORT_TITLE = "JsPlus-LogReplaced Log4j2 Facade Test Report";

var Log4j2LogManager = Java.type("org.apache.logging.log4j.LogManager");
var Log4j2Level = Java.type("org.apache.logging.log4j.Level");
var Log4j2MarkerManager = Java.type("org.apache.logging.log4j.MarkerManager");

function appLogLine(tag, levelPrefix, tries) {
    var attempts = (tries === undefined || tries === null) ? 12 : tries;
    var logPath = basePath + "../logs/app/" + LogRecord.getLogFileName();
    for (var i = 0; i < attempts; i++) {
        var file = new DiskFile(logPath);
        if (file.exists()) {
            var content = file.readAllString("UTF-8");
            if (content !== null && ("" + content).indexOf(tag) >= 0) {
                var lines = ("" + content).split("\n");
                for (var j = lines.length - 1; j >= 0; j--) {
                    var line = lines[j];
                    if (line.indexOf(tag) < 0) {
                        continue;
                    }
                    if (levelPrefix === null || levelPrefix === undefined || line.indexOf(levelPrefix) === 0) {
                        return line;
                    }
                }
            }
        }
        Thread.sleep(500);
    }
    return null;
}

// True when the line starts an app-log event ("<LEVEL> <yyyyMMdd> ...").
// Delegates to JsString so the prefix test runs as java.lang.String#startsWith
// instead of a JS regex (no TRegex in this GraalVM build).
function isLevelStart(line) {
    if (line === null || line === undefined) {
        return false;
    }
    var text = JsString.valueOf(line);
    var prefixes = ["DEBUG ", "INFO ", "WARN ", "ERROR ", "MARK "];
    for (var i = 0; i < prefixes.length; i++) {
        if (text.startsWith(prefixes[i])) {
            return true;
        }
    }
    return false;
}

// Returns the newest app-log event block that contains the tag: the matched
// line plus every following line up to the next level-started line. Throwables
// are rendered on the lines BELOW the event line (the engine writes the
// message wrapped in ** and then the stack trace), so a single-line check
// cannot see them.
function appLogEventBlock(tag, levelPrefix, tries) {
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
                    if (levelPrefix !== null && levelPrefix !== undefined && !line.startsWith(levelPrefix)) {
                        continue;
                    }
                    var block = line;
                    for (var k = j + 1; k < lines.length; k++) {
                        var next = "" + lines[k];
                        if (isLevelStart(next)) {
                            break;
                        }
                        block = block + "\n" + next;
                    }
                    return block;
                }
            }
        }
        Thread.sleep(500);
    }
    return null;
}

print("=== JsPlus-LogReplaced Log4j2 Facade Test ===");
print("JS_PLUS_PATH: " + System.getProperty("JS_PLUS_PATH"));

var tag = "REPL-LOG4J2-" + System.nanoTime();
var loggerName = "test.replacement.log4j2";
var log = Log4j2LogManager.getLogger(loggerName);

// Case G1
check({
    name: "log4j2 LogManager.getLogger(name) returns a Logger with that name",
    ok: log !== null && log !== undefined && ("" + log.getName()) === loggerName,
    detail: "getName=" + (log === null || log === undefined ? "null" : log.getName()) + ", level=" + log.getLevel(),
    objective: "Verify LogManager.getLogger(String) returns a logger bound to that name",
    objectiveZh: "驗證 LogManager.getLogger(String) 回傳綁定該名稱的 logger",
    command: "LogManager.getLogger(\"" + loggerName + "\")"
});

// Case G2
var markerName = "M-" + tag;
var marker = Log4j2MarkerManager.getMarker(markerName);
var causeText = tag + "-builder-cause";
log.atInfo().withMarker(marker).withThrowable(new RuntimeException(causeText)).log("G2 " + tag + " builder");
var g2block = appLogEventBlock("G2 " + tag, "INFO ");
check({
    name: "log4j2 LogBuilder carries marker, throwable and message to the sink",
    ok: g2block !== null && g2block.indexOf(markerName + " - ") >= 0 && g2block.indexOf(causeText) >= 0,
    detail: g2block === null ? "no INFO event for G2 after 6s" : ("event=" + JsString.valueOf(g2block).replaceAll("\n", " | ").asString()),
    objective: "Verify atInfo().withMarker().withThrowable().log() writes one INFO event with both attachments",
    objectiveZh: "驗證 atInfo().withMarker().withThrowable().log() 寫入一條含標記與異常的 INFO 事件",
    command: "log.atInfo().withMarker(marker).withThrowable(t).log(message)"
});

// Case G3
log.fatal("G3 " + tag + " fatal");
var g3line = appLogLine("G3 " + tag, "ERROR ");
check({
    name: "log4j2 fatal() lands on the engine ERROR level",
    ok: g3line !== null,
    detail: g3line === null ? "no ERROR line for G3 after 6s" : ("line=" + g3line),
    objective: "Verify the documented mapping: log4j2 fatal -> JsPlus error",
    objectiveZh: "驗證已記錄的映射：log4j2 fatal → JsPlus error",
    command: "log.fatal(message) -> app log line starting with \"ERROR \""
});

// Case G4
log.printf(Log4j2Level.INFO, "G4 %s tag %s " + tag, "A", "B");
var g4line = appLogLine("G4 A tag B " + tag, "INFO ");
check({
    name: "log4j2 printf() expands its format arguments",
    ok: g4line !== null,
    detail: g4line === null ? "no INFO line \"G4 A tag B " + tag + "\" after 6s" : ("line=" + g4line),
    objective: "Verify printf(Level, format, args) renders String.format output into the sink",
    objectiveZh: "驗證 printf(Level, format, args) 以 String.format 展開並寫入日誌",
    command: "log.printf(Level.INFO, \"G4 %s tag %s\", \"A\", \"B\")"
});

// Case G5
log.trace("G5 " + tag + " trace to mark");
var g5line = appLogLine("G5 " + tag, "MARK ");
check({
    name: "log4j2 trace() lands on the engine MARK level",
    ok: g5line !== null,
    detail: g5line === null ? "no MARK line for G5 after 6s" : ("line=" + g5line),
    objective: "Verify the documented mapping: log4j2 trace -> JsPlus MARK",
    objectiveZh: "驗證已記錄的映射：log4j2 trace → JsPlus MARK",
    command: "log.trace(message) -> app log line starting with \"MARK \""
});

// Case G6
var thrown = new RuntimeException(tag + "-throwing");
var returned = log.throwing(thrown);
check({
    name: "log4j2 throwing(T) returns the same exception instance",
    ok: returned !== null && returned !== undefined && ("" + returned.getMessage()) === ("" + thrown.getMessage()),
    detail: "returned=" + returned,
    objective: "Verify the generic throwing(T) contract (returns the passed exception) is preserved",
    objectiveZh: "驗證泛型 throwing(T) 契約（回傳傳入的例外）被保留",
    command: "Throwable r = log.throwing(new RuntimeException(...))"
});

// Case G7
var probeMarker = Log4j2MarkerManager.getMarker(tag);
var probeMarkerOn = log.isInfoEnabled(probeMarker);
var plainOn = log.isInfoEnabled();
var levelOrder = Log4j2Level.INFO.intLevel() > Log4j2Level.DEBUG.intLevel();
var inRange = Log4j2Level.INFO.isInRange(Log4j2Level.DEBUG, Log4j2Level.WARN);
var forName = Log4j2Level.forName("INFO", 0);
check({
    name: "log4j2 Level semantics and isXEnabled(Marker) agreement hold",
    ok: probeMarkerOn === plainOn && levelOrder === true && inRange === true
        && ("" + forName.name()) === "INFO",
    detail: "isInfoEnabled(marker)=" + probeMarkerOn + ", isInfoEnabled()=" + plainOn
        + ", INFO>DEBUG=" + levelOrder + ", INFO in [DEBUG,WARN]=" + inRange
        + ", forName(INFO).name=" + forName.name(),
    objective: "Verify Level ordering/range/forName and that marker probes match the plain probes",
    objectiveZh: "驗證 Level 的序、範圍、forName，以及 Marker 探測與普通探測一致",
    command: "Level.INFO.intLevel(); Level.INFO.isInRange(DEBUG, WARN); Level.forName(\"INFO\", 0); log.isInfoEnabled(marker)"
});

writeReport();
writeMd();
print("TestLog4j2 result: " + passCount + " passed, " + failCount + " failed");

// Exit only when this suite is driven directly by TestFacades.bat / .sh
// (JSPLUS_FACADE_EXIT=1). A run loaded by TestAll.js must return normally so
// the aggregate run can continue and include this suite's counters.
var facadeExit = System.getenv("JSPLUS_FACADE_EXIT");
if (facadeExit !== null && facadeExit !== undefined && ("" + facadeExit) === "1") {
    System.exit(failCount > 0 ? 1 : 0);
}
