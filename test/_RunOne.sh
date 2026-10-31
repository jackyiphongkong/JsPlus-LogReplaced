#!/bin/bash
# ------------------------------------------------------------------
# JsPlus-LogReplaced test/_RunOne.sh - internal shared launcher for a single
# test suite. Same shape as JsPlus-Tomcat/test/_RunOne.sh.
# Usage: _RunOne.sh <TestName> <FacadeExit 0|1> [scriptEngine]
# Flow: pre-cleanup -> StopAll -> H2 init/start -> wait :PORT -> one suite
#       -> StopAll. Does NOT run LoadTest and does NOT archive reports.
# ------------------------------------------------------------------

TEST_NAME="${1:-}"
FACADE_EXIT="${2:-0}"
SCRIPT_ENGINE="${3:-graalvm.js}"

if [ -z "$TEST_NAME" ]; then
    echo "Usage: $0 <TestName> <FacadeExit 0|1> [scriptEngine]"
    exit 1
fi

BIN_PATH="$(cd "$(dirname "$0")" && pwd)/"
JS_PLUS_PATH_ORIGIN="${BIN_PATH}../JsPlus/"
export JS_PLUS_PATH="${JS_PLUS_PATH_ORIGIN//\\//}"
LIB_HOME="${JS_PLUS_PATH}../LibHome/"

DEPEND_LIBS="$(find "${LIB_HOME}" -type f -name '*.jar' | tr '\n' ':')"
DEPEND_LIBS="${DEPEND_LIBS%:}"
if [ -z "$DEPEND_LIBS" ]; then
    echo "Error: No JARs found under LibHome. Run 'ant build' first."
    exit 1
fi

AppName="$(basename "$(cd "${JS_PLUS_PATH}/.." && pwd)")"
JavaDirEnvName="JsPlus_JavaDir_${AppName}"
JAVA_DIR="$(printenv "$JavaDirEnvName" || true)"
JAVA_DIR="${JAVA_DIR:-default}"
JAVA_BIN="${JS_PLUS_PATH}../Java/${JAVA_DIR}/bin/java"

if [ ! -f "$JAVA_BIN" ]; then
    if [ -n "$JAVA_HOME" ]; then
        JAVA_BIN="$JAVA_HOME/bin/java"
    else
        JAVA_BIN="java"
    fi
fi

JvmParamsName="JsPlus_JvmParams_${AppName}"
JvmParams="$(printenv "$JvmParamsName" || true)"
if [ -z "$JvmParams" ]; then
    JvmParams="-Xms256m -Xmx1024m -Xss256k"
fi

export H2_PORT="${JsPlus_H2_PORT:-3308}"

# Step -1: pre-cleanup
echo "[$TEST_NAME] Cleaning JsPlus/.tmp and logs/* ..."
rm -rf "${JS_PLUS_PATH}.tmp" 2>/dev/null || true
rm -rf "${JS_PLUS_PATH}../logs"/* 2>/dev/null || true

# Step 0: stop existing services
echo "[$TEST_NAME] Stopping existing services..."
"${JS_PLUS_PATH}bin/StopAll.sh" || true

# Step 1: H2 init + start
if (netstat -tln 2>/dev/null | grep -q ":$H2_PORT .*LISTEN") || (ss -tln 2>/dev/null | grep -q ":$H2_PORT .*LISTEN"); then
    echo "[$TEST_NAME] H2 already running on :$H2_PORT - skipping DB init."
else
    echo "[$TEST_NAME] Initializing database logdb..."
    "${JS_PLUS_PATH}bin/StartH2Console.sh" init logdb
    echo "[$TEST_NAME] Starting H2 server (port $H2_PORT)..."
    "${JS_PLUS_PATH}bin/BG_StartH2.sh" "$SCRIPT_ENGINE"
fi

echo "[$TEST_NAME] Waiting for H2 :$H2_PORT..."
H2_READY=0
for i in $(seq 1 45); do
    if (netstat -tln 2>/dev/null | grep -q ":$H2_PORT .*LISTEN") || (ss -tln 2>/dev/null | grep -q ":$H2_PORT .*LISTEN"); then
        H2_READY=1
        break
    fi
    sleep 1
done
if [ "$H2_READY" != "1" ]; then
    echo "[$TEST_NAME] ERROR: H2 did not start on port $H2_PORT."
    exit 1
fi
echo "[$TEST_NAME] H2 ready."

# Step 2: run the single suite via _RunOne.js
export RUN_SUITE="$TEST_NAME"
if [ "$FACADE_EXIT" = "1" ]; then
    export JSPLUS_FACADE_EXIT=1
fi
echo "[$TEST_NAME] Running $TEST_NAME.js (via _RunOne.js) ..."
"$JAVA_BIN" $JvmParams -cp "$DEPEND_LIBS" \
    com.killcoding.jsplus.Main -ScriptEngine "$SCRIPT_ENGINE" -Env script -WriteHistory false \
    -Task "${JS_PLUS_PATH}../test/_RunOne.js" < /dev/null
RC=$?
echo "[$TEST_NAME] exit code: $RC"

# Step 3: cleanup
echo "[$TEST_NAME] Stopping all remaining services..."
"${JS_PLUS_PATH}bin/StopAll.sh" || true

exit "$RC"
