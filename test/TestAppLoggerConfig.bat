@echo off
setlocal
call "%~dp0_RunOne.bat" TestAppLoggerConfig 0 %~1
exit /b %ERRORLEVEL%
