@echo off
chcp 65001 >nul
rem 로봇에 남은 OnBotJava 컴파일 결과물(/sdcard/FIRST/java/build)을 지운다.
rem 앱이 켜졌다 꺼졌다 반복할 때 사용. 연결되는 순간을 최대 2분 기다린다.
call "%~dp0_common.bat"
set /a TRIES=0
:wait
"%ADB%" connect %ROBOT_IP%:5555 >nul 2>&1
set "STATE="
for /f "usebackq delims=" %%S in (`""%ADB%" -s %ROBOT_IP%:5555 get-state 2^>nul"`) do set "STATE=%%S"
if "%STATE%"=="device" goto connected
set /a TRIES+=1
if %TRIES% GEQ 60 goto timeout
timeout /t 2 /nobreak >nul
goto wait
:connected
echo [연결] 지울 폴더 내용:
"%ADB%" -s %ROBOT_IP%:5555 shell "ls -la /sdcard/FIRST/java/ /sdcard/FIRST/java/build"
set /p YN=위 /sdcard/FIRST/java/build 폴더를 지울까요? y 입력 후 Enter: 
if /i not "%YN%"=="y" goto cancel
"%ADB%" connect %ROBOT_IP%:5555 >nul 2>&1
"%ADB%" -s %ROBOT_IP%:5555 shell "rm -rf /sdcard/FIRST/java/build && echo DELETED"
if errorlevel 1 (
  echo [실패] 그사이 연결이 끊겼습니다. 다시 실행하세요.
) else (
  echo [성공] 지움. 로봇 전원을 껐다 켜세요.
)
goto end
:cancel
echo 취소함.
goto end
:timeout
echo [실패] 2분 동안 연결이 안 됐습니다. Wi-Fi 연결을 확인하세요.
:end
pause
