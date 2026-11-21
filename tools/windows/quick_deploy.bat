@echo off
chcp 65001 >nul
rem 코드만 바꿨을 때. 몇 초면 끝남. 전체설치를 한 번은 먼저 해야 함.
call "%~dp0_common.bat"
call "%~dp0_connect.bat"
if errorlevel 1 goto end
pushd "%REPO%"
for /f "delims=" %%B in ('git branch --show-current 2^>nul') do echo [레포] %REPO_NAME%  브랜치 %%B
call gradlew.bat :TeamCode:deploySloth --console=plain
if errorlevel 1 (
  echo [실패] 위 오류 메시지를 확인하세요.
) else (
  echo [성공] 올리기 끝. 실행 중인 OpMode를 멈췄다가 다시 시작하세요.
)
popd
:end
pause
