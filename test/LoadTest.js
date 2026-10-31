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

// ---- host / JVM snapshot (same shape as JsPlus-Tomcat's LoadTest) ----------
// The measured numbers (lines/s, per-call latency) are only comparable between
// runs recorded on the same environment, so the snapshot is archived with the
// result instead of being guessed afterwards.
var Runtime = Java.type("java.lang.Runtime");
var ManagementFactory = Java.type("java.lang.management.ManagementFactory");
var ClassT = Java.type("java.lang.Class");
var ProcessHandle = Java.type("java.lang.ProcessHandle");
var File = Java.type("java.io.File");

function ltRound1(x) { return Math.round(x * 10.0) / 10.0; }
function ltRound2(x) { return Math.round(x * 100.0) / 100.0; }
function ltBytesToMb(b) { return Math.round(b / 1048576.0); }
function ltPct(part, whole) { return whole <= 0 ? 0 : ltRound1(part * 100.0 / whole); }

// JSON-oriented escaping without RegExp literals: this GraalVM build ships no
// TRegex, so any JS regex literal throws "No language for id regex".
function ltEsc(s) {
    return String(s).split("\\").join("\\\\").split('"').join('\\"')
        .split("\r").join("\\r").split("\n").join("\\n");
}

/** HTML-escape for dashboard table cells. */
function ltEscHtml(s) {
    return String(s).split("&").join("&amp;").split("<").join("&lt;")
        .split(">").join("&gt;").split('"').join("&quot;");
}

/**
 * Reads one optional metric from com.sun.management.OperatingSystemMXBean. The
 * concrete implementation lives in com.sun.management.internal (not exported by
 * jdk.management), so the method must be resolved on the exported interface
 * class and then invoked on the bean.
 */
function ltOsMetric(methodNames) {
    var out = null;
    try {
        var iface = ClassT.forName("com.sun.management.OperatingSystemMXBean");
        var bean = ManagementFactory.getOperatingSystemMXBean();
        for (var i = 0; i < methodNames.length && out === null; i++) {
            try {
                var m = iface.getMethod(methodNames[i]);
                var v = m.invoke(bean);
                if (v !== null && v !== undefined) {
                    out = v;
                }
            } catch (e1) {
            }
        }
    } catch (e2) {
    }
    return out;
}

/**
 * Host CPU load. JMX returns 0 on the very first call because it has no previous
 * sample to diff against, so sample a few times and take the first non-zero
 * reading (a genuinely idle host stays at 0).
 */
function ltHostCpuLoad() {
    var best = null;
    for (var i = 0; i < 4; i++) {
        var v = ltOsMetric(["getCpuLoad", "getSystemCpuLoad"]);
        if (v !== null) {
            var n = Number(v);
            if (best === null || n > best) {
                best = n;
            }
            if (n > 0) {
                return n;
            }
        }
        try {
            Thread.sleep(250);
        } catch (e) {
        }
    }
    return best;
}

/** Read-only host/JVM snapshot; anything unavailable is reported as null. */
function collectEnvironment() {
    var rt = Runtime.getRuntime();
    var heapMax = rt.maxMemory();
    var heapUsed = rt.totalMemory() - rt.freeMemory();

    var memTotal = ltOsMetric(["getTotalMemorySize"]);
    var memFree = ltOsMetric(["getFreeMemorySize"]);
    var cpu = ltHostCpuLoad();
    var procCpu = ltOsMetric(["getProcessCpuLoad"]);
    var procs = null;
    try {
        procs = ProcessHandle.allProcesses().count();
    } catch (e) {
    }

    var args = "";
    try {
        var jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        var parts = [];
        for (var i = 0; i < jvmArgs.size(); i++) {
            parts.push(String(jvmArgs.get(i)));
        }
        args = parts.join(" ");
    } catch (e3) {
    }

    return {
        os: System.getProperty("os.name", "") + " " + System.getProperty("os.version", ""),
        osArch: System.getProperty("os.arch", ""),
        cpuCores: rt.availableProcessors(),
        cpuLoadPct: cpu === null ? null : ltRound1(Number(cpu) * 100.0),
        procCpuLoadPct: procCpu === null ? null : ltRound1(Number(procCpu) * 100.0),
        memTotalMb: memTotal === null ? null : ltBytesToMb(Number(memTotal)),
        memFreeMb: memFree === null ? null : ltBytesToMb(Number(memFree)),
        memUsedPct: (memTotal === null || memFree === null)
            ? null : ltPct(Number(memTotal) - Number(memFree), Number(memTotal)),
        heapMaxMb: ltBytesToMb(heapMax),
        heapUsedMb: ltBytesToMb(heapUsed),
        heapUsedPct: ltPct(heapUsed, heapMax),
        javaVersion: System.getProperty("java.version", ""),
        javaVendor: System.getProperty("java.vendor", ""),
        javaVm: System.getProperty("java.vm.name", ""),
        javaHome: System.getProperty("java.home", ""),
        jvmArgs: args,
        hostProcesses: procs === null ? null : Number(procs)
    };
}

function ltEnvJson(env, indent) {
    var j = "";
    j += indent + "\"os\": \"" + ltEsc(env.os) + "\",\n";
    j += indent + "\"osArch\": \"" + ltEsc(env.osArch) + "\",\n";
    j += indent + "\"cpuCores\": " + env.cpuCores + ",\n";
    j += indent + "\"cpuLoadPct\": " + (env.cpuLoadPct === null ? "null" : env.cpuLoadPct) + ",\n";
    j += indent + "\"procCpuLoadPct\": " + (env.procCpuLoadPct === null ? "null" : env.procCpuLoadPct) + ",\n";
    j += indent + "\"memTotalMb\": " + (env.memTotalMb === null ? "null" : env.memTotalMb) + ",\n";
    j += indent + "\"memFreeMb\": " + (env.memFreeMb === null ? "null" : env.memFreeMb) + ",\n";
    j += indent + "\"memUsedPct\": " + (env.memUsedPct === null ? "null" : env.memUsedPct) + ",\n";
    j += indent + "\"heapMaxMb\": " + env.heapMaxMb + ",\n";
    j += indent + "\"heapUsedMb\": " + env.heapUsedMb + ",\n";
    j += indent + "\"heapUsedPct\": " + env.heapUsedPct + ",\n";
    j += indent + "\"javaVersion\": \"" + ltEsc(env.javaVersion) + "\",\n";
    j += indent + "\"javaVendor\": \"" + ltEsc(env.javaVendor) + "\",\n";
    j += indent + "\"javaVm\": \"" + ltEsc(env.javaVm) + "\",\n";
    j += indent + "\"javaHome\": \"" + ltEsc(env.javaHome) + "\",\n";
    j += indent + "\"jvmArgs\": \"" + ltEsc(env.jvmArgs) + "\",\n";
    j += indent + "\"hostProcesses\": " + (env.hostProcesses === null ? "null" : env.hostProcesses) + "\n";
    return j;
}

function ltPercentile(sorted, p) {
    if (sorted.length === 0) {
        return 0;
    }
    var idx = Math.ceil(sorted.length * p / 100.0) - 1;
    if (idx < 0) {
        idx = 0;
    }
    if (idx >= sorted.length) {
        idx = sorted.length - 1;
    }
    return sorted[idx];
}

function ltEnvCell(v, suffix) {
    return (v === null || v === undefined) ? "n/a" : String(v) + (suffix || "");
}

/** CPU cell: a 0% "before" reading is normally the JVM's first JMX sample. */
function ltCpuCell(v, other) {
    if (v === null || v === undefined) {
        return "n/a";
    }
    if (v === 0 && other !== null && other !== undefined && other > 0) {
        return "0% (first sample; see after)";
    }
    return v + "%";
}

function ltHeapCell(e) {
    return e.heapUsedMb + " MB (" + e.heapUsedPct + "%)";
}

/** Before/after table; rows that cannot change during the run span both columns. */
function ltEnvTable(b, a) {
    var sb = "<table><tr><th>Metric</th><th>Before run</th><th>After run</th></tr>\n";
    sb += "<tr><td>OS / arch</td><td>" + ltEscHtml(b.os + " " + b.osArch)
        + "</td><td>" + ltEscHtml(a.os + " " + a.osArch) + "</td></tr>\n";
    sb += "<tr><td>CPU cores</td><td>" + b.cpuCores + "</td><td>" + a.cpuCores + "</td></tr>\n";
    sb += "<tr><td>CPU load (whole host)</td><td>" + ltCpuCell(b.cpuLoadPct, a.cpuLoadPct)
        + "</td><td>" + ltCpuCell(a.cpuLoadPct, b.cpuLoadPct) + "</td></tr>\n";
    sb += "<tr><td>CPU load (this JVM)</td><td>" + ltCpuCell(b.procCpuLoadPct, a.procCpuLoadPct)
        + "</td><td>" + ltCpuCell(a.procCpuLoadPct, b.procCpuLoadPct) + "</td></tr>\n";
    sb += "<tr><td>Physical memory total</td><td colspan=\"2\">"
        + ltEnvCell(b.memTotalMb, " MB") + "</td></tr>\n";
    sb += "<tr><td>Physical memory free</td><td>" + ltEnvCell(b.memFreeMb, " MB")
        + "</td><td>" + ltEnvCell(a.memFreeMb, " MB") + "</td></tr>\n";
    sb += "<tr><td>Physical memory used</td><td>" + ltEnvCell(b.memUsedPct, "%")
        + "</td><td>" + ltEnvCell(a.memUsedPct, "%") + "</td></tr>\n";
    sb += "<tr><td>JVM heap max</td><td colspan=\"2\">" + b.heapMaxMb + " MB</td></tr>\n";
    sb += "<tr><td>JVM heap used</td><td>" + ltHeapCell(b) + "</td><td>" + ltHeapCell(a) + "</td></tr>\n";
    sb += "<tr><td>JDK</td><td colspan=\"2\">" + ltEscHtml(b.javaVersion) + " · " + ltEscHtml(b.javaVendor)
        + " · " + ltEscHtml(b.javaVm) + "</td></tr>\n";
    sb += "<tr><td>JVM args</td><td colspan=\"2\">"
        + (b.jvmArgs === "" ? "(none)" : ltEscHtml(b.jvmArgs)) + "</td></tr>\n";
    sb += "<tr><td>Host processes</td><td>" + ltEnvCell(b.hostProcesses)
        + "</td><td>" + ltEnvCell(a.hostProcesses) + "</td></tr>\n";
    sb += "</table>\n";
    return sb;
}

function ltCard(k, v, cls) {
    return "<div class=\"card " + cls + "\"><div class=\"k\">" + k
        + "</div><div class=\"v\">" + v + "</div></div>\n";
}

/** Vertical SVG bars for per-second enqueue rate. */
function ltSvgBars(tPerSec) {
    var secs = Object.keys(tPerSec).sort(function (a, b) { return Number(a) - Number(b); });
    if (secs.length === 0) {
        return "<div class=\"err\">No samples.</div>";
    }
    var maxV = 0;
    for (var s = 0; s < secs.length; s++) {
        maxV = Math.max(maxV, tPerSec[secs[s]]);
    }
    var W = 900, H = 180, pad = 10, bw = 6, gap = 2;
    var out = "";
    out += "<svg viewBox=\"0 0 " + W + " " + (H + 24) + "\" style=\"width:100%;height:auto\">\n";
    var x = pad;
    for (var s2 = 0; s2 < secs.length; s2++) {
        var v = tPerSec[secs[s2]];
        var h = maxV === 0 ? 0 : Math.max(1, Math.round(v * (H - 20) / maxV));
        var y = H - h;
        out += "<rect x=\"" + x + "\" y=\"" + y + "\" width=\"" + bw + "\" height=\"" + h
            + "\" fill=\"#38bdf8\" rx=\"1\"><title>t+" + secs[s2] + "s: " + v + " lines</title></rect>\n";
        x += bw + gap;
    }
    out += "<text x=\"" + pad + "\" y=\"" + (H + 16)
        + "\" fill=\"#94a3b8\" font-size=\"10\">time (s) since burst start</text>\n";
    out += "</svg>\n";
    return out;
}

/** Horizontal SVG bars for the per-call latency histogram (1 us buckets). */
function ltSvgBars2(tHist) {
    var buckets = Object.keys(tHist).sort(function (a, b) { return Number(a) - Number(b); });
    if (buckets.length === 0) {
        return "<div class=\"err\">No latency samples.</div>";
    }
    var maxV = 0;
    for (var b = 0; b < buckets.length; b++) {
        maxV = Math.max(maxV, tHist[buckets[b]]);
    }
    var out = "<div style=\"max-height:240px;overflow-y:auto\">\n";
    for (var b2 = 0; b2 < buckets.length; b2++) {
        var v = tHist[buckets[b2]];
        var p = maxV === 0 ? 0 : Math.round(v * 100.0 / maxV);
        out += "<div class=\"bar-row\"><div class=\"bar-label\">"
            + buckets[b2] + "+us</div><div class=\"bar-track\"><div class=\"bar-fill\" style=\"width:"
            + p + "%\"></div></div><div style=\"font-size:12px\">" + v + "</div></div>\n";
    }
    out += "</div>\n";
    return out;
}

// Host/JVM snapshot taken before the measured run (archived with the result).
var envBefore = collectEnvironment();



// ---- app-log helpers ----------------------------------------------------
function appLogPath() {
    return basePath + "../logs/app/" + LogRecord.getLogFileName();
}

// Reads the whole app log for today: rotated files (date.N.log) first in
// ascending N, then the active file (date.log) last. The engine rotates at
// FileOutMaxFileSize (5mb); a 16.5k-line burst can straddle that boundary,
// so counting only LogRecord.getLogFileName() would see just the tail in the
// new file and miss the head that was already archived to date.1.log. We
// cannot rely on a fresh-log-tree guarantee here: this suite is also run
// standalone (not only via TestAll.bat), so merge every same-day file.
// String ops only - no JS regex engine in this GraalVM setup.
function readAppLog() {
    var appDirPath = basePath + "../logs/app/";
    var currentName = "" + LogRecord.getLogFileName();   // e.g. "20261031.log"
    var dotIdx = currentName.lastIndexOf(".");
    var datePrefix = dotIdx > 0 ? currentName.substring(0, dotIdx) : currentName;

    function rotRank(n) {
        if (n === currentName) return 1000000;            // active file goes last
        var fd = n.indexOf(".");
        var ld = n.lastIndexOf(".");
        if (fd > 0 && ld > fd) {
            var v = parseInt(n.substring(fd + 1, ld), 10);
            return isNaN(v) ? 0 : v;
        }
        return 0;
    }

    var out = "";
    var listed = false;
    try {
        var Files = Java.type("java.nio.file.Files");
        var Paths = Java.type("java.nio.file.Paths");
        var Charset = Java.type("java.nio.charset.StandardCharsets");
        var dir = Paths.get(appDirPath);
        if (Files.isDirectory(dir)) {
            var names = [];
            var stream = Files.list(dir);
            try {
                var it = stream.iterator();
                while (it.hasNext()) {
                    var p = "" + it.next().getFileName().toString();
                    if (p.length > datePrefix.length
                        && p.substring(0, datePrefix.length) === datePrefix
                        && p.endsWith(".log")) {
                        names.push(p);
                    }
                }
            } finally {
                stream.close();
            }
            names.sort(function (a, b) { return rotRank(a) - rotRank(b); });
            for (var i = 0; i < names.length; i++) {
                try {
                    var s = "" + Files.readString(Paths.get(appDirPath + names[i]), Charset.UTF_8);
                    if (out.length > 0 && out.charAt(out.length - 1) !== "\n") out += "\n";
                    out += s;
                } catch (eRead) {
                    // one file locked mid-rotation - skip it, keep the rest
                }
            }
            listed = true;
        }
    } catch (eList) {
        listed = false;
    }
    if (listed && out.length > 0) return out;

    // Fallback: pure single-file read (matches the old behaviour).
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
// Chunked so the per-call cost can be sampled: the engine writes asynchronously,
// so a log call should cost microseconds and never wait for the disk or the DB.
// latencySamples and perSec feed the JSON + dashboard that the JsPlus-Tomcat
// style LoadTest archives next to the correctness report.
var latencySamples = [];
var perSec = {};
var LT_CHUNK = 100;
var burstStart = System.nanoTime();
for (var b = 0; b < N_BURST; b += LT_CHUNK) {
    var chunkEnd = Math.min(b + LT_CHUNK, N_BURST);
    var chunkStart = System.nanoTime();
    for (var c = b; c < chunkEnd; c++) {
        log.info("L1 " + runTag + "#" + c);
    }
    var chunkMs = (System.nanoTime() - chunkStart) / 1000000.0;
    latencySamples.push(chunkMs / (chunkEnd - b));
    var secIdx = Math.floor((System.nanoTime() - burstStart) / 1000000000);
    perSec[secIdx] = (perSec[secIdx] === undefined ? 0 : perSec[secIdx]) + (chunkEnd - b);
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

// dbVerify
// dbVerify on the persisted state: the engine's DatabaseOut path writes these
// rows into logdb.app_logger_msg (async flush, OutLogTimer=1s), so poll until they
// appear instead of assuming a fixed delay. Console.getDataSource() is the
// datasource the engine writes through (config/DataSource.properties -> logdb).
var dbSql = "select count(*) as TOTAL from app_logger_msg where message like '%" + runTag + "%'";
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
    name: "load rows persisted in app_logger_msg (dbVerify)",
    ok: dbCount > 0,
    detail: "rows matching the suite tag=" + dbCount,
    objective: "Verify the load log calls are persisted into app_logger_msg by the engine's DatabaseOut path",
    objectiveZh: "驗證 load 的日誌呼叫由引擎 DatabaseOut 路徑寫入 app_logger_msg",
    command: "dbVerify(\"" + dbSql + "\")"
});
writeReport();
writeMd();

// test/LoadTest.bat / .sh set JSPLUS_LOADTEST_EXIT=1 so a standalone run is
// CI-usable (exit 1 when a case fails). Inside TestAll this stays unset, so the
// suite returns to the aggregate runner instead of killing the JVM.
// ============================================================================
// Result archive - JSON + offline dashboard, same shape as JsPlus-Tomcat's
// LoadTest: an environment snapshot taken before AND after the run, throughput,
// latency percentiles, and a static dark-theme dashboard. The numbers are only
// comparable between runs recorded on the same environment, so the snapshot is
// archived with the result instead of being reconstructed later.
// ============================================================================
var ltEnvAfter = collectEnvironment();

var ltLinesTotal = N_BURST + N_BIG + N_EX + (N_LOGGERS * N_PER_LOGGER) + N_FILTER + N_WARN;
var ltLatSorted = [];
for (var ltI = 0; ltI < latencySamples.length; ltI++) {
    ltLatSorted.push(latencySamples[ltI] * 1000.0);   // ms -> us
}
ltLatSorted.sort(function (a, b) { return a - b; });

var ltP50 = ltPercentile(ltLatSorted, 50);
var ltP90 = ltPercentile(ltLatSorted, 90);
var ltP95 = ltPercentile(ltLatSorted, 95);
var ltP99 = ltPercentile(ltLatSorted, 99);
var ltMax = ltLatSorted.length === 0 ? 0 : ltLatSorted[ltLatSorted.length - 1];
var ltAvg = 0;
if (ltLatSorted.length > 0) {
    var ltSum = 0;
    for (var ltS = 0; ltS < ltLatSorted.length; ltS++) {
        ltSum += ltLatSorted[ltS];
    }
    ltAvg = ltSum / ltLatSorted.length;
}

// latency histogram, 1 us buckets
var ltHist = {};
for (var ltH = 0; ltH < ltLatSorted.length; ltH++) {
    var ltBucket = Math.floor(ltLatSorted[ltH]);
    ltHist[ltBucket] = (ltHist[ltBucket] === undefined ? 0 : ltHist[ltBucket]) + 1;
}

var ltSdf = Java.type("java.text.SimpleDateFormat");
var ltNow = new ltSdf("yyyy-MM-dd HH:mm:ss").format(new Date());

// ---- JSON ----------------------------------------------------------------
var ltJson = "";
ltJson += "{\n";
ltJson += "  \"suite\": \"JsPlus-LogReplaced LoadTest\",\n";
ltJson += "  \"runTag\": \"" + ltEsc(runTag) + "\",\n";
ltJson += "  \"generatedAt\": \"" + ltEsc("" + ltNow) + "\",\n";
ltJson += "  \"profile\": \"log throughput (async engine pipeline)\",\n";
ltJson += "  \"loadShape\": {\n";
ltJson += "    \"burst\": " + N_BURST + ",\n";
ltJson += "    \"bigPayload\": " + N_BIG + ",\n";
ltJson += "    \"bigPayloadChars\": " + BIG_SIZE + ",\n";
ltJson += "    \"exceptions\": " + N_EX + ",\n";
ltJson += "    \"loggers\": " + N_LOGGERS + ",\n";
ltJson += "    \"perLogger\": " + N_PER_LOGGER + ",\n";
ltJson += "    \"filteredInfo\": " + N_FILTER + ",\n";
ltJson += "    \"warn\": " + N_WARN + "\n";
ltJson += "  },\n";
ltJson += "  \"metrics\": {\n";
ltJson += "    \"linesTotal\": " + ltLinesTotal + ",\n";
ltJson += "    \"burstMs\": " + ltRound1(burstMs) + ",\n";
ltJson += "    \"linesPerSec\": " + Math.round(burstRate) + ",\n";
ltJson += "    \"callSampleCount\": " + ltLatSorted.length + ",\n";
ltJson += "    \"avgUs\": " + ltRound2(ltAvg) + ",\n";
ltJson += "    \"p50Us\": " + ltRound2(ltP50) + ",\n";
ltJson += "    \"p90Us\": " + ltRound2(ltP90) + ",\n";
ltJson += "    \"p95Us\": " + ltRound2(ltP95) + ",\n";
ltJson += "    \"p99Us\": " + ltRound2(ltP99) + ",\n";
ltJson += "    \"maxUs\": " + ltRound2(ltMax) + ",\n";
ltJson += "    \"casesPassed\": " + passCount + ",\n";
ltJson += "    \"casesFailed\": " + failCount + "\n";
ltJson += "  },\n";
ltJson += "  \"perSecond\": {";
var ltPerSecKeys = Object.keys(perSec).sort(function (a, b) { return Number(a) - Number(b); });
for (var ltK = 0; ltK < ltPerSecKeys.length; ltK++) {
    if (ltK > 0) {
        ltJson += ", ";
    }
    ltJson += "\"" + ltPerSecKeys[ltK] + "\": " + perSec[ltPerSecKeys[ltK]];
}
ltJson += "},\n";
ltJson += "  \"histogram\": {";
var ltHistKeys = Object.keys(ltHist).sort(function (a, b) { return Number(a) - Number(b); });
for (var ltK2 = 0; ltK2 < ltHistKeys.length; ltK2++) {
    if (ltK2 > 0) {
        ltJson += ", ";
    }
    ltJson += "\"" + ltHistKeys[ltK2] + "\": " + ltHist[ltHistKeys[ltK2]];
}
ltJson += "},\n";
ltJson += "  \"environment\": {\n";
ltJson += "    \"before\": {\n" + ltEnvJson(envBefore, "      ") + "    },\n";
ltJson += "    \"after\": {\n" + ltEnvJson(ltEnvAfter, "      ") + "    }\n";
ltJson += "  }\n";
ltJson += "}\n";

// ---- dashboard -----------------------------------------------------------
var ltHtml = ltBuildHtml(ltLinesTotal, Math.round(burstRate), ltRound2(ltAvg),
    ltRound2(ltP50), ltRound2(ltP90), ltRound2(ltP95), ltRound2(ltP99), ltRound2(ltMax),
    ltLatSorted.length, passCount, failCount, perSec, ltHist, envBefore, ltEnvAfter,
    runTag, "" + ltNow);

// ---- write into docs/latest/report (same folder as the suite reports) -----
try {
    var ltFiles = Java.type("java.nio.file.Files");
    var ltPaths = Java.type("java.nio.file.Paths");
    var ltCharsets = Java.type("java.nio.charset.StandardCharsets");
    var ltDir = new File(basePath + "../docs/latest/report");
    if (!ltDir.exists()) {
        ltDir.mkdirs();
    }
    var ltDirPath = basePath + "../docs/latest/report/";
    ltFiles.writeString(ltPaths.get(ltDirPath + "loadtest-result.json"), ltJson, ltCharsets.UTF_8);
    print("LoadTest JSON written to " + ltDirPath + "loadtest-result.json");
    ltFiles.writeString(ltPaths.get(ltDirPath + "loadtest-dashboard.html"), ltHtml, ltCharsets.UTF_8);
    print("LoadTest dashboard written to " + ltDirPath + "loadtest-dashboard.html");
} catch (eLt) {
    print("[WARN] Could not write load test JSON/dashboard: " + eLt);
}

print("Result: lines=" + ltLinesTotal + " burst=" + Math.round(burstRate) + " lines/s"
    + " avg=" + ltRound2(ltAvg) + "us p50=" + ltRound2(ltP50) + "us p90=" + ltRound2(ltP90)
    + "us p99=" + ltRound2(ltP99) + "us max=" + ltRound2(ltMax) + "us");

// ============================================================================
// Dashboard builder - fully static: inline SVG/CSS only, no JS, no CDN.
// ============================================================================
function ltBuildHtml(tLines, tRate, tAvg, tP50, tP90, tP95, tP99, tMax,
                     tSampleCount, tPass, tFail, tPerSec, tHist, tEnvBefore, tEnvAfter,
                     tRunTag, tTs) {
    var sb = "";
    sb += "<!DOCTYPE html>\n<html lang=\"zh-HK\">\n<head>\n<meta charset=\"UTF-8\">\n";
    sb += "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n";
    sb += "<title>Load Test Dashboard</title>\n";
    sb += "<style>\n";
    sb += "body{font-family:Segoe UI,Arial,sans-serif;background:#0f172a;color:#e2e8f0;margin:0;padding:24px;}\n";
    sb += "h1{font-size:22px;margin:0 0 4px;color:#f8fafc;}\n";
    sb += ".sub{color:#94a3b8;font-size:13px;margin-bottom:20px;}\n";
    sb += ".cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px;margin-bottom:20px;}\n";
    sb += ".card{background:#1e293b;border:1px solid #334155;border-radius:10px;padding:14px;}\n";
    sb += ".card .k{font-size:11px;color:#94a3b8;text-transform:uppercase;letter-spacing:.5px;}\n";
    sb += ".card .v{font-size:24px;font-weight:600;margin-top:4px;color:#f8fafc;}\n";
    sb += ".card.ok .v{color:#34d399;}\n.card.bad .v{color:#f87171;}\n";
    sb += ".panel{background:#1e293b;border:1px solid #334155;border-radius:10px;padding:16px;margin-bottom:16px;}\n";
    sb += ".panel h2{font-size:15px;margin:0 0 12px;color:#cbd5e1;}\n";
    sb += "table{border-collapse:collapse;width:100%;font-size:13px;}\n";
    sb += "th,td{text-align:left;padding:6px 10px;border-bottom:1px solid #334155;}\n";
    sb += "th{color:#94a3b8;font-weight:500;}\n";
    sb += ".err{color:#f87171;font-family:Consolas,monospace;font-size:12px;}\n";
    sb += ".bar-row{display:flex;align-items:center;gap:8px;margin-bottom:4px;}\n";
    sb += ".bar-label{width:70px;font-size:11px;color:#94a3b8;text-align:right;}\n";
    sb += ".bar-track{flex:1;background:#0f172a;border-radius:4px;height:16px;overflow:hidden;}\n";
    sb += ".bar-fill{height:100%;background:#38bdf8;border-radius:4px;}\n";
    sb += "</style>\n</head>\n<body>\n";

    sb += "<h1>Load Test Dashboard</h1>\n";
    sb += "<div class=\"sub\">Suite: <b>JsPlus-LogReplaced LoadTest</b> &nbsp;|&nbsp; "
        + "Run tag: <b>" + ltEscHtml(tRunTag) + "</b> &nbsp;|&nbsp; "
        + "Profile: <b>log throughput (async engine pipeline)</b> &nbsp;|&nbsp; "
        + "Run at: <b>" + ltEscHtml(tTs) + "</b></div>\n";

    sb += "<div class=\"cards\">\n";
    sb += ltCard("Lines Enqueued", String(tLines), "");
    sb += ltCard("Lines/s", String(tRate), "");
    sb += ltCard("Avg / call", tAvg + " µs", "");
    sb += ltCard("P50", tP50 + " µs", "");
    sb += ltCard("P90", tP90 + " µs", "");
    sb += ltCard("P99", tP99 + " µs", "");
    sb += ltCard("Max", tMax + " µs", "");
    sb += ltCard("Cases", tPass + " / " + (tPass + tFail), tFail > 0 ? "bad" : "ok");
    sb += "</div>\n";

    sb += "<div class=\"panel\"><h2>Throughput (lines enqueued per second, L1 burst)</h2>\n";
    sb += ltSvgBars(tPerSec);
    sb += "</div>\n";

    sb += "<div class=\"panel\"><h2>Per-call Latency Distribution (1 µs buckets, "
        + tSampleCount + " samples)</h2>\n";
    sb += ltSvgBars2(tHist);
    sb += "</div>\n";

    sb += "<div class=\"panel\"><h2>Per-call Latency Percentiles</h2>\n";
    sb += "<table><tr><th>Metric</th><th>Value</th></tr>\n";
    sb += "<tr><td>Average</td><td>" + tAvg + " µs</td></tr>\n";
    sb += "<tr><td>P50</td><td>" + tP50 + " µs</td></tr>\n";
    sb += "<tr><td>P90</td><td>" + tP90 + " µs</td></tr>\n";
    sb += "<tr><td>P95</td><td>" + tP95 + " µs</td></tr>\n";
    sb += "<tr><td>P99</td><td>" + tP99 + " µs</td></tr>\n";
    sb += "<tr><td>Max</td><td>" + tMax + " µs</td></tr>\n";
    sb += "</table>\n</div>\n";

    sb += "<div class=\"panel\"><h2>Runtime Environment</h2>\n";
    sb += ltEnvTable(tEnvBefore, tEnvAfter);
    sb += "<div class=\"sub\" style=\"margin:10px 0 0;line-height:1.5\">"
        + "lines/s 與每次呼叫延遲只宜在同一個環境下互相比較：CPU 核數、可用實體記憶體、"
        + "JVM heap、背景進程數，以及 logs/app 所在磁碟的寫入速度都會直接影響讀數。"
        + "本區塊記錄的是該次量測當時的實際狀態，同一份 JSON 的 "
        + "<code>environment.before / environment.after</code> 亦保留同樣資料。"
        + "</div>\n";
    sb += "</div>\n";

    sb += "</body>\n</html>\n";
    return sb;
}

var loadExit = System.getenv("JSPLUS_LOADTEST_EXIT");
if (loadExit !== null && loadExit !== undefined && ("" + loadExit) === "1") {
    System.exit(failCount > 0 ? 1 : 0);
}
