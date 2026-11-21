로봇 작업 도구 (더블클릭 스크립트) 사용법
=============================================

이 폴더의 스크립트는 로봇에 코드를 올리고, 대시보드를 열고, 문제가 생겼을 때 기록을 가져오는 일을
더블클릭 한 번으로 해 줍니다. 스크립트는 자기가 들어 있는 레포(이 레포)를 대상으로 동작합니다.

  Mac      : tools/mac/*.command
  Windows  : tools\windows\*.bat


1. 먼저 준비할 것 (컴퓨터마다 한 번)
-------------------------------------
- Android Studio 설치 (기본 위치에 설치할 것)
  · Mac     : /Applications/Android Studio.app
  · Windows : C:\Program Files\Android\Android Studio  (기본값)
- Android Studio → Settings → Android SDK → SDK Tools 탭에서 "Android SDK Platform-Tools" 체크 후 설치
  (로봇과 연결하는 adb 프로그램이 여기 들어 있음)
- 이 레포를 한 번은 Android Studio로 열어서 Gradle Sync가 끝나게 해 두세요 (처음에 필요한 파일을 내려받음).


2. 사용 순서
------------
1) 컴퓨터 Wi-Fi를 로봇(Control Hub) 네트워크에 연결
2) full_install 실행 — 처음 한 번, 그리고 build.gradle이나 라이브러리를 바꿨을 때 (1~2분)
3) 이후 .java 코드만 바꿨으면 quick_deploy 실행 (몇 초). 실행 중인 OpMode를 멈췄다 다시 시작하면 적용됨
4) open_dashboards 로 Panels(값 실시간 수정) / Pedro 튜닝 페이지 / Pedro Visualizer 열기
※ 로봇 Wi-Fi에 붙어 있는 동안에는 인터넷이 안 됩니다 (Visualizer는 인터넷 필요).


3. 파일 목록 (Mac은 .command, Windows는 .bat — 이름은 같음)
-----------------------------------------------------------
  connect_robot          로봇 연결만 (Android Studio에서 Run 하기 전에도 필요)
  full_install           코드 전체 설치
  quick_deploy           코드 빠르게 올리기 (Sloth)
  open_dashboards        대시보드 열기
  pull_robot_logs        로봇 기록(로그) 가져오기 → 레포 옆 robot_logs 폴더 (레포 밖이라 git에 안 섞임)
  clear_onbotjava_build  OnBotJava 빌드 찌꺼기 지우기 (로봇 앱이 켜졌다 꺼졌다 반복할 때)

  _common.sh / _common.bat / _connect.bat 는 다른 스크립트가 불러 쓰는 파일이라 직접 실행하지 않습니다.


4. 처음 실행할 때 경고가 뜨면
-----------------------------
- Mac: "확인되지 않은 개발자" → 파일을 오른쪽 클릭 → 열기 → 열기.
  실행 권한이 없다고 나오면 터미널에서 레포 폴더로 가서:  chmod +x tools/mac/*.command
- Windows: "Windows의 PC 보호" 파란 창 → 추가 정보 → 실행.
  한글이 깨져 보이면 글자 표시만 문제이고 동작은 같습니다 (Windows 10 이상 권장).


5. 자주 나는 문제
-----------------
- "연결 실패"         → Wi-Fi가 로봇에 붙어 있는지, 로봇 전원이 켜져 있는지 확인
- "adb가 없습니다"     → 1번의 Platform-Tools 설치
- "Android Studio를 찾지 못했습니다" → 기본 위치에 설치했는지 확인
- 빌드가 "defined multiple times" 같은 오류로 깨짐 (Mac, iCloud 바탕화면 동기화)
  → Mac 스크립트가 빌드 전에 자동으로 정리함. 그래도 안 되면 Android Studio에서 Build → Clean Project
- quick_deploy가 이상하게 동작 → full_install로 다시 올리기

※ Windows용 스크립트는 Mac에서 만들어서 실제 Windows PC에서는 아직 시험해 보지 않았습니다.
  처음 쓸 때 문제가 있으면 화면에 나온 메시지를 캡처해서 코치에게 알려 주세요.
