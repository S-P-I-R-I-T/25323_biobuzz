@echo off
chcp 65001 >nul
rem 처음 한 번, 그리고 라이브러리나 build.gradle을 바꿨을 때. 보통 1~2분.
call "%~dp0_common.bat"
call "%~dp0_connect.bat"
if errorlevel 1 goto end
pushd "%REPO%"
for /f "delims=" %%B in ('git branch --show-current 2^>nul') do echo [레포] %REPO_NAME%  브랜치 %%B
call gradlew.bat :TeamCode:removeSlothRemote :TeamCode:installDebug --console=plain
if errorlevel 1 (
  echo [실패] 위 오류 메시지를 확인하세요.
) else (
  echo [성공] 설치 끝. Driver Station에서 OpMode를 고르면 됩니다.
)
popd
:end
pause
