#!/bin/bash
set -e

# Kill the H2 server JVM (keyword StartH2 from the launcher name).
ScriptDir="$(cd "$(dirname "$0")" && pwd)"

exec "${ScriptDir}/Stop.sh" StartH2.sh
