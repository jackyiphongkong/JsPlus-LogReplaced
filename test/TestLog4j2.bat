@echo off
setlocal
call "%~dp0_RunOne.bat" TestLog4j2 1 %~1
exit /b %ERRORLEVEL%
