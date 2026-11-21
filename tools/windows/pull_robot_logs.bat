@echo off
chcp 65001 >nul
rem 로봇이 끊기거나 오류가 날 때: 로봇 쪽 기록을 레포 옆 robot_logs 폴더로 가져온다.
call "%~dp0_common.bat"
call "%~dp0_connect.bat"
if errorlevel 1 goto end
for /f "delims=" %%T in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "TS=%%T"
set "OUT=%LOG_ROOT%\%TS%"
mkdir "%OUT%" 2>nul
"%ADB%" pull /sdcard/robotControllerLog.txt "%OUT%"
"%ADB%" logcat -d > "%OUT%\logcat.txt"
echo [성공] 저장: %OUT%
start "" "%OUT%"
:end
pause
