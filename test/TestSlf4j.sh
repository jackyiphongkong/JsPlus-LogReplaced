#!/bin/bash
# Single-suite entry for TestSlf4j - delegates to _RunOne.sh (facade=1).
exec "$(cd "$(dirname "$0")" && pwd)/_RunOne.sh" TestSlf4j 1 "${1:-}"
