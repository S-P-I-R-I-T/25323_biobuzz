@echo off
rem 로봇에 adb로 연결. 성공하면 errorlevel 0, 실패하면 1.
echo [연결] 로봇 연결 중 %ROBOT_IP% -- PC Wi-Fi가 로봇 Control Hub에 연결되어 있어야 합니다.
"%ADB%" connect %ROBOT_IP%:5555
set "STATE="
for /f "usebackq delims=" %%S in (`""%ADB%" -s %ROBOT_IP%:5555 get-state 2^>nul"`) do set "STATE=%%S"
if "%STATE%"=="device" goto ok
echo [실패] Wi-Fi가 로봇에 연결되어 있는지, 로봇 전원이 켜져 있는지 확인하세요.
exit /b 1
:ok
echo [성공] 로봇 연결됨
exit /b 0
