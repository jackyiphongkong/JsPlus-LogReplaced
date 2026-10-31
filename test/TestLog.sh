#!/bin/bash
# Single-suite entry for TestLog - delegates to _RunOne.sh (facade=0).
exec "$(cd "$(dirname "$0")" && pwd)/_RunOne.sh" TestLog 0 "${1:-}"
