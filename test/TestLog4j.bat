@echo off
setlocal
call "%~dp0_RunOne.bat" TestLog4j 1 %~1
exit /b %ERRORLEVEL%
