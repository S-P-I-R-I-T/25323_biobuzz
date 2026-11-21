#!/bin/bash
# 로봇 작업용 공통 설정 (macOS). 직접 실행하지 말고 다른 .command 파일이 불러 씀.
# 이 스크립트는 레포의 tools/mac/ 안에 있고, 자기가 들어 있는 레포를 대상으로 동작한다.

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPO_NAME="$(basename "$REPO")"
LOG_ROOT="$(dirname "$REPO")/robot_logs"   # 로그는 레포 밖(레포 옆 폴더)에 저장 → git에 안 섞임

# Java: Android Studio에 들어 있는 것을 우선 사용 (터미널 기본 Java가 낮으면 Gradle이 안 돎)
for jbr in "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
           "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"; do
  if [ -x "$jbr/bin/java" ]; then export JAVA_HOME="$jbr"; break; fi
done
if [ -z "$JAVA_HOME" ]; then echo "⚠ Android Studio를 찾지 못했습니다. Android Studio를 /Applications 에 설치하세요."; fi

# Android SDK: 이미 설정돼 있으면 그대로, 아니면 Android Studio 기본 위치
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="$ANDROID_HOME/platform-tools/adb"
if [ ! -x "$ADB" ]; then echo "⚠ adb가 없습니다 ($ADB). Android Studio → SDK Manager → SDK Tools → Platform-Tools 설치."; fi
ROBOT_IP="192.168.43.1"

pause() { echo; read -n1 -s -p "아무 키나 누르면 창이 닫힙니다..."; echo; }

connect_robot() {
  echo "▶ 로봇 연결 중 ($ROBOT_IP) — 맥이 로봇 Wi-Fi에 연결되어 있어야 합니다."
  "$ADB" connect "$ROBOT_IP:5555"
  if [ "$("$ADB" -s "$ROBOT_IP:5555" get-state 2>/dev/null)" = "device" ]; then
    echo "✔ 로봇 연결됨"
    return 0
  fi
  echo "✘ 연결 실패: Wi-Fi가 로봇(Control Hub)에 연결되어 있는지, 로봇 전원이 켜져 있는지 확인하세요."
  return 1
}

# $@ = gradle 작업
run_gradle() {
  cd "$REPO" || { echo "✘ 레포 폴더를 찾을 수 없음: $REPO"; return 1; }
  echo "▶ $REPO_NAME : 브랜치 $(git branch --show-current 2>/dev/null || echo '?')"
  # iCloud(바탕화면 동기화)가 빌드 폴더에 "파일 2.class" 같은 복제본을 만들면 빌드가 깨진다 → 빌드 결과물 폴더 안의 복제본만 지움
  local dups
  dups=$(find . -path "*/build/*" -name "* 2*" 2>/dev/null | wc -l | tr -d ' ')
  if [ "$dups" != "0" ]; then
    echo "  (iCloud 복제 파일 $dups 개를 빌드 폴더에서 지움)"
    find . -path "*/build/*" -name "* 2*" -delete 2>/dev/null
  fi
  ./gradlew "$@" --console=plain
}
