@echo off
title Kill Java Processes by Path
setlocal enabledelayedexpansion

REM ------------------------------------------------------------------
REM JsPlus-LogReplaced StopAll - kill EVERY java/javaw process whose
REM command line contains this project's runtime anchor (JsPlus/), i.e. every
REM JsPlus JVM started from this checkout (console launchers, H2 server, test
REM consoles) - same shape as JsPlus-Core/JsPlus/bin/StopAll.bat.
REM Usage: StopAll.bat
REM ------------------------------------------------------------------

REM Get the parent directory of the script location (application root)
set "ScriptDir=%~dp0"
set "ScriptDir=%ScriptDir:~0,-1%"
for %%i in ("%ScriptDir%\..") do set "AppRootDir=%%~fi"

REM Convert backslash to forward slash to match the path format in command line
set "Pattern=!AppRootDir:\=/!"

echo ==============================================
echo Application Root: !AppRootDir!
echo Pattern: !Pattern!
echo ==============================================
echo Searching and terminating Java processes...

REM Execute PowerShell command
REM NOTE: @($procs).Count instead of $procs.Count - a single match is a bare
REM object (not an array), so .Count renders empty on PowerShell 7. Wrapping in
REM @() always yields an array and prints the real count.
powershell -NoProfile -Command ^
    "$pattern='!Pattern!';" ^
    "$procs=@(Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | Where-Object { $_.CommandLine -like \"*$pattern*\" });" ^
    "if ($procs.Count -gt 0) { $procs | ForEach-Object { Write-Host (\"Terminating PID: $($_.ProcessId)\") -ForegroundColor Yellow; Stop-Process -Id $_.ProcessId -Force }; Write-Host (\"Terminated $($procs.Count) process(es).\") -ForegroundColor Green } else { Write-Host \"No matching Java processes found.\" -ForegroundColor Cyan }"

echo.
echo Operation completed.
