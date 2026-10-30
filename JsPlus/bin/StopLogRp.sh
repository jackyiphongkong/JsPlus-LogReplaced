#!/bin/bash
set -e

# ------------------------------------------------------------------
# Stop the JsPlus-LogReplaced console started by bin/StartLogRp.sh.
# Matches java processes whose command line contains BOTH
# "script/main.js" (the StartLogRp task) and this project folder, so
# consoles of other projects are left alone.
# ------------------------------------------------------------------

KW="script/main.js"
ROOT="JsPlus-LogReplaced"

echo "Searching Java processes for $ROOT StartLogRp..."

pids=$(ps -eo pid,args | grep -F "$KW" | grep -F "$ROOT" | grep -v grep | awk '{print $1}')

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
