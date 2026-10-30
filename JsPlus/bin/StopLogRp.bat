@echo off
title Kill Java Processes for JsPlus-LogReplaced StartLogRp
setlocal

REM ------------------------------------------------------------------
REM Stop the JsPlus-LogReplaced console started by bin\StartLogRp.bat.
REM StartLogRp runs "com.killcoding.jsplus.Main ... -Task script/main.js";
REM this script kills java.exe / javaw.exe whose command line matches BOTH
REM that task and this project folder, so consoles of other projects
REM (JsPlus-Core, JsPlus-Tomcat, ...) are left alone.
REM ------------------------------------------------------------------

echo Searching Java processes for JsPlus-LogReplaced StartLogRp...

powershell -NoProfile -Command ^
    "$kw='script/main.js';" ^
    "$root='JsPlus-LogReplaced';" ^
    "$procs=@(Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | Where-Object { $_.CommandLine -like \"*$kw*\" -and $_.CommandLine -like \"*$root*\" });" ^
    "if ($procs) {" ^
    "  $procs | ForEach-Object { Write-Host ('Terminating PID: {0}' -f $_.ProcessId) -ForegroundColor Yellow; Stop-Process -Id $_.ProcessId -Force };" ^
    "  Write-Host ('Terminated {0} process(es).' -f $procs.Count) -ForegroundColor Green" ^
    "} else {" ^
    "  Write-Host 'No matching Java processes found.' -ForegroundColor Cyan" ^
    "}"

echo.
endlocal
