@echo off
setlocal
call "%~dp0_RunOne.bat" TestCommonsLogging 1 %~1
exit /b %ERRORLEVEL%
