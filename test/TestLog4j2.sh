#!/bin/bash
# Single-suite entry for TestLog4j2 - delegates to _RunOne.sh (facade=1).
exec "$(cd "$(dirname "$0")" && pwd)/_RunOne.sh" TestLog4j2 1 "${1:-}"
