#!/bin/bash
set -e

# ------------------------------------------------------------------
# JsPlus-LogReplaced facade suites - TestFacades.sh
# Runs one suite per logging vendor, each in its own JsPlus console:
#   TestSlf4j.js           org.slf4j
#   TestCommonsLogging.js  org.apache.commons.logging
#   TestJuli.js            org.apache.juli.logging
#   TestLog4j.js           org.apache.log4j (1.2)
#   TestLog4j2.js          org.apache.logging.log4j (2.20)
# Each suite exits 1 when any of its cases fail, so the aggregate exit
# code below is CI-usable. Reports land in <PROJECT_ROOT>/docs/latest/report/.
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
    echo "Error: No JARs found under LibHome. Run 'ant package' first."
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

export JSPLUS_FACADE_EXIT=1
FAILED=0

for suite in TestSlf4j TestCommonsLogging TestJuli TestLog4j TestLog4j2; do
    echo ""
    echo "===== Running ${suite} ====="
    set +e
    "$JAVA_BIN" $JvmParams -cp "$DEPEND_LIBS" com.killcoding.jsplus.Main -ScriptEngine "$SCRIPT_ENGINE" -Env script -WriteHistory false -Task "${JS_PLUS_PATH}../test/${suite}.js" < /dev/null
    RC=$?
    set -e
    if [ "$RC" -ne 0 ]; then
        echo "${suite} FAILED (exit ${RC})"
        FAILED=1
    else
        echo "${suite} PASSED"
    fi
done

echo ""
echo "TestFacades exit code: ${FAILED}"
exit "$FAILED"
