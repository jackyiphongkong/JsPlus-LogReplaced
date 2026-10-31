#!/bin/bash
set -e

# ------------------------------------------------------------------
# JsPlus-LogReplaced automated test suite - TestAll.sh
# Flow:
#   1) Build the runtime classpath from every JAR under
#      <PROJECT_ROOT>/LibHome - script engines + deps, including the
#      project JAR produced by 'ant build':
#      <PROJECT_ROOT>/LibHome/jsplus/default/jsplus-logreplaced-<version>.jar
#      (the org.* logging-replacement layer; com.killcoding.* comes from the jsplus-core-<version>.jar
#      in the same LibHome/jsplus/default folder; version auto-bumped from <PROJECT_ROOT>/version.properties).
#   2) Run TestAll.js in a standalone JsPlus console in THIS JVM
#      (com.killcoding.jsplus.Main, same entry the JsPlus-Tomcat suite uses).
#      No external server, H2 or Tomcat is required.
#      JS_PLUS_PATH=<PROJECT_ROOT>/JsPlus/ (runtime anchor), so the suites
#      resolve <PROJECT_ROOT>/test/TestBase.js + suites and write reports to
#      <PROJECT_ROOT>/docs/latest/report/ (logs land in <PROJECT_ROOT>/logs/app/).
#      Stdin is redirected to /dev/null so com.killcoding.jsplus.Main exits
#      right after the -Task completes (otherwise it blocks on the
#      interactive scanner, see Main.java scanner loop).
#   3) Exit with the JVM's exit code.
# Override via env (same convention as JsPlus-Tomcat):
#      JsPlus_JavaDir_JsPlus-LogReplaced   - JDK directory under Java/ (default: default)
#      JsPlus_JvmParams_JsPlus-LogReplaced - JVM params (default: -Xms256m -Xmx1024m -Xss256k)
#      SCRIPT_ENGINE (arg $1)       - default: graalvm.js
# ------------------------------------------------------------------

if [ -z "$1" ]; then
    SCRIPT_ENGINE=graalvm.js
else
    SCRIPT_ENGINE="$1"
fi

BIN_PATH="$(cd "$(dirname "$0")" && pwd)/"
JS_PLUS_PATH_ORIGIN="${BIN_PATH}../JsPlus/"
export JS_PLUS_PATH="${JS_PLUS_PATH_ORIGIN//\\//}"
LIB_HOME="${JS_PLUS_PATH}../LibHome/"

# ------------------------------------------------------------------
# Step 0: clean the run-scoped scratch + log output so every run starts
# from a clean tree. JsPlus/.tmp holds the evalJavaSource/evalJavaFile
# scratch (compiled temp classes + MD5 source dirs), so stale MD5 dirs
# cannot leak between runs (same as JsPlus-Core Step 0b). logs/* is the
# engine's output tree (logs/app/<yyyyMMdd>.log); a clean tree also keeps
# the suites honest - they assert on freshly written lines, so lines
# left by a previous run could otherwise satisfy a check by accident.
# ------------------------------------------------------------------
if [ -d "${JS_PLUS_PATH_ORIGIN}.tmp" ]; then
    rm -rf "${JS_PLUS_PATH_ORIGIN}.tmp"
    echo "[TestAll] Removed JsPlus/.tmp"
fi
if [ -d "${JS_PLUS_PATH_ORIGIN}../logs" ]; then
    rm -rf "${JS_PLUS_PATH_ORIGIN}../logs"/* 2>/dev/null || true
    echo "[TestAll] Cleared logs/*"
fi

DEPEND_LIBS="$(find "${LIB_HOME}" -type f -name '*.jar' | tr '\n' ':')"
DEPEND_LIBS="${DEPEND_LIBS%:}"
if [ -z "$DEPEND_LIBS" ]; then
    echo "Error: No JARs found under LibHome. Run 'ant build' first (produces LibHome/jsplus/default/jsplus-logreplaced-<version>.jar)."
    exit 1
fi
CORE_JAR="$(ls "${LIB_HOME}jsplus/default/"/jsplus-logreplaced-*.jar 2>/dev/null | head -n 1 || true)"
if [ -z "$CORE_JAR" ]; then
    echo "WARN: LibHome/jsplus/default/jsplus-logreplaced-*.jar not found. Run 'ant build' first."
fi

# The JDK directory name under Java/ comes from per-script env var
# JsPlus_JavaDir_<AppName> (default: default).
AppName="$(basename "$(cd "${JS_PLUS_PATH}/.." && pwd)")"
JavaDirEnvName="JsPlus_JavaDir_${AppName}"
JAVA_DIR="$(printenv "$JavaDirEnvName" || true)"
JAVA_DIR="${JAVA_DIR:-default}"
JAVA_BIN="${JS_PLUS_PATH}../Java/${JAVA_DIR}/bin/java"

if [ ! -f "$JAVA_BIN" ]; then
    echo "WARN: Bundled Java not found at $JAVA_BIN, falling back to system Java..."
    if [ -n "$JAVA_HOME" ]; then
        JAVA_BIN="$JAVA_HOME/bin/java"
    else
        JAVA_BIN="java"
    fi
fi

JvmParamsName="JsPlus_JvmParams_${AppName}"
JvmParams="$(printenv "$JvmParamsName" || true)"

echo "$JvmParamsName"

if [ -z "$JvmParams" ]; then
    JvmParams="-Xms256m -Xmx1024m -Xss256k"
    echo "WARN: Missing ${JvmParamsName} then using default params"
else
    echo "Using System Env JVM Params"
fi

echo "Using \"$JvmParams\""

# ------------------------------------------------------------------
# Step 0b: stop any existing services, then make sure H2 is listening
# before the suites run. The suites' dbVerify assertions read
# app_logger_msg through the TCP pool (logdb on :H2_PORT), so a dead or
# half-dead H2 fails the persistence checks. Same shape as
# JsPlus-Tomcat/TestAll.sh Step 0/1: StopAll -> H2 init -> BG_StartH2.
# ------------------------------------------------------------------
export H2_PORT="${JsPlus_H2_PORT:-3308}"
echo "[TestAll] H2 port: $H2_PORT"

echo "[TestAll] Stopping existing services..."
"${JS_PLUS_PATH}bin/StopAll.sh" || true

echo "[TestAll] Initializing database logdb..."
if ! "${JS_PLUS_PATH}bin/StartH2Console.sh" init logdb; then
    echo "[TestAll] ERROR: Database initialization failed."
    exit 1
fi

# Start + wait with ONE in-place retry: a just-killed H2 can still hold the
# port in TIME_WAIT, so the first start never binds - the known flakiness of
# this project's H2 bootstrap.
H2_READY=0
for attempt in 1 2; do
    if [ "$H2_READY" = "1" ]; then
        break
    fi
    echo "[TestAll] Starting H2 server on port $H2_PORT, attempt $attempt ..."
    "${JS_PLUS_PATH}bin/BG_StartH2.sh" "$SCRIPT_ENGINE" || true
    for i in $(seq 1 30); do
        if (netstat -tln 2>/dev/null | grep -q ":$H2_PORT .*LISTEN") || (ss -tln 2>/dev/null | grep -q ":$H2_PORT .*LISTEN"); then
            H2_READY=1
            break
        fi
        sleep 1
    done
    if [ "$H2_READY" != "1" ]; then
        echo "[TestAll] WARN: H2 not listening after 30s, retrying..."
        "${JS_PLUS_PATH}bin/StopAll.sh" || true
    fi
done
if [ "$H2_READY" != "1" ]; then
    echo "[TestAll] ERROR: H2 did not start on port $H2_PORT after 2 attempts."
    exit 1
fi
echo "[TestAll] H2 ready."

# ------------------------------------------------------------------
# Run TestAll.js against this JVM.
# JSPLUS_TEST_EXIT=1 tells TestAll.js to System.exit(0) after finishing
# (interactive Console.eval runs skip it). "< /dev/null" closes stdin: the
# JsPlus Main scanner gets EOF right after the -Task finishes and the JVM
# exits cleanly (Main.java).
# NOTE: set -e is suspended around the java call so a non-zero JVM exit is
# captured and propagated explicitly (instead of the shell dying silently).
# ------------------------------------------------------------------
export JSPLUS_TEST_EXIT=1
set +e
"$JAVA_BIN" $JvmParams -cp "$DEPEND_LIBS" com.killcoding.jsplus.Main -ScriptEngine "$SCRIPT_ENGINE" -Env script -WriteHistory false -Task "${JS_PLUS_PATH}../test/TestAll.js" < /dev/null
RC=$?
set -e

# ------------------------------------------------------------------
# Step 2b: Archive the test reports under the same docs layout as the
# javadoc site: docs/reports/<version>.jar (one jar per run) plus
# docs/latest/report/ as the browsable newest reports. Version comes from
# the project jar name. A failure to archive is only a WARN and never
# changes the suite exit code.
# ------------------------------------------------------------------
PROJ_JAR=$(ls -1 "${JS_PLUS_PATH_ORIGIN}../LibHome/jsplus/default/"jsplus-logreplaced-*.jar 2>/dev/null | tail -n 1)
if [ -z "$PROJ_JAR" ]; then
    echo "WARN: Cannot locate jsplus-logreplaced-*.jar, skipping report archive."
else
    PROJ_VERSION=$(basename "$PROJ_JAR" | sed -e "s/^jsplus-logreplaced-//" -e "s/\.jar$//")
    REPORTS_DIR="${JS_PLUS_PATH_ORIGIN}../docs/reports"
    echo "[TestAll] Archiving test reports as docs/reports/${PROJ_VERSION}.jar (docs/latest/report)..."
    mkdir -p "$REPORTS_DIR"
    rm -f "$REPORTS_DIR/${PROJ_VERSION}.jar"
    set +e
    "${JS_PLUS_PATH}../Java/${JAVA_DIR}/bin/jar" cf "$REPORTS_DIR/${PROJ_VERSION}.jar" -C "${JS_PLUS_PATH_ORIGIN}../docs/latest/report" .
    PACK_RC=$?
    set -e
    if [ "$PACK_RC" -eq 0 ]; then
        echo "[TestAll] Packed docs/reports/${PROJ_VERSION}.jar"
    else
        echo "WARN: Failed to pack test report jar (jar missing?), continuing."
    fi
    # Keep only the 10 newest report jars - the archive is owned by TestAll.
    ls -1t "$REPORTS_DIR"/*.jar 2>/dev/null | tail -n +11 | while read -r f; do rm -f "$f"; done
    KEPT=$(ls -1 "$REPORTS_DIR"/*.jar 2>/dev/null | wc -l | tr -d " ")
    echo "[TestAll] Pruned docs/reports to newest 10 (${KEPT} jars kept)"
fi
# ------------------------------------------------------------------
# Step 2c: leave nothing behind. The H2 server started in Step 0b is
# stopped here (same as JsPlus-Tomcat/TestAll.sh Step 3); RC was captured
# right after TestAll.js so this never changes the exit code.
# ------------------------------------------------------------------
echo "[TestAll] Stopping all remaining services..."
"${JS_PLUS_PATH}bin/StopAll.sh" || true

echo "TestAll exit code: $RC"
exit "$RC"
