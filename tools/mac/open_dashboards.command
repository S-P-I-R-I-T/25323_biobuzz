#!/bin/bash
source "$(dirname "$0")/_common.sh"
# Panels: 실시간 값 확인·수정(@Configurable), 텔레메트리, OpMode 실행
open "http://$ROBOT_IP:8001"
# Pedro 튜닝 페이지: Mecanum / Pinpoint / Foresight / Shooter 튜너
open "http://$ROBOT_IP:10158"
# Pedro 경로 그리기 (인터넷 필요)
open "https://visualizer.pedropathing.com/"
