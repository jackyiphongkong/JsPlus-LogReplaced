#!/bin/bash
set -e

# ------------------------------------------------------------------
# JsPlus-LogReplaced Stop - kill java processes whose command line
# contains the keyword derived from the startup script name.
# Usage : Stop.sh <StartXxx.sh>   e.g. Stop.sh StartLogRp.sh
# ------------------------------------------------------------------

ScriptDir="$(cd "$(dirname "$0")" && pwd)"

if [ -z "$1" ]; then
    echo "Usage: $0 <StartXxx.sh>"
    echo "  e.g. $0 StartLogRp.sh"
    exit 1
fi

Arg="$1"
case "$Arg" in
    /*) TargetSh="$Arg" ;;
    *)  TargetSh="${ScriptDir}/${Arg}" ;;
esac
if [ -d "$(dirname "$TargetSh")" ]; then
    TargetSh="$(cd "$(dirname "$TargetSh")" && pwd)/$(basename "$TargetSh")"
fi

# The keyword is the startup script base name: StartLogRp.sh -> StartLogRp
Base="$(basename "$1")"
Base="${Base%.*}"

if [ -z "$Base" ]; then
    echo "[ERROR] Cannot derive keyword from \"$1\"."
    exit 1
fi

echo "=============================================="
echo "TargetSh : $TargetSh"
echo "Keyword  : $Base"
echo "=============================================="
echo "Searching and terminating Java processes..."

pids=$(ps -eo pid,args | grep -E 'java|javaw' | grep -F "$Base" | grep -v grep | awk '{print $1}')

if [ -n "$pids" ]; then
    echo "$pids" | while read pid; do
        echo "Terminating PID: $pid"
        kill -9 "$pid" 2>/dev/null || true
    done
    count=$(echo "$pids" | wc -l)
    echo "Terminated $count process(es)."
else
    echo "No matching Java processes found."
fi

echo
