#!/bin/bash
# Single-suite entry for TestJuli - delegates to _RunOne.sh (facade=1).
exec "$(cd "$(dirname "$0")" && pwd)/_RunOne.sh" TestJuli 1 "${1:-}"
