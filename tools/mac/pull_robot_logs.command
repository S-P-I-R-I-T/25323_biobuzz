#!/bin/bash
# 로봇이 끊기거나 오류가 날 때: 로봇 쪽 기록을 레포 옆 robot_logs 폴더로 가져온다.
source "$(dirname "$0")/_common.sh"
connect_robot || { pause; exit 1; }
OUT="$LOG_ROOT/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$OUT"
"$ADB" pull /sdcard/robotControllerLog.txt "$OUT/" 2>/dev/null || echo "(robotControllerLog.txt 없음)"
"$ADB" logcat -d > "$OUT/logcat.txt" 2>/dev/null
echo "✔ 저장: $OUT"
open "$OUT"
pause
