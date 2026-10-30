@echo off
setlocal enabledelayedexpansion
REM ------------------------------------------------------------------
REM JsPlus-LogReplaced facade suites - TestFacades.bat
REM Runs one suite per logging vendor, each in its own JsPlus console:
REM   TestSlf4j.js           org.slf4j
REM   TestCommonsLogging.js  org.apache.commons.logging
REM   TestJuli.js            org.apache.juli.logging
REM   TestLog4j.js           org.apache.log4j (1.2)
REM   TestLog4j2.js          org.apache.logging.log4j (2.20)
REM Each suite exits 1 when any of its cases fail, so the aggregate exit
REM code below is CI-usable. Reports land in <PROJECT_ROOT>/docs/latest/report/.
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
    echo Error: No JARs found under LibHome. Run 'ant package' first.
    exit /b 1
)

FOR %%I IN ("%JS_PLUS_PATH%..") DO SET "AppName=%%~nxI"
SET "JavaDirEnvName=JsPlus_JavaDir_%AppName%"
call set "JAVA_DIR=%%%JavaDirEnvName%%%"
if not defined JAVA_DIR set "JAVA_DIR=default"
set "JAVA_BIN=%JS_PLUS_PATH%../Java/%JAVA_DIR%/bin/java.exe"

SET "JvmParamsName=JsPlus_JvmParams_%AppName%"
call set "JvmParams=%%%JvmParamsName%%%"
if "%JvmParams%"=="" SET "JvmParams=-Xms256m -Xmx1024m -Xss256k"

set JSPLUS_FACADE_EXIT=1
set FAILED=0

for %%s in (TestSlf4j TestCommonsLogging TestJuli TestLog4j TestLog4j2) do (
    echo.
    echo ===== Running %%s =====
    "%JAVA_BIN%" %JvmParams% -cp "%DEPEND_LIBS%" com.killcoding.jsplus.Main -ScriptEngine "%SCRIPT_ENGINE%" -Env script -WriteHistory false -Task "%JS_PLUS_PATH%../test/%%s.js" < NUL
    if errorlevel 1 (
        echo %%s FAILED
        set FAILED=1
    ) else (
        echo %%s PASSED
    )
)

echo.
echo TestFacades exit code: !FAILED!
exit /b !FAILED!