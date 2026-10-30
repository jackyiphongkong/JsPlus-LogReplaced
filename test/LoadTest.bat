@echo off
setlocal enabledelayedexpansion
REM ------------------------------------------------------------------
REM JsPlus-LogReplaced load suite - LoadTest.bat
REM Runs test/LoadTest.js in its own JsPlus console (single-threaded load of
REM ~16.5k lines against com.killcoding.log: burst throughput, zero line loss,
REM 8 KB payloads, throwable load, multi-logger interleave, level gate).
REM The suite exits 1 when any load case fails, so this entry is CI-usable.
REM Report : <PROJECT_ROOT>/docs/latest/report/LoadTest-Report.html / .md
REM Inside TestAll.bat the same suite runs last without exiting (JSPLUS_LOADTEST_EXIT
REM is only set here).
REM Override via env (same convention as TestAll.bat):
REM   JsPlus_JavaDir_JsPlus-LogReplaced / JsPlus_JvmParams_JsPlus-LogReplaced / SCRIPT_ENGINE (arg %1)
REM ------------------------------------------------------------------

if "%1"=="" (
    set SCRIPT_ENGINE=graalvm.js
) else (
    set SCRIPT_ENGINE=%1
)

set BIN_PATH=%~dp0
set JS_PLUS_PATH_ORIGIN=%BIN_PATH%..\JsPlus\
SET "JS_PLUS_PATH=%JS_PLUS_PATH_ORIGIN:\=/%"

set "DEPEND_LIBS="
for /r "%JS_PLUS_PATH_ORIGIN%..\LibHome" %%j in (*.jar) do set "DEPEND_LIBS=!DEPEND_LIBS!;%%j"
set "DEPEND_LIBS=!DEPEND_LIBS:~1!"
if not defined DEPEND_LIBS (
    echo Error: No JARs found under LibHome. Run 'ant build' first.
    exit /b 1
)

FOR %%I IN ("%JS_PLUS_PATH%..") DO SET "AppName=%%~nxI"
SET "JavaDirEnvName=JsPlus_JavaDir_%AppName%"
call set "JAVA_DIR=%%%JavaDirEnvName%%%"
if not defined JAVA_DIR set "JAVA_DIR=default"
set "JAVA_BIN=%JS_PLUS_PATH%../Java/%JAVA_DIR%/bin/java.exe"

if not exist "%JAVA_BIN%" (
    echo WARN: Bundled Java not found at %JAVA_BIN%, falling back to system Java...
    if defined JAVA_HOME (
        set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
    ) else (
        set "JAVA_BIN=java"
    )
)

SET "JvmParamsName=JsPlus_JvmParams_%AppName%"
call set "JvmParams=%%%JvmParamsName%%%"
if "%JvmParams%"=="" SET "JvmParams=-Xms256m -Xmx1024m -Xss256k"

set JSPLUS_LOADTEST_EXIT=1
"%JAVA_BIN%" %JvmParams% -cp "%DEPEND_LIBS%" com.killcoding.jsplus.Main -ScriptEngine "%SCRIPT_ENGINE%" -Env script -WriteHistory false -Task "%JS_PLUS_PATH%../test/LoadTest.js" < NUL
set "RC=%ERRORLEVEL%"

echo.
echo LoadTest exit code: %RC%
exit /b %RC%

endlocal
