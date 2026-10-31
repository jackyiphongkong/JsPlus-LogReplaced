#!/bin/bash
# Single-suite entry for TestCommonsLogging - delegates to _RunOne.sh (facade=1).
exec "$(cd "$(dirname "$0")" && pwd)/_RunOne.sh" TestCommonsLogging 1 "${1:-}"
