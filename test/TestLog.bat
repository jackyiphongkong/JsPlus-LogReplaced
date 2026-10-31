@echo off
setlocal
call "%~dp0_RunOne.bat" TestLog 0 %~1
exit /b %ERRORLEVEL%
