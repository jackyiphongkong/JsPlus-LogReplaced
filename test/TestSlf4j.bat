@echo off
setlocal
call "%~dp0_RunOne.bat" TestSlf4j 1 %~1
exit /b %ERRORLEVEL%
