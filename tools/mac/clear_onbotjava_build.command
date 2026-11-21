#!/bin/bash
# 로봇에 남은 OnBotJava "컴파일 결과물"(/sdcard/FIRST/java/build)을 지운다.
# 웹에서 OnBotJava 파일을 지워도 컴파일 결과물은 남아 있어서, Sloth가 그걸 읽다가 앱이 계속 죽을 수 있다.
# 로봇 앱이 켜졌다 꺼졌다 해도 됨: 연결되는 순간을 기다렸다가 실행한다. (최대 2분)
source "$(dirname "$0")/_common.sh"
T="$ROBOT_IP:5555"
echo "▶ 로봇이 연결되는 순간을 기다리는 중... (Mac Wi-Fi가 로봇에 붙어 있어야 함)"
for i in $(seq 1 60); do
  "$ADB" connect "$T" >/dev/null 2>&1
  if [ "$("$ADB" -s "$T" get-state 2>/dev/null)" = "device" ]; then
    echo "✔ 연결됨. 지울 폴더 내용:"
    "$ADB" -s "$T" shell 'ls -la /sdcard/FIRST/java/ /sdcard/FIRST/java/build 2>&1'
    echo
    read -p "위 /sdcard/FIRST/java/build 폴더를 지울까요? (y 입력 후 Enter) " yn
    if [ "$yn" = "y" ]; then
      "$ADB" connect "$T" >/dev/null 2>&1
      if "$ADB" -s "$T" shell 'rm -rf /sdcard/FIRST/java/build && echo DELETED'; then
        echo "✔ 지움. 로봇 전원을 껐다 켜세요."
      else
        echo "✘ 그사이 연결이 끊겼습니다. 이 스크립트를 다시 실행하세요."
      fi
    else
      echo "취소함."
    fi
    pause; exit 0
  fi
  sleep 2
done
echo "✘ 2분 동안 연결이 안 됐습니다. Wi-Fi 연결을 확인하세요."
pause
