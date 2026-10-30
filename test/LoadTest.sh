#!/bin/bash
set -e

# ------------------------------------------------------------------
# JsPlus-LogReplaced load suite - LoadTest.sh
# Runs test/LoadTest.js in its own JsPlus console (single-threaded load of
# ~16.5k lines against com.killcoding.log: burst throughput, zero line loss,
# 8 KB payloads, throwable load, multi-logger interleave, level gate).
# The suite exits 1 when any load case fails, so this entry is CI-usable.
# Report : <PROJECT_ROOT>/docs/latest/report/LoadTest-Report.html / .md
# Inside TestAll.sh the same suite runs last without exiting (JSPLUS_LOADTEST_EXIT
# is only set here).
# Override via env (same convention as TestAll.sh):
#   JsPlus_JavaDir_JsPlus-LogReplaced / JsPlus_JvmParams_JsPlus-LogReplaced / SCRIPT_ENGINE (arg $1)
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
    echo "WARN: Bundled Java not found at $JAVA_BIN, falling back to system Java..."
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

export JSPLUS_LOADTEST_EXIT=1
set +e
"$JAVA_BIN" $JvmParams -cp "$DEPEND_LIBS" com.killcoding.jsplus.Main -ScriptEngine "$SCRIPT_ENGINE" -Env script -WriteHistory false -Task "${JS_PLUS_PATH}../test/LoadTest.js" < /dev/null
RC=$?
set -e

echo ""
echo "LoadTest exit code: ${RC}"
exit "$RC"
