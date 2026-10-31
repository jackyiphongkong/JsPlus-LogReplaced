@echo off
setlocal enabledelayedexpansion

REM ------------------------------------------------------------------
REM JsPlus-LogReplaced test/_RunOne.bat - internal shared launcher for a
REM single test suite. Same shape as JsPlus-Tomcat/test/_RunOne.bat.
REM Usage: _RunOne.bat <TestName> <FacadeExit 0|1> [scriptEngine]
REM   <TestName>     TestConsole, TestLog, TestSlf4j, TestCommonsLogging,
REM                  TestJuli, TestLog4j, TestLog4j2, TestAppLoggerConfig
REM   <FacadeExit>   1 = set JSPLUS_FACADE_EXIT so the vendor suite exits with
REM                  its own pass/fail code; 0 = engine-side suite (TestConsole
REM                  / TestLog / TestAppLoggerConfig) which has no self-exit and
REM                  gets its code from _RunOne.js counters
REM   [scriptEngine] default graalvm.js
REM Flow: pre-cleanup -> StopAll -> H2 init/start -> wait :PORT -> one suite
REM       -> StopAll. Does NOT run LoadTest and does NOT archive reports
REM       (that is TestAll.bat's job).
REM ------------------------------------------------------------------

if "%~1"=="" (
    echo Usage: %~nx0 ^<TestName^> ^<FacadeExit 0^|1^> [scriptEngine]
    exit /b 1
)
set "TEST_NAME=%~1"
set "FACADE_EXIT=%~2"
if "%FACADE_EXIT%"=="" set "FACADE_EXIT=0"

if "%~3"=="" (
    set "SCRIPT_ENGINE=graalvm.js"
) else (
    set "SCRIPT_ENGINE=%~3"
)

set BIN_PATH=%~dp0
set JS_PLUS_PATH_ORIGIN=%BIN_PATH%..\JsPlus\
SET "JS_PLUS_PATH=%JS_PLUS_PATH_ORIGIN:\=/%"
set LIB_HOME=%JS_PLUS_PATH%../LibHome/

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
    if defined JAVA_HOME (
        set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
    ) else (
        set "JAVA_BIN=java"
    )
)

SET "JvmParamsName=JsPlus_JvmParams_%AppName%"
call set "JvmParams=%%%JvmParamsName%%%"
if "%JvmParams%"=="" set "JvmParams=-Xms256m -Xmx1024m -Xss256k"

REM H2 TCP port - read from the environment, default 3308 (logdb), matching
REM config/DataSource.properties.
if not defined JsPlus_H2_PORT set "JsPlus_H2_PORT=3308"
set "H2_PORT=%JsPlus_H2_PORT%"

REM Step -1: pre-cleanup (same as TestAll.bat)
echo [%TEST_NAME%] Cleaning JsPlus\.tmp and logs\* ...
if exist "%JS_PLUS_PATH_ORIGIN%.tmp" rmdir /s /q "%JS_PLUS_PATH_ORIGIN%.tmp" >nul 2>&1
if exist "%JS_PLUS_PATH_ORIGIN%..\logs" (
    del /f /q "%JS_PLUS_PATH_ORIGIN%..\logs\*" >nul 2>&1
    for /d %%d in ("%JS_PLUS_PATH_ORIGIN%..\logs\*") do rmdir /s /q "%%d"
)

REM Step 0: stop existing services so the DB file is unlocked
echo [%TEST_NAME%] Stopping existing services...
call "%JS_PLUS_PATH%bin\StopAll.bat"

REM Step 1: H2 init + start (skipped when the port is already listening)
netstat -ano | findstr ":%H2_PORT%" | findstr "LISTENING" >nul 2>&1
if not errorlevel 1 goto :h2_ready

echo [%TEST_NAME%] Initializing database logdb...
call "%JS_PLUS_PATH%bin\StartH2Console.bat" init logdb
if errorlevel 1 (
    echo [%TEST_NAME%] ERROR: Database initialization failed.
    exit /b 1
)

REM Start + wait, with ONE in-place retry. A just-killed H2 can still hold the
REM port (TIME_WAIT) so the first start never binds - the known flakiness of
REM this project's H2 bootstrap. The server itself is started through
REM bin\BG_StartH2.bat, the same entry every other launcher uses.
set "H2_READY="
for /l %%a in (1,1,2) do (
    if not defined H2_READY (
        echo [%TEST_NAME%] Starting H2 server on port %H2_PORT%, attempt %%a ...
        call "%JS_PLUS_PATH%bin\BG_StartH2.bat" %SCRIPT_ENGINE%
        for /l %%i in (1,1,30) do (
            netstat -ano | findstr ":%H2_PORT%" | findstr "LISTENING" >nul 2>&1
            if not errorlevel 1 set "H2_READY=1"
            if defined H2_READY goto :h2_ready
            ping -n 2 127.0.0.1 >nul 2>&1
        )
        echo [%TEST_NAME%] WARN: H2 not listening after 30s, retrying...
        call "%JS_PLUS_PATH%bin\StopAll.bat"
    )
)

:h2_ready
if not defined H2_READY (
    echo [%TEST_NAME%] ERROR: H2 did not start on port %H2_PORT% after 2 attempts.
    exit /b 1
)
echo [%TEST_NAME%] H2 ready.

REM Step 2: run the single suite via _RunOne.js
set "RUN_SUITE=%TEST_NAME%"
if "%FACADE_EXIT%"=="1" set "JSPLUS_FACADE_EXIT=1"
echo [%TEST_NAME%] Running %TEST_NAME%.js (via _RunOne.js) ...
%JAVA_BIN% %JvmParams% -cp "%DEPEND_LIBS%" com.killcoding.jsplus.Main -ScriptEngine "%SCRIPT_ENGINE%" -Env script -WriteHistory false -Task "%JS_PLUS_PATH%../test/_RunOne.js" < NUL
set "RC=%ERRORLEVEL%"
echo [%TEST_NAME%] exit code: %RC%

REM Step 3: cleanup services
echo [%TEST_NAME%] Stopping all remaining services...
call "%JS_PLUS_PATH%bin\StopAll.bat"

exit /b %RC%

endlocal
