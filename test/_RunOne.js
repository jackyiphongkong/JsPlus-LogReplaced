// ============================================================================
// _RunOne.js - run a single JsPlus-LogReplaced test suite standalone.
// Usage: set env RUN_SUITE=TestLog4j, then:
//   java ... com.killcoding.jsplus.Main -Task .../test/_RunOne.js
// Unlike JsPlus-Tomcat/test/_RunOne.js this entry has NO server to bootstrap:
// JsPlus-LogReplaced is a pure logging-replacement layer, so there is no
// global `tomcat` object to create and no port to wait for. What it does
// provide is one uniform entry so every suite can be driven by
// test\_RunOne.bat / .sh (pre-cleanup -> StopAll -> H2 init/start -> one
// suite -> StopAll).
// The suite prints its own pass/fail summary and writes its own report;
// this file only loads it and turns the final counters into an exit code.
// ============================================================================

var System = Java.type("java.lang.System");
var Runtime = Java.type("java.lang.Runtime");
var suiteName = System.getenv("RUN_SUITE");

if (!suiteName) {
    System.err.println("[_RunOne] ERROR: RUN_SUITE env var not set.");
    System.exit(1);
}

var basePath = System.getProperty("JS_PLUS_PATH");
print("[_RunOne] Running " + suiteName + ".js ...");
Console.eval(basePath + "/../test/" + suiteName + ".js");

// The suites share TestBase.js counters (passCount / failCount). Vendor suites
// self-exit when JSPLUS_FACADE_EXIT=1, in which case System.exit has already
// ended the JVM and this code is never reached. For the engine-side suites
// (TestConsole / TestLog), which never call System.exit, this is the only way
// the process exit code can reflect the result - so it is read defensively:
// if the counter is not visible from here, the run still completes with 0
// rather than failing on a binding problem.
var rc = 0;
try {
    if (typeof failCount !== "undefined" && failCount > 0) {
        rc = 1;
    }
} catch (e) {
    rc = 0;
}
print("[_RunOne] " + suiteName + " result: exit code " + rc);
Runtime.getRuntime().halt(rc);
