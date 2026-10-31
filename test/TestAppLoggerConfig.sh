#!/bin/bash
# Single-suite entry for TestAppLoggerConfig - delegates to _RunOne.sh (facade=0).
exec "$(cd "$(dirname "$0")" && pwd)/_RunOne.sh" TestAppLoggerConfig 0 "${1:-}"
