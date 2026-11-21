#!/bin/bash
# 처음 한 번, 그리고 라이브러리나 build.gradle을 바꿨을 때. (보통 1~2분)
source "$(dirname "$0")/_common.sh"
connect_robot || { pause; exit 1; }
run_gradle :TeamCode:removeSlothRemote :TeamCode:installDebug && echo "✔ 설치 끝. Driver Station에서 OpMode를 고르면 됩니다."
pause
