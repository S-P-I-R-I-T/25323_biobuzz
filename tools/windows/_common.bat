@echo off
chcp 65001 >nul
rem 로봇 작업용 공통 설정 (Windows). 직접 실행하지 말고 다른 .bat 파일이 불러 씀.
rem 이 스크립트는 레포의 tools\windows\ 안에 있고, 자기가 들어 있는 레포를 대상으로 동작한다.

for %%I in ("%~dp0..\..") do set "REPO=%%~fI"
for %%I in ("%~dp0..\..") do set "REPO_NAME=%%~nxI"
for %%I in ("%~dp0..\..\..") do set "LOG_ROOT=%%~fI\robot_logs"

rem Java: Android Studio에 들어 있는 것을 사용 (PC에 따로 설치된 낮은 버전 Java로는 Gradle이 안 돎)
set "AS_JBR="
if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "AS_JBR=%LOCALAPPDATA%\Programs\Android Studio\jbr"
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "AS_JBR=%ProgramFiles%\Android\Android Studio\jbr"
if defined AS_JBR set "JAVA_HOME=%AS_JBR%"
if not defined AS_JBR echo [주의] Android Studio를 찾지 못했습니다. 기본 위치에 설치하세요.

rem Android SDK: 이미 설정돼 있으면 그대로, 아니면 Android Studio 기본 위치
if not defined ANDROID_HOME if defined ANDROID_SDK_ROOT set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
if not exist "%ADB%" echo [주의] adb가 없습니다: %ADB%  -- Android Studio, SDK Manager, SDK Tools, Platform-Tools 설치
set "ROBOT_IP=192.168.43.1"
exit /b 0
