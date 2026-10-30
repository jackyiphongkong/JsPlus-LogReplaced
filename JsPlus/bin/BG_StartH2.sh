#!/bin/bash
set -e

# ------------------------------------------------------------------
# JsPlus-LogReplaced BG_StartH2 - start the H2 TCP server in the
# background (nohup), delegating to bin/StartH2.sh.
# ------------------------------------------------------------------

BIN_PATH="$(cd "$(dirname "$0")" && pwd)/"
JS_PLUS_PATH_ORIGIN="${BIN_PATH}../"
export JS_PLUS_PATH="${JS_PLUS_PATH_ORIGIN//\\//}"

nohup "${JS_PLUS_PATH}bin/StartH2.sh" "$@" > /dev/null 2>&1 &
