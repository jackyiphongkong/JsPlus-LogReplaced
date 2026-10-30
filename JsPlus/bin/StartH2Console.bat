@echo off
setlocal enabledelayedexpansion

REM ------------------------------------------------------------------
REM JsPlus-LogReplaced H2 database console / initializer
REM Usage:
REM   StartH2Console.bat                 Interactive shell (default DB: logdb)
REM   StartH2Console.bat <DB_NAME>       Interactive shell on the given DB
REM   StartH2Console.bat init [DB_NAME]  Reset + rebuild schema:
REM                                      fallback.sql -> change.sql
REM                                      (default DB: logdb, matching
REM                                       config/DataSource.properties)
REM Same shape as JsPlus-Core/JsPlus/bin/StartH2Console.bat with two
REM project-specific differences:
REM   * default DB is logdb (Core defaults to appdb, the Tomcat app DB)
REM   * the DB user is sample_user / sample_password, NOT sa - this
REM     project's Database/logdb is created by sample_user on the first
REM     connection, so "sa" does not exist ("Wrong user name or password
REM     [28000-240]"); config/DataSource.properties ships the same account.
REM NOTE: "init" opens the database file directly, so stop the server first
REM       (JsPlus\bin\StopH2.bat) - a running H2 holds the file lock.
REM ------------------------------------------------------------------

set "INIT_MODE=0"
if /i "%1"=="init" (
    set "INIT_MODE=1"
    if "%2"=="" (
        set "DB_NAME=logdb"
    ) else (
        set "DB_NAME=%2"
    )
) else (
    if "%1"=="" (
        set "DB_NAME=logdb"
    ) else (
        set "DB_NAME=%1"
    )
)

REM Get script directory and parent path
set BIN_PATH=%~dp0
set JS_PLUS_PATH_ORIGIN=%BIN_PATH%..\
SET "JS_PLUS_PATH=%JS_PLUS_PATH_ORIGIN:\=/%"
set LIB_HOME=%JS_PLUS_PATH%../LibHome/

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

echo "%JvmParamsName%"

if "%JvmParams%"=="" (
    SET "JvmParams=-Xms128m -Xmx128m -Xss256k"
    echo WARN: Missing %JvmParamsName% then using default params
) else (
    echo Using System Env JVM Params
)

echo Using "%JvmParams%"

set "DB_USER=sample_user"
set "DB_PASSWORD=sample_password"
set "DB_URL=jdbc:h2:%JS_PLUS_PATH%../Database/%DB_NAME%"

echo [StartH2Console] DB=%DB_NAME% url=%DB_URL% user=%DB_USER%

if "%INIT_MODE%"=="1" (
    echo [StartH2Console] Initializing database "%DB_NAME%"...
    echo [StartH2Console] NOTE: the H2 server must be stopped ^(StopH2.bat^) - init opens the DB file directly.
    echo [StartH2Console] Executing dbscript/fallback.sql ...
    "%JAVA_BIN%" %JvmParams% -cp "%DEPEND_LIBS%" org.h2.tools.RunScript -url "%DB_URL%" -user "%DB_USER%" -password "%DB_PASSWORD%" -script "%JS_PLUS_PATH%../dbscript/fallback.sql" -checkResults
    if errorlevel 1 (
        echo [StartH2Console] ERROR: fallback.sql failed
        exit /b 1
    )
    echo [StartH2Console] Executing dbscript/change.sql ...
    "%JAVA_BIN%" %JvmParams% -cp "%DEPEND_LIBS%" org.h2.tools.RunScript -url "%DB_URL%" -user "%DB_USER%" -password "%DB_PASSWORD%" -script "%JS_PLUS_PATH%../dbscript/change.sql" -checkResults
    if errorlevel 1 (
        echo [StartH2Console] ERROR: change.sql failed
        exit /b 1
    )
    echo [StartH2Console] Database "%DB_NAME%" initialized: sample_user + app_logger_config + app_logger_msg.
) else (
    "%JAVA_BIN%" %JvmParams% -cp "%DEPEND_LIBS%" org.h2.tools.Shell -url "%DB_URL%" -user "%DB_USER%" -password "%DB_PASSWORD%"
)

endlocal
