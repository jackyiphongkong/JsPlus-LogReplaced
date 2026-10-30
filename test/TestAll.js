// ============================================================================
// JsPlus-LogReplaced TestAll - unified console entry for the 8 test suites
// ----------------------------------------------------------------------------
// Run      : test/TestAll.bat / test/TestAll.sh (standalone JsPlus console):
//              <java> -cp <LibHome + build jars> com.killcoding.jsplus.Main
//                     -ScriptEngine graalvm.js -Env script -WriteHistory false
//                     -Task <PROJECT_ROOT>/test/TestAll.js < NUL
//            Interactive console eval:
//              Console.eval(<PROJECT_ROOT>/test/TestAll.js)
// Output   : Runs, in order:
//              1) TestConsole.js    -> <PROJECT_ROOT>/docs/latest/report/TestConsole-Report.html
//              2) TestLog.js        -> <PROJECT_ROOT>/docs/latest/report/TestLog-Report.html
//              3) TestSlf4j.js      -> .../TestSlf4j-Report.html
//              4) TestCommonsLogging.js -> .../TestCommonsLogging-Report.html
//              5) TestJuli.js       -> .../TestJuli-Report.html
//             6) TestLog4j.js       -> .../TestLog4j-Report.html
//              7) TestLog4j2.js     -> .../TestLog4j2-Report.html
//              8) LoadTest.js       -> .../LoadTest-Report.html (load runs LAST:
//                                      its 16.5k burst lines must not disturb
//                                      the correctness suites above)
//            This entry point then prints an aggregate pass/fail summary and
//            writes TestAll-Report.md / TestAll-Report.html / TestAll-CI-Summary.md.
//            The five replacement-layer vendor suites run in the same JVM too
//            (they only System.exit when JSPLUS_FACADE_EXIT=1, i.e. when driven
//            directly by TestFacades.bat/.sh), so this report covers the whole
//            44-case surface: 15 console/config + 29 vendor.
//            LoadTest adds its own 6 load cases (burst throughput, zero loss,
//            large payload, throwable load, multi-logger interleave, level
//            filtering under load), so the full surface is 50 cases.
// REMOVED  : TestDatasource.js (false positive: a scope note that always passed,
//            Clock getters only, unrelated to this layer), TestCache.js /
//            TestFile.js / TestTool.js (engine packages com.killcoding.cache /
//            .file / .tool - not what this replacement layer ships).
// NOTE     : Each suite calls beginTest() and reports its own counters, so
//            TestAll captures the counters right after each load() completes.
//            The suites NEVER call System.exit(); TestAll only exits when the
//            JSPLUS_TEST_EXIT env var is set (TestAll.bat/.sh), which lets an
//            interactive Console.eval(...) run finish without killing the console.
// PREREQ   : <PROJECT_ROOT>/JsPlus/config/Logger.properties exists (the runtime
//            config anchor that ScriptConsole loads at startup).
// ============================================================================

var System = Java.type("java.lang.System");
var testStart = Date.now();
var basePath = System.getProperty("JS_PLUS_PATH");

try {
    if (basePath === null || basePath === undefined || basePath.length === 0) {
        throw "JS_PLUS_PATH is not set - cannot locate the test suites";
    }
} catch (e) {
    print("[ERROR] " + e);
    throw e;
}

print("=== JsPlus-LogReplaced TestAll (unified entry) ===");
print("JS_PLUS_PATH: " + System.getProperty("JS_PLUS_PATH"));
print("Suites       : TestConsole -> TestLog");
print("              -> TestSlf4j -> TestCommonsLogging -> TestJuli -> TestLog4j -> TestLog4j2");
print("              -> LoadTest (last: 16.5k burst lines, async-flush assertions)");

var suites = [
    { file: "TestConsole.js",        title: "TestConsole" },
    { file: "TestLog.js",            title: "TestLog" },
    // Replacement-layer vendor suites (one per logging family). They do not
    // exit here: their System.exit only fires under JSPLUS_FACADE_EXIT=1.
    { file: "TestSlf4j.js",          title: "TestSlf4j" },
    { file: "TestCommonsLogging.js", title: "TestCommonsLogging" },
    { file: "TestJuli.js",           title: "TestJuli" },
    { file: "TestLog4j.js",          title: "TestLog4j" },
    { file: "TestLog4j2.js",         title: "TestLog4j2" },
    // Load suite LAST: it enqueues ~16.5k lines (burst, 8 KB payloads,
    // throwables, multi-logger, level gate), so it must not run before any
    // suite that asserts on freshly written lines. Its System.exit only fires
    // under JSPLUS_LOADTEST_EXIT=1 (test/LoadTest.bat / .sh).
    { file: "LoadTest.js",           title: "LoadTest" }
];

var totalPass = 0;
var totalFail = 0;
var allFailures = [];
var suiteResults = [];

// ---- Runtime config self-check (fail fast & visibly) ----
// The logging suite depends on <JS_PLUS_PATH>/config/Logger.properties being
// loaded by ScriptConsole.initEnv(); verify the anchor exists up front and
// surface the result in the console + aggregate reports.
var loggerPropsPath = basePath + "config/Logger.properties";
var cfgCheck = { ok: false, detail: "" };
try {
    var File = Java.type("java.io.File");
    var cfgFile = new File(loggerPropsPath);
    if (cfgFile.exists() && cfgFile.length() > 0) {
        cfgCheck.ok = true;
        cfgCheck.detail = loggerPropsPath + " (" + cfgFile.length() + " bytes)";
    } else {
        cfgCheck.detail = loggerPropsPath + " missing or empty";
    }
} catch (e) {
    cfgCheck.detail = "" + e;
}
print("");
print("=== Config self-check (JsPlus/config/Logger.properties) ===");
if (cfgCheck.ok) {
    totalPass++;
    print("[PASS] " + cfgCheck.detail);
} else {
    totalFail++;
    allFailures.push("Runtime config self-check - " + cfgCheck.detail);
    print("[FAIL] " + cfgCheck.detail);
}

for (var i = 0; i < suites.length; i++) {
    var s = suites[i];
    print("");
    print("===== Running " + s.title + " =====");
    // A throwing suite must NOT abort the whole run: record it as a failed
    // suite and continue, so TestAll always reaches the exit below (otherwise
    // the non-daemon DetectSystemExit thread keeps this JVM alive forever).
    var suitePass = 0;
    var suiteFail = 0;
    var suiteErr = "";
    try {
        load(basePath + "../test/" + s.file);
        // Capture the suite's own counters immediately after it finishes
        // (the next suite's beginTest() would reset them).
        suitePass = passCount;
        suiteFail = failCount;
        for (var j = 0; j < failures.length; j++) {
            allFailures.push(s.title + " - " + failures[j]);
        }
    } catch (e) {
        suiteErr = "" + e;
        suiteFail = 1;
        allFailures.push(s.title + " - suite load/run exception: " + e);
        print("[ERROR] " + s.title + " failed: " + e);
        if (e !== null && e !== undefined && e.stack !== undefined && e.stack !== null) {
            print("[ERROR] stack: " + e.stack);
        }
    }
    totalPass += suitePass;
    totalFail += suiteFail;
    suiteResults.push({ title: s.title, pass: suitePass, fail: suiteFail });
    print(s.title + " => " + suitePass + " passed, " + suiteFail + " failed"
        + (suiteErr.length > 0 ? " (" + suiteErr + ")" : ""));
}

print("");
print("========================================");
print("TestAll result: " + totalPass + " passed, " + totalFail + " failed");
if (totalFail > 0) {
    print("FAILED checks:");
    for (var k = 0; k < allFailures.length; k++) {
        print("  " + allFailures[k]);
    }
} else {
    print("ALL TESTS PASSED");
}

// Aggregate markdown + HTML summary into <PROJECT_ROOT>/docs/latest/report/
try {
    var reportDir = new File(basePath + "../docs/latest/report");
    if (!reportDir.exists()) {
        reportDir.mkdirs();
    }
    var md = "# JsPlus-LogReplaced TestAll Report\n\n";
    md += "Generated: " + new Date().toString() + "\n\n";
    md += "**" + totalPass + " passed / " + totalFail + " failed**\n\n";
    md += "## Config Self-Check\n\n"
        + (cfgCheck.ok ? "- PASS: " + cfgCheck.detail : "- FAIL: " + cfgCheck.detail) + "\n\n";
    md += "| Suite | Passed | Failed |\n";
    md += "| --- | --- | --- |\n";
    for (var m = 0; m < suiteResults.length; m++) {
        md += "| " + suiteResults[m].title + " | " + suiteResults[m].pass + " | " + suiteResults[m].fail + " |\n";
    }
    var reportPath = basePath + "../docs/latest/report/TestAll-Report.md";
    var report = new DiskFile(reportPath);
    report.write(md, "UTF-8");
    print("Markdown report written to " + reportPath);

    var html = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<title>JsPlus-LogReplaced TestAll Report</title>\n"
        + "<style>\n"
        + "  body { font-family: Consolas, 'Courier New', monospace; background: #f5f6f8; margin: 24px; }\n"
        + "  h1 { font-size: 20px; color: #24292f; }\n"
        + "  .meta { color: #57606a; margin-bottom: 12px; }\n"
        + "  .summary-pass { display: inline-block; padding: 8px 16px; border-radius: 6px; background: #e6ffec; color: #1a7f37; font-weight: bold; margin-bottom: 16px; }\n"
        + "  .summary-fail { display: inline-block; padding: 8px 16px; border-radius: 6px; background: #ffebe9; color: #b42318; font-weight: bold; margin-bottom: 16px; }\n"
        + "  table { border-collapse: collapse; width: 100%; background: #fff; }\n"
        + "  th { background: #eaeef2; text-align: left; padding: 8px 10px; border: 1px solid #d0d7de; }\n"
        + "  td { padding: 6px 10px; border: 1px solid #d0d7de; vertical-align: top; }\n"
        + "  pre.cfg { background: #fff; border: 1px solid #d0d7de; padding: 10px; font-size: 12px; overflow-x: auto; }\n"
        + "</style>\n</head>\n<body>\n"
        + "<h1>JsPlus-LogReplaced TestAll Report</h1>\n"
        + "<div class=\"meta\">Generated: " + new Date().toString() + "</div>\n"
        + "<div class=\"" + (totalFail === 0 ? "summary-pass" : "summary-fail") + "\">" + totalPass + " passed / " + totalFail + " failed</div>\n"
        + "<h2>Config Self-Check</h2>\n"
        + "<div class=\"" + (totalFail === 0 ? "summary-pass" : "summary-fail") + "\">"
        + (cfgCheck.ok ? "PASS: " + cfgCheck.detail : "FAIL: " + cfgCheck.detail) + "</div>\n"
        + "<table>\n<tr><th>Suite</th><th>Passed</th><th>Failed</th></tr>\n";
    for (var m2 = 0; m2 < suiteResults.length; m2++) {
        html += "<tr><td>" + suiteResults[m2].title + "</td><td>" + suiteResults[m2].pass + "</td><td>" + suiteResults[m2].fail + "</td></tr>\n";
    }
    html += "</table>\n</body>\n</html>\n";
    var htmlPath = basePath + "../docs/latest/report/TestAll-Report.html";
    var report2 = new DiskFile(htmlPath);
    report2.write(html, "UTF-8");
    print("HTML report written to " + htmlPath);

    // CI summary (from TestBase.writeCiSummary) - compact Markdown that the
    // workflow appends to $GITHUB_STEP_SUMMARY (visible on the Actions run page)
    // and that ships inside the uploaded docs/latest/report/ artifact.
    load(basePath + "../test/TestBase.js");
    writeCiSummary({
        title: "JsPlus-LogReplaced CI Summary",
        reportName: "TestAll-CI-Summary.md",
        pass: totalPass,
        fail: totalFail,
        failures: allFailures,
        suiteTable: suiteResults,
        configOk: cfgCheck.ok,
        configMissing: cfgCheck.ok ? [] : [cfgCheck.detail],
        durationMs: Date.now() - testStart
    });
} catch (e) {
    print("[WARN] Could not write TestAll report: " + e);
    if (e !== null && e !== undefined && e.stack !== undefined && e.stack !== null) {
        print("[WARN] stack: " + e.stack);
    }
}

// TestAll.bat/.sh set JSPLUS_TEST_EXIT=1 so this JVM exits after the task.
// System.exit(0) only terminates this process - JsPlus-LogReplaced has no ghost
// console to keep alive. Interactive Console.eval runs skip the exit.
// When a check fails the process still exits 0 (known P1 issue); the aggregate
// line above is the authoritative result for now.
var testExit = System.getenv("JSPLUS_TEST_EXIT");
if (testExit !== null && testExit !== undefined && ("" + testExit) === "1") {
    System.exit(0);
}
