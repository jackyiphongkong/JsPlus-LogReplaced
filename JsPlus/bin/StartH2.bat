@echo off
setlocal enabledelayedexpansion
REM ------------------------------------------------------------------
REM JsPlus-LogReplaced StartH2 - start the H2 TCP server (foreground).
REM Config : JsPlus\config\Appenv.properties (H2_PORT / H2_BASE_DIR)
REM          JsPlus\config\DataSource.properties (jdbc:h2:tcp://localhost:<port>/logdb)
REM Stop   : JsPlus\bin\StopH2.bat (Stop.bat StartH2.bat)
REM The JVM stays alive after the task returns because -GhostName H2Ghost
REM starts a ghost console (that ghost is the handle StopH2 looks for).
REM Override via env (same convention as TestAll.bat):
REM   JsPlus_JavaDir_JsPlus-LogReplaced / JsPlus_JvmParams_JsPlus-LogReplaced / SCRIPT_ENGINE (arg %1)
REM ------------------------------------------------------------------

if "%1"=="" (
    set SCRIPT_ENGINE=graalvm.js
) else (
    set SCRIPT_ENGINE=%1
)

set BIN_PATH=%~dp0
set JS_PLUS_PATH_ORIGIN=%BIN_PATH%..\
SET "JS_PLUS_PATH=%JS_PLUS_PATH_ORIGIN:\=/%"

set "DEPEND_LIBS="
for /r "%JS_PLUS_PATH_ORIGIN%..\LibHome" %%j in (*.jar) do set "DEPEND_LIBS=!DEPEND_LIBS!;%%j"
set "DEPEND_LIBS=!DEPEND_LIBS:~1!"
if not defined DEPEND_LIBS (
    echo Error: No JARs found under LibHome
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

if "%JvmParams%"=="" (
    SET "JvmParams=-Xms128m -Xmx1024m -Xss256k"
    echo WARN: Missing %JvmParamsName% then using default params
)

REM H2_BASE_DIR points at <PROJECT_ROOT>/Database - create it up front so
REM -baseDir never fails on a fresh checkout.
if not exist "%JS_PLUS_PATH_ORIGIN%..\Database" mkdir "%JS_PLUS_PATH_ORIGIN%..\Database"

echo Using "%JvmParams%"
%JAVA_BIN% %JvmParams% -cp "%DEPEND_LIBS%" com.killcoding.jsplus.Main -ScriptEngine "%SCRIPT_ENGINE%" -Env script -WriteHistory false -GhostName H2Ghost -Task "%JS_PLUS_PATH%script/StartH2.js"

endlocal
