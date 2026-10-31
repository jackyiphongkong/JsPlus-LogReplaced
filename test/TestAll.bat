@echo off
setlocal enabledelayedexpansion

REM ------------------------------------------------------------------
REM JsPlus-LogReplaced automated test suite - TestAll.bat
REM Flow:
REM   1) Build the runtime classpath from every JAR under
REM      <PROJECT_ROOT>/LibHome - script engines + deps, including the
REM      project JAR produced by 'ant build':
REM      <PROJECT_ROOT>/LibHome/jsplus/default/jsplus-logreplaced-<version>.jar
REM      (the org.* logging-replacement layer; com.killcoding.* comes from the jsplus-core-<version>.jar
REM      in the same LibHome/jsplus/default folder; version auto-bumped from <PROJECT_ROOT>/version.properties).
REM   2) Run TestAll.js in a standalone JsPlus console in THIS JVM
REM      (com.killcoding.jsplus.Main, same entry the JsPlus-Tomcat suite uses).
REM      No external server, H2 or Tomcat is required.
REM      JS_PLUS_PATH=<PROJECT_ROOT>/JsPlus/ (runtime anchor), so the suites
REM      resolve <PROJECT_ROOT>/test/TestBase.js + suites and write reports to
REM      <PROJECT_ROOT>/docs/latest/report/ (logs land in <PROJECT_ROOT>/logs/app/).
REM      Stdin is redirected to NUL so com.killcoding.jsplus.Main exits
REM      right after the -Task completes (otherwise it blocks on the
REM      interactive scanner, see Main.java scanner loop).
REM   3) Exit with the JVM's exit code.
REM Override via env (same convention as JsPlus-Tomcat):
REM      JsPlus_JavaDir_JsPlus-LogReplaced   - JDK directory under Java\ (default: default)
REM      JsPlus_JvmParams_JsPlus-LogReplaced - JVM params (default: -Xms256m -Xmx1024m -Xss256k)
REM      SCRIPT_ENGINE (arg %1)       - default: graalvm.js
REM ------------------------------------------------------------------

if "%1"=="" (
    set SCRIPT_ENGINE=graalvm.js
) else (
    set SCRIPT_ENGINE=%1
)

set BIN_PATH=%~dp0
set JS_PLUS_PATH_ORIGIN=%BIN_PATH%..\JsPlus\
SET "JS_PLUS_PATH=%JS_PLUS_PATH_ORIGIN:\=/%"
set LIB_HOME=%JS_PLUS_PATH%../LibHome/

REM ------------------------------------------------------------------
REM Step 0: clean the run-scoped scratch + log output so every run starts
REM from a clean tree. JsPlus\.tmp holds the evalJavaSource/evalJavaFile
REM scratch (compiled temp classes + MD5 source dirs), so stale MD5 dirs
REM cannot leak between runs (same as JsPlus-Core Step 0b). logs\* is the
REM engine's output tree (logs\app\<yyyyMMdd>.log); a clean tree also keeps
REM the suites honest - they assert on freshly written lines, so lines
REM left by a previous run could otherwise satisfy a check by accident.
REM ------------------------------------------------------------------
if exist "%JS_PLUS_PATH_ORIGIN%.tmp" (
    rmdir /s /q "%JS_PLUS_PATH_ORIGIN%.tmp"
    echo [TestAll] Removed JsPlus\.tmp
)
if exist "%JS_PLUS_PATH_ORIGIN%..\logs" (
    del /f /q "%JS_PLUS_PATH_ORIGIN%..\logs\*" >nul 2>&1
    for /d %%d in ("%JS_PLUS_PATH_ORIGIN%..\logs\*") do rmdir /s /q "%%d"
    echo [TestAll] Cleared logs\*
)

set "DEPEND_LIBS="
for /r "%JS_PLUS_PATH_ORIGIN%..\LibHome" %%j in (*.jar) do set "DEPEND_LIBS=!DEPEND_LIBS!;%%j"
set "DEPEND_LIBS=!DEPEND_LIBS:~1!"
if not defined DEPEND_LIBS (
    echo Error: No JARs found under LibHome. Run 'ant build' first ^(produces LibHome/jsplus/default/jsplus-logreplaced-^<version^>.jar^).
    exit /b 1
)
set "CORE_JAR="
for %%j in ("%JS_PLUS_PATH_ORIGIN%..\LibHome\jsplus\default\jsplus-logreplaced-*.jar") do set "CORE_JAR=%%j"
if not defined CORE_JAR (
    echo WARN: LibHome/jsplus/default/jsplus-logreplaced-*.jar not found. Run 'ant build' first.
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

echo "%JvmParamsName%"

if "%JvmParams%"=="" (
    SET "JvmParams=-Xms256m -Xmx1024m -Xss256k"
    echo WARN: Missing %JvmParamsName% then using default params
) else (
    echo Using System Env JVM Params
)

echo Using "%JvmParams%"

REM ------------------------------------------------------------------
REM Step 0b: stop any existing services, then make sure H2 is listening
REM before the suites run. The suites' dbVerify assertions read
REM app_logger_msg through the TCP pool (logdb on :H2_PORT), so a dead or
REM half-dead H2 fails the persistence checks. Same shape as
REM JsPlus-Tomcat/TestAll.bat Step 0/1: StopAll -> H2 init -> BG_StartH2.
REM ------------------------------------------------------------------
if not defined JsPlus_H2_PORT set "JsPlus_H2_PORT=3308"
set "H2_PORT=%JsPlus_H2_PORT%"
echo [TestAll] H2 port: %H2_PORT%

echo [TestAll] Stopping existing services...
call "%JS_PLUS_PATH%bin\StopAll.bat"

echo [TestAll] Initializing database logdb...
call "%JS_PLUS_PATH%bin\StartH2Console.bat" init logdb
if errorlevel 1 (
    echo [TestAll] ERROR: Database initialization failed.
    exit /b 1
)

REM Start + wait with ONE in-place retry: a just-killed H2 can still hold the
REM port (TIME_WAIT) so the first start never binds - the known flakiness of
REM this project's H2 bootstrap.
set "H2_READY="
for /l %%a in (1,1,2) do (
    if not defined H2_READY (
        echo [TestAll] Starting H2 server on port %H2_PORT%, attempt %%a ...
        call "%JS_PLUS_PATH%bin\BG_StartH2.bat" %SCRIPT_ENGINE%
        for /l %%i in (1,1,30) do (
            netstat -ano | findstr ":%H2_PORT%" | findstr "LISTENING" >nul 2>&1
            if not errorlevel 1 set "H2_READY=1"
            if defined H2_READY goto :h2_ready
            ping -n 2 127.0.0.1 >nul 2>&1
        )
        echo [TestAll] WARN: H2 not listening after 30s, retrying...
        call "%JS_PLUS_PATH%bin\StopAll.bat"
    )
)
:h2_ready
if not defined H2_READY (
    echo [TestAll] ERROR: H2 did not start on port %H2_PORT% after 2 attempts.
    exit /b 1
)
echo [TestAll] H2 ready.

REM ------------------------------------------------------------------
REM Run TestAll.js against this JVM.
REM JSPLUS_TEST_EXIT=1 tells TestAll.js to System.exit(0) after finishing
REM (interactive Console.eval runs skip it). "< NUL" closes stdin: the JsPlus
REM Main scanner gets EOF right after the -Task finishes and the JVM exits
REM cleanly (Main.java).
REM ------------------------------------------------------------------
set JSPLUS_TEST_EXIT=1
%JAVA_BIN% %JvmParams% -cp "%DEPEND_LIBS%" com.killcoding.jsplus.Main -ScriptEngine "%SCRIPT_ENGINE%" -Env script -WriteHistory false -Task "%JS_PLUS_PATH%../test/TestAll.js" < NUL
set "RC=%ERRORLEVEL%"
echo TestAll exit code: %RC%

REM ------------------------------------------------------------------
REM Step 2b: Archive the test reports under the same docs layout as the
REM javadoc site: docs/reports/<version>.jar (one jar per run) plus
REM docs/latest/report/ as the browsable newest reports. Version is taken
REM from the project jar name (jsplus-logreplaced-<version>.jar). A failure
REM to archive is only a WARN and never changes the suite exit code
REM (RC was captured above). The report archive is owned by TestAll (ant
REM only prunes its own docs/apis tree), so the newest-10 bound lives here.
REM ------------------------------------------------------------------
set "PROJ_FNAME="
for %%f in ("%CORE_JAR%") do set "PROJ_FNAME=%%~nxf"
if not defined PROJ_FNAME (
    echo WARN: Cannot locate LibHome/jsplus/default/jsplus-logreplaced-*.jar, skipping report archive.
) else (
    set "PROJ_VERSION=%PROJ_FNAME:jsplus-logreplaced-=%"
    set "PROJ_VERSION=!PROJ_VERSION:.jar=!"
    echo [TestAll] Archiving test reports as docs/reports/!PROJ_VERSION!.jar ^(docs/latest/report^)...
    if not exist "%JS_PLUS_PATH_ORIGIN%..\docs\reports" mkdir "%JS_PLUS_PATH_ORIGIN%..\docs\reports"
    if exist "%JS_PLUS_PATH_ORIGIN%..\docs\reports\!PROJ_VERSION!.jar" del "%JS_PLUS_PATH_ORIGIN%..\docs\reports\!PROJ_VERSION!.jar"
    "%JS_PLUS_PATH%../Java/%JAVA_DIR%/bin/jar.exe" cf "%JS_PLUS_PATH_ORIGIN%..\docs\reports\!PROJ_VERSION!.jar" -C "%JS_PLUS_PATH_ORIGIN%..\docs\latest\report" .
    if errorlevel 1 (
        echo WARN: Failed to pack test report jar ^(jar.exe missing?^), continuing.
    ) else (
        echo [TestAll] Packed docs/reports/!PROJ_VERSION!.jar
    )
    powershell -NoProfile -Command "$d='%JS_PLUS_PATH_ORIGIN%..\docs\reports'; $jars=@(Get-ChildItem -LiteralPath $d -Filter *.jar | Sort-Object LastWriteTime); if($jars.Count -gt 10){ $jars | Select-Object -First ($jars.Count-10) | ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force }; Write-Output ('[TestAll] Pruned docs/reports to newest 10 (' + (Get-ChildItem -LiteralPath $d -Filter *.jar).Count + ' jars kept)') }"
)

REM ------------------------------------------------------------------
REM Step 2c: leave nothing behind. The H2 server started in Step 0b is
REM stopped here (same as JsPlus-Tomcat/TestAll.bat Step 3); RC was
REM captured right after TestAll.js so this never changes the exit code.
REM ------------------------------------------------------------------
echo [TestAll] Stopping all remaining services...
call "%JS_PLUS_PATH%bin\StopAll.bat"

echo TestAll exit code: %RC%
exit /b %RC%

endlocal
