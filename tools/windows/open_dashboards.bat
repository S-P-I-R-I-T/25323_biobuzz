@echo off
chcp 65001 >nul
call "%~dp0_common.bat"
rem Panels: 실시간 값 확인/수정, 텔레메트리, OpMode 실행
start "" "http://%ROBOT_IP%:8001"
rem Pedro 튜닝 페이지
start "" "http://%ROBOT_IP%:10158"
rem Pedro 경로 그리기 (인터넷 필요)
start "" "https://visualizer.pedropathing.com/"
