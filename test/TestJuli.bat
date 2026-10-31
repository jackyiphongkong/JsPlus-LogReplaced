@echo off
setlocal
call "%~dp0_RunOne.bat" TestJuli 1 %~1
exit /b %ERRORLEVEL%
