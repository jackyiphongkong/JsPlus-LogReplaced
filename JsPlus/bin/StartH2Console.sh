#!/bin/bash
set -e

# ------------------------------------------------------------------
# JsPlus-LogReplaced H2 database console / initializer
# Usage:
#   StartH2Console.sh                 Interactive shell (default DB: logdb)
#   StartH2Console.sh <DB_NAME>       Interactive shell on the given DB
#   StartH2Console.sh init [DB_NAME]  Reset + rebuild schema:
#                                     fallback.sql -> change.sql
#                                     (default DB: logdb, matching
#                                      config/DataSource.properties)
# Same shape as JsPlus-Core/JsPlus/bin/StartH2Console.sh with two
# project-specific differences:
#   * default DB is logdb (Core defaults to appdb, the Tomcat app DB)
#   * the DB user is sample_user / sample_password, NOT sa - this project's
#     Database/logdb is created by sample_user on the first connection, so
#     "sa" does not exist ("Wrong user name or password [28000-240]").
# NOTE: "init" opens the database file directly, so stop the server first
#       (JsPlus/bin/StopH2.sh) - a running H2 holds the file lock.
# ------------------------------------------------------------------

INIT_MODE=0
if [ "$1" = "init" ]; then
    INIT_MODE=1
    if [ -z "$2" ]; then
        DB_NAME="logdb"
    else
        DB_NAME="$2"
    fi
elif [ -z "$1" ]; then
    DB_NAME="logdb"
else
    DB_NAME="$1"
fi

BIN_PATH="$(cd "$(dirname "$0")" && pwd)/"
JS_PLUS_PATH_ORIGIN="${BIN_PATH}../"
JS_PLUS_PATH="${JS_PLUS_PATH_ORIGIN//\\//}"
LIB_HOME="${JS_PLUS_PATH}../LibHome/"

DEPEND_LIBS="$(find "${LIB_HOME}" -type f -name '*.jar' | tr '\n' ':')"
DEPEND_LIBS="${DEPEND_LIBS%:}"
if [ -z "$DEPEND_LIBS" ]; then
    echo "Error: No JARs found under LibHome"
    exit 1
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
    JvmParams="-Xms128m -Xmx128m -Xss256k"
    echo "WARN: Missing ${JvmParamsName} then using default params"
else
    echo "Using System Env JVM Params"
fi

echo "Using \"$JvmParams\""

DB_USER="sample_user"
DB_PASSWORD="sample_password"
DB_URL="jdbc:h2:${JS_PLUS_PATH}../Database/${DB_NAME}"

echo "[StartH2Console] DB=${DB_NAME} url=${DB_URL} user=${DB_USER}"

if [ "$INIT_MODE" = "1" ]; then
    echo "[StartH2Console] Initializing database \"$DB_NAME\"..."
    echo "[StartH2Console] NOTE: the H2 server must be stopped (StopH2.sh) - init opens the DB file directly."
    echo "[StartH2Console] Executing dbscript/fallback.sql ..."
    "$JAVA_BIN" $JvmParams -cp "$DEPEND_LIBS" org.h2.tools.RunScript \
        -url "$DB_URL" -user "$DB_USER" -password "$DB_PASSWORD" \
        -script "${JS_PLUS_PATH}../dbscript/fallback.sql" -checkResults
    echo "[StartH2Console] Executing dbscript/change.sql ..."
    "$JAVA_BIN" $JvmParams -cp "$DEPEND_LIBS" org.h2.tools.RunScript \
        -url "$DB_URL" -user "$DB_USER" -password "$DB_PASSWORD" \
        -script "${JS_PLUS_PATH}../dbscript/change.sql" -checkResults
    echo "[StartH2Console] Database \"$DB_NAME\" initialized: sample_user + app_logger_config + app_logger_msg."
else
    "$JAVA_BIN" $JvmParams -cp "$DEPEND_LIBS" org.h2.tools.Shell \
        -url "$DB_URL" -user "$DB_USER" -password "$DB_PASSWORD"
fi
