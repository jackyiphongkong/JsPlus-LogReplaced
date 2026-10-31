#!/bin/bash
# Single-suite entry for TestConsole - delegates to _RunOne.sh (facade=0).
exec "$(cd "$(dirname "$0")" && pwd)/_RunOne.sh" TestConsole 0 "${1:-}"
