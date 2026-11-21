#!/bin/bash
# 로봇 연결만 (Android Studio에서 Run 하기 전에도 필요)
source "$(dirname "$0")/_common.sh"
connect_robot
"$ADB" devices
pause
