@echo off
chcp 65001 >nul
call "%~dp0_common.bat"
call "%~dp0_connect.bat"
"%ADB%" devices
pause
