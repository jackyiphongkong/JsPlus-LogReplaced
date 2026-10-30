@echo off
setlocal

REM ------------------------------------------------------------------
REM JsPlus-LogReplaced BG_StartH2 - start the H2 TCP server in the
REM background (hidden cmd window), delegating to bin\StartH2.bat.
REM ------------------------------------------------------------------

if "%1"=="" (
    set SCRIPT_ENGINE=graalvm.js
) else (
    set SCRIPT_ENGINE=%1
)

set BIN_PATH=%~dp0
set JS_PLUS_PATH_ORIGIN=%BIN_PATH%..\
SET "JS_PLUS_PATH=%JS_PLUS_PATH_ORIGIN:\=/%"

powershell -Command Start-Process -FilePath 'cmd.exe' -ArgumentList '/c %JS_PLUS_PATH%/bin/StartH2.bat %SCRIPT_ENGINE%' -WindowStyle Hidden
