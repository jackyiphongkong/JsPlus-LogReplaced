@echo off
setlocal enabledelayedexpansion

REM ==================================================================
REM JsPlus-LogReplaced release helper - release.bat
REM Scans every sibling project folder under the scan root (default
REM D:/Working). Any project that contains a LibHome/jsplus/default
REM folder is a release target: the freshly built
REM jsplus-logreplaced-<version>.jar is copied into its
REM LibHome/jsplus/default. The previous jsplus-logreplaced-*.jar
REM there is renamed to *.jar.bak (not deleted); other jars are left
REM untouched.
REM The source project itself (JsPlus-LogReplaced) is skipped.
REM Scan root   : default parent folder of this project, override via JS_PLUS_RELEASE_ROOT
REM Exit code   : 0 = success, 1 = failure
REM ==================================================================

set LR_ROOT=%~dp0
if "%LR_ROOT:~-1%"=="\" set LR_ROOT=%LR_ROOT:~0,-1%

REM ---- locate the latest built project jar ----
set JAR_DIR=%LR_ROOT%\LibHome\jsplus\default
if not exist "%JAR_DIR%" (
    echo [release] ERROR: build output dir not found: %JAR_DIR%
    exit /b 1
)
set "LATEST_JAR="
for /f "delims=" %%f in ('dir /b /o-d "%JAR_DIR%\jsplus-logreplaced-*.jar" 2^>nul') do (
    if not defined LATEST_JAR set "LATEST_JAR=%%f"
)
if not defined LATEST_JAR (
    echo [release] ERROR: no jsplus-logreplaced-*.jar under %JAR_DIR% - run 'ant build' first.
    exit /b 1
)
set SRC_JAR=%JAR_DIR%\%LATEST_JAR%
echo [release] Source : %SRC_JAR%

REM ---- scan root (defaults to the parent folder of this project) ----
set "SCAN_ROOT=%JS_PLUS_RELEASE_ROOT%"
if not defined JS_PLUS_RELEASE_ROOT (
    for %%i in ("%~dp0..") do set "SCAN_ROOT=%%~fi"
)
echo [release] Scan root : %SCAN_ROOT%
set LR_NORM=%LR_ROOT:\=/%

REM ---- iterate every sibling project ----
set COPIED=0
for /d %%d in ("%SCAN_ROOT%\*") do (
    set PROJ=%%d
    set PROJ_NORM=!PROJ:\=/!
    set TGT_DIR=!PROJ!\LibHome\jsplus\default
    if /i not "!PROJ_NORM!"=="%LR_NORM%" (
        if exist "!TGT_DIR!" (
            echo [release] Target : !TGT_DIR!
            for /f "delims=" %%f in ('dir /b "!TGT_DIR!\jsplus-logreplaced-*.jar" 2^>nul') do (
                if exist "!TGT_DIR!\%%f.bak" del /q "!TGT_DIR!\%%f.bak" >nul 2>&1
                ren "!TGT_DIR!\%%f" "%%f.bak" >nul 2>&1
            )
            copy /y "%SRC_JAR%" "!TGT_DIR!\%LATEST_JAR%" >nul
            if errorlevel 1 (
                echo [release] ERROR: copy to !TGT_DIR! failed
                exit /b 1
            )
            echo [release]   Copied : !TGT_DIR!\%LATEST_JAR%
            set /a COPIED+=1
        )
    )
)
if "%COPIED%"=="0" echo [release] No target project found under %SCAN_ROOT%
echo [release] Done.
exit /b 0
