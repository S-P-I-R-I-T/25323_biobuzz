package org.firstinspires.ftc.teamcode.opmode;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.robot.Drive;
import org.firstinspires.ftc.teamcode.robot.Intake;
import org.firstinspires.ftc.teamcode.robot.Shooter;
import org.firstinspires.ftc.teamcode.robot.StartPose;
import org.firstinspires.ftc.teamcode.robot.Turret;
import org.firstinspires.ftc.teamcode.robot.Wedge;

/**
 * 25323 대회용 TeleOp.
 *
 * <pre>
 * gamepad1 (드라이버)
 *   왼쪽 스틱      이동 (필드 기준: 스틱 위 = 시작할 때 로봇이 보던 쪽)
 *   오른쪽 스틱    그 방향을 로봇이 바라보도록 회전 (스틱 위 = 시작할 때 보던 쪽, 놓으면 멈춤)
 *   D-pad 좌 / 우  제자리 회전 (반시계 / 시계)
 *   (이동·회전 모두 슬루 레이트 리미터로 부드럽게 가속·감속)
 *   RT 누르고 있기 느린 모드
 *   LB (또는 OPTIONS)  지금 방향을 "앞"으로 다시 정하기 (IMU 방향 리셋). START 누를 때도 자동으로 한 번 함
 *   SHARE          필드 기준 ↔ 로봇 기준 주행 바꾸기 (IMU가 이상할 때 비상용)
 *
 * gamepad2 (오퍼레이터)
 *   RT 누르고 있기 발사: ① 쐐기 발사 위치 + 슈터 가속 (블로커는 닫힌 채)
 *                  ② 목표 속도 도달 → 블로커 열림 → 잠깐 뒤 인테이크가 공을 밀어 넣음
 *                  ③ 한 발 쏘고 속도가 떨어지면 회복될 때까지 급탄을 멈췄다가 다시 (공 간격 일정)
 *                  → 놓으면 블로커 닫힘·급탄 정지·슈터 예열 속도로, 쐐기는 발사 전 상태로
 *   A / B          인테이크 줍기 / 뱉기 (누르는 동안)
 *   D-pad 좌 / 우  블로커 열기 / 닫기
 *   D-pad 상 / 하  쐐기 한 단계 내밀기 / 접기 (높게 접음 ↔ 낮게 접음(발사) ↔ 내밂(빼오기))
 *   오른쪽 스틱 X  터렛 돌리기
 *   LB / RB        터렛 천천히 왼쪽 / 오른쪽 (누르는 동안)
 *   X              터렛 가운데(인테이크 방향)로 복귀
 *   Y              슈터 예열 켜기/끄기 (기본 켜짐)
 * </pre>
 */
@TeleOp(name = "25323 TeleOp", group = "Competition")
public class MainTeleOp extends LinearOpMode {
    @Override
    public void runOpMode() {
        for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        Drive drive = new Drive(hardwareMap);
        Shooter shooter = new Shooter(hardwareMap);
        Intake intake = new Intake(hardwareMap);
        Wedge wedge = new Wedge(hardwareMap);
        Turret turret = new Turret(hardwareMap);

        // INIT: 기본은 아무것도 움직이지 않는다 (규칙 G403: 오토 → TeleOp 전환 시간에 INIT 으로 움직이면 반칙).
        // 오토 없이 TeleOp 으로 경기를 시작할 때만(인스펙션 18 in 자세 필요) gamepad1 A 로 시작 자세를 만든다.
        StartPose pose = new StartPose(turret, wedge);
        while (opModeInInit()) {
            if (!pose.started() && gamepad1.aWasPressed()) pose.begin();
            pose.initLoop();
            telemetry.addLine("INIT 중에는 로봇이 움직이지 않음 (전환 시간에 INIT 눌러도 안전).");
            telemetry.addData("시작 자세", pose.started() ? pose.status()
                    : "안 만듦 — 오토 없이 TeleOp 으로 경기 시작할 때만 gamepad1 A");
            telemetry.addData("자세 값", StartPose.values());
            telemetry.addLine("START: 쐐기 먼저 내려가고 0.5초 뒤 터렛 가운데. 필드 기준 '앞' = START 순간 로봇이 보는 방향.");
            telemetry.update();
        }
        waitForStart();
        if (isStopRequested()) return;

        drive.resetHeading();
        intake.closeBlocker();
        pose.beginRelease();   // 쐐기 먼저 STOW 로 내리고, 잠시 뒤 터렛 가운데
        Wedge.State wedgeBeforeShot = null;
        boolean preheat = true;
        boolean wasShooting = false;
        boolean feeding = false;
        boolean blockerOpenedForShot = false;
        ElapsedTime sinceBlockerOpen = new ElapsedTime();
        String shotStatus = "-";

        while (opModeIsActive()) {
            // ── 주행 (gamepad1) ──
            // 방향 리셋: LB (잘 보이는 버튼). OPTIONS 도 같은 기능으로 남겨 둠
            boolean resetPressed = gamepad1.leftBumperWasPressed();
            if (gamepad1.optionsWasPressed()) resetPressed = true;
            if (resetPressed) drive.resetHeading();
            if (gamepad1.shareWasPressed()) drive.setFieldCentric(!drive.isFieldCentric());
            drive.teleOp(-gamepad1.left_stick_y, gamepad1.left_stick_x,
                    gamepad1.right_stick_x, -gamepad1.right_stick_y,
                    gamepad1.dpad_left, gamepad1.dpad_right, gamepad1.right_trigger > 0.5);

            // ── 발사 (gamepad2 RT) ──
            if (gamepad2.yWasPressed()) preheat = !preheat;
            boolean shooting = gamepad2.right_trigger > 0.5;
            if (shooting) {
                if (!wasShooting) {
                    wedgeBeforeShot = wedge.state();
                    wedge.set(Wedge.State.SHOOT);
                    blockerOpenedForShot = false;   // 블로커는 슈터가 목표 속도에 도달한 뒤에 연다
                }
                shooter.setMode(Shooter.Mode.SHOOT);
                if (!blockerOpenedForShot && shooter.atShootSpeed()) {
                    intake.openBlocker();
                    blockerOpenedForShot = true;
                    sinceBlockerOpen.reset();
                }
                boolean speedOk = !Shooter.FEED_GATING
                        || shooter.velocity() >= Shooter.SHOOT_VELOCITY - Shooter.FEED_GATE_TOLERANCE;
                feeding = blockerOpenedForShot && sinceBlockerOpen.seconds() >= Shooter.OPEN_DELAY_S && speedOk;
                if (!blockerOpenedForShot) shotStatus = "가속 중 (블로커 닫힘)";
                else if (sinceBlockerOpen.seconds() < Shooter.OPEN_DELAY_S) shotStatus = "블로커 여는 중";
                else shotStatus = feeding ? "급탄 중" : "속도 회복 대기";
            } else {
                shotStatus = "-";
                if (wasShooting) {
                    intake.closeBlocker();
                    if (wedgeBeforeShot != null) wedge.set(wedgeBeforeShot);
                }
                feeding = false;
                shooter.setMode(preheat ? Shooter.Mode.IDLE : Shooter.Mode.OFF);
            }
            wasShooting = shooting;
            shooter.update();

            // ── 인테이크 (A/B가 발사 급탄보다 우선) ──
            if (gamepad2.a) intake.intake();
            else if (gamepad2.b) intake.outtake();
            else if (feeding) intake.feed();
            else intake.stop();

            // ── 블로커·쐐기 수동 ──
            if (gamepad2.dpadLeftWasPressed()) intake.openBlocker();
            if (gamepad2.dpadRightWasPressed()) intake.closeBlocker();
            if (gamepad2.dpadUpWasPressed()) wedge.stepOut();
            if (gamepad2.dpadDownWasPressed()) wedge.stepIn();

            // ── 터렛 ──
            if (pose.holdingTurret()) {
                // START 직후: 쐐기가 내려갈 때까지 터렛을 지금 자리에 잡아 둠. 지금 자리를 모르면 아무 명령도 안 보냄.
                if (turret.knowsPosition() || pose.started()) turret.update(0, 0);
            } else {
                if (gamepad2.xWasPressed()) turret.center();
                int fine = gamepad2.left_bumper ? -1 : gamepad2.right_bumper ? 1 : 0;
                turret.update(Math.abs(gamepad2.right_stick_x) < 0.05 ? 0 : gamepad2.right_stick_x, fine);
            }

            // ── 화면 ──
            telemetry.addData("주행", "%s  방향 %.1f°  %s", drive.isFieldCentric() ? "필드 기준" : "로봇 기준", drive.headingDeg(),
                    Double.isNaN(drive.aimTargetDeg()) ? "" : String.format("→ 목표 %.0f°", drive.aimTargetDeg()));
            telemetry.addData("슈터", "%.0f / %.0f  %s  (예열 %s)", shooter.velocity(), shooter.targetVelocity(),
                    shotStatus, preheat ? "켜짐" : "꺼짐");
            telemetry.addData("블로커", intake.blockerOpen() ? "열림" : "닫힘");
            telemetry.addData("쐐기", Wedge.label(wedge.state()));
            if (Turret.POSITION_MODE) {
                telemetry.addData("터렛", "위치 %.3f  (가운데 %.3f, 범위 %.3f~%.3f) %s", turret.targetPosition(),
                        Turret.CENTER_POS, Turret.MIN_POS, Turret.MAX_POS, turret.atLimit() ? "■ 끝" : "");
            } else if (turret.angleKnown()) {
                telemetry.addData("터렛", "%.1f°  파워 %.2f  %s%s", turret.angleDeg(), turret.power(),
                        turret.isCentering() ? "가운데로 복귀 중 " : "", turret.atLimit() ? "■ 한계각" : "");
            } else {
                telemetry.addData("터렛", "파워 %.2f  (%s)", turret.power(),
                        turret.hasFeedback() ? "기어비 미입력 → 각도 모름" : "피드백 없음 → 수동만");
            }
            telemetry.update();
        }

        drive.stop();
        shooter.stop();
        intake.stop();
        turret.stop();
    }
}
