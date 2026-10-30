#!/bin/bash
set -e

# ------------------------------------------------------------------
# JsPlus-LogReplaced StartH2 - start the H2 TCP server (foreground).
# Config : JsPlus/config/Appenv.properties (H2_PORT / H2_BASE_DIR)
#          JsPlus/config/DataSource.properties (jdbc:h2:tcp://localhost:<port>/logdb)
# Stop   : JsPlus/bin/StopH2.sh (Stop.sh StartH2.sh)
# The JVM stays alive after the task returns because -GhostName H2Ghost
# starts a ghost console (that ghost is the handle StopH2 looks for).
# Override via env (same convention as TestAll.sh):
#   JsPlus_JavaDir_JsPlus-LogReplaced / JsPlus_JvmParams_JsPlus-LogReplaced / SCRIPT_ENGINE (arg $1)
# ------------------------------------------------------------------

if [ -z "$1" ]; then
    SCRIPT_ENGINE=graalvm.js
else
    SCRIPT_ENGINE="$1"
fi

BIN_PATH="$(cd "$(dirname "$0")" && pwd)/"
JS_PLUS_PATH_ORIGIN="${BIN_PATH}../"
export JS_PLUS_PATH="${JS_PLUS_PATH_ORIGIN//\\//}"
LIB_HOME="${JS_PLUS_PATH}../LibHome/"

DEPEND_LIBS="$(find "${LIB_HOME}" -type f -name '*.jar' | tr '\n' ':')"
DEPEND_LIBS="${DEPEND_LIBS%:}"
if [ -z "$DEPEND_LIBS" ]; then
    echo "Error: No JARs found under LibHome"
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
    JvmParams="-Xms128m -Xmx1024m -Xss256k"
fi

# H2_BASE_DIR points at <PROJECT_ROOT>/Database - create it up front so
# -baseDir never fails on a fresh checkout.
mkdir -p "${JS_PLUS_PATH_ORIGIN}../Database"

"$JAVA_BIN" $JvmParams -cp "$DEPEND_LIBS" com.killcoding.jsplus.Main -ScriptEngine "$SCRIPT_ENGINE" -Env script -WriteHistory false -GhostName H2Ghost -Task "${JS_PLUS_PATH}script/StartH2.js"
