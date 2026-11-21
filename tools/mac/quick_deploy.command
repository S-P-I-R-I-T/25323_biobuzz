#!/bin/bash
# 코드(teamcode 안의 .java)만 바꿨을 때. 몇 초면 끝남. 실행 중인 OpMode를 끝내면 새 코드가 적용됨.
# 전체설치를 한 번도 안 했거나, 라이브러리를 바꿨으면 full_install 을 쓸 것.
source "$(dirname "$0")/_common.sh"
connect_robot || { pause; exit 1; }
run_gradle :TeamCode:deploySloth && echo "✔ 올리기 끝. 실행 중인 OpMode를 멈췄다가 다시 시작하세요."
pause
