@echo off
setlocal
call "%~dp0_RunOne.bat" TestConsole 0 %~1
exit /b %ERRORLEVEL%
