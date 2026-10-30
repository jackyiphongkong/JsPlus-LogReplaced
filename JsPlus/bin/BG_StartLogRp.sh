#!/bin/bash
set -e

# ------------------------------------------------------------------
# JsPlus-LogReplaced BG_StartLogRp - start the JsPlus console in the
# background (nohup), delegating to bin/StartLogRp.sh.
# ------------------------------------------------------------------

BIN_PATH="$(cd "$(dirname "$0")" && pwd)/"
JS_PLUS_PATH_ORIGIN="${BIN_PATH}../"
export JS_PLUS_PATH="${JS_PLUS_PATH_ORIGIN//\\//}"

nohup "${JS_PLUS_PATH}bin/StartLogRp.sh" "$@" > /dev/null 2>&1 &
