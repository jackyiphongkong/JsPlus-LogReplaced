// ============================================================================
// JsPlus-LogReplaced start task - JsPlus/script/main.js
// ----------------------------------------------------------------------------
// Run by JsPlus/bin/StartLogRp.bat / StartLogRp.sh:
//   com.killcoding.jsplus.Main -Env script -ScriptEngine graalvm.js
//     -Task script/main.js
// The classpath is every JAR under LibHome, so this console sees both the
// JsPlus-Core engine (com.killcoding.*) and this replacement layer (org.*).
// NOTE: keep this file dependency-free - it is the minimal "console is up"
// task; the test suites live in <PROJECT_ROOT>/test/.
// ============================================================================

var System = Java.type("java.lang.System");
var Logger = Java.type("com.killcoding.log.Logger");
var LoggerFactory = Java.type("com.killcoding.log.LoggerFactory");

var log = LoggerFactory.getLogger("main.js");
Logger.systemInfo("main.js", "Starting...");

var jsPlusPath = System.getProperty("JS_PLUS_PATH");
print("Hello World!");
print("JS_PLUS_PATH: " + jsPlusPath);
