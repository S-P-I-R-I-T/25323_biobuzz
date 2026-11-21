package org.firstinspires.ftc.teamcode.opmode;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.robot.Drive;
import org.firstinspires.ftc.teamcode.robot.Intake;
import org.firstinspires.ftc.teamcode.robot.Shooter;
import org.firstinspires.ftc.teamcode.robot.StartPose;
import org.firstinspires.ftc.teamcode.robot.TimedDrive;
import org.firstinspires.ftc.teamcode.robot.Turret;
import org.firstinspires.ftc.teamcode.robot.Wedge;

/**
 * 오토 (RED / BLUE 공용): 상향 셀 앞에서 미리 실은 공 발사 → 시간 × 파워로 LOADING ZONE 주차 (Pinpoint 없이, 11/21).
 * 동선은 ftc-tactic-sim 경기 파일 tacticsim-match_20261121-1252_RED_29pts 를 재생해 뽑은 것 (시뮬레이터 좌표는 화면 기준 y 아래쪽 +,
 * 로봇 오른쪽 = (−sinψ, cosψ)):
 *   RED 시작 (60.1, 134.2), 정면 = 필드 −y, 뒤 = RED GARDEN 벽, 왼쪽 = RED LOADING ZONE 벽
 *   → 그 자리에서 발사 → 왼쪽(필드 −x) 30 in → 앞(필드 −y) 87 in → 왼쪽 16 in → (14, 47) 근처 정지.
 *   (11/21 변경: 원래 시뮬레이터는 앞 103 in → ZONE 가운데(y 31). 먼 쪽에서 동맹이 들어오므로
 *    ZONE 의 가까운 쪽 끝(y 47)에 로봇 절반(9 in)만 걸치게 앞 87 in 로 줄임.)
 *   RED LOADING ZONE = x 0~11, y 24~47. 로봇(18 in)이 일부만 걸쳐도 PARK, 벽에 안 닿아 있으면 LEAVE 까지 (3 + 5 = 8점).
 *   로봇 중심이 LOADING ZONE 벽에서 20 in 안쪽이고, 가까운 쪽 끝(y 47)에서 9 in 넘게 덜 가지 않으면 PARK
 *   (= 앞 구간이 78 in 보다 짧으면 ZONE 밖. 87 in 이면 9 in 여유, 더 가면 동맹 쪽으로 들어감).
 * BLUE 는 RED 의 필드 중심 점대칭(180° 회전)이라 로봇 기준 동작(왼쪽 → 앞 → 왼쪽)이 똑같다. 놓는 위치만 BLUE 쪽 대칭 자리.
 *
 * 순서: INIT 시작 자세(StartPose) → START: 쐐기 내림 → 터렛 가운데 → 발사 → (대기) → 주행 3구간.
 * 구간 거리를 0으로 두면 그 구간은 건너뛴다. 속도(TimedDrive.FORWARD_IPS / STRAFE_IPS)는 Auto Drive Calibration 으로 잰다.
 */
@Configurable
@Autonomous(name = "Shoot + Park Auto (상향셀, RED/BLUE)", group = "Competition")
public class ShootParkAuto extends LinearOpMode {
    /** 발사할지. false 면 주차만. */
    public static boolean SHOOT = true;
    /** 터렛이 가운데로 돌아온 뒤 발사 전까지 기다리는 시간 (초). */
    public static double TURRET_SETTLE_S = 1.5;
    /** 슈터가 속도에 못 도달해도 이 시간이 지나면 그냥 쏜다 (초). */
    public static double SPINUP_TIMEOUT_S = 3.0;
    /** 블로커를 연 뒤 공을 밀어 넣는 시간 (초). 공 3~4개가 다 나갈 만큼. */
    public static double FEED_TIME_S = 3.5;
    /** 발사 후 주행 전에 기다리는 시간 (초). 같은 편 로봇과 부딪히지 않게 조절. */
    public static double START_DELAY_S = 0.0;

    /** 1구간: 왼쪽 거리 (inch). LOADING ZONE 벽 쪽으로, 터널(HIVE 아래)을 피해 벽에서 30 in 줄로. */
    public static double LEG1_LEFT_IN = 30;
    /** 2구간: 앞(+) 거리 (inch). LOADING ZONE 가까운 쪽 끝에 로봇 절반이 걸치는 높이까지 (먼 쪽은 동맹 자리). 78 미만이면 ZONE 밖. */
    public static double LEG2_FORWARD_IN = 95;
    /** 3구간: 왼쪽 거리 (inch). 벽 쪽으로 붙이되 벽에 닿지 않게 (LEAVE 3점 유지). */
    public static double LEG3_LEFT_IN = 24;

    @Override
    public void runOpMode() {
        for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }
        Drive drive = new Drive(hardwareMap);
        Shooter shooter = new Shooter(hardwareMap);
        Intake intake = new Intake(hardwareMap);
        Turret turret = new Turret(hardwareMap);
        Wedge wedge = new Wedge(hardwareMap);

        StartPose pose = new StartPose(turret, wedge);
        pose.begin();
        while (opModeInInit()) {
            pose.initLoop();
            telemetry.addData("시작 자세", pose.status());
            telemetry.addData("자세 값", StartPose.values());
            telemetry.addLine("놓는 위치: 뒤 = 우리 GARDEN 벽에 붙임, 왼쪽 = 우리 LOADING ZONE 벽, 로봇 중심 ~ 그 벽 60 in.");
            telemetry.addData("발사", SHOOT ? "함" : "안 함 (주차만)");
            telemetry.addData("주행", "왼쪽 %.0f → 앞 %.0f → 왼쪽 %.0f in  (속도 앞 %.0f / 옆 %.0f in/s)",
                    LEG1_LEFT_IN, LEG2_FORWARD_IN, LEG3_LEFT_IN, TimedDrive.FORWARD_IPS, TimedDrive.STRAFE_IPS);
            telemetry.update();
        }
        waitForStart();
        if (isStopRequested()) return;

        drive.resetHeading();
        TimedDrive timed = new TimedDrive(drive, this);
        intake.closeBlocker();

        // 1) 시작 자세 풀기: 쐐기 먼저 내림 → 터렛 가운데 (쐐기가 올라간 채로 터렛을 돌리면 안 됨)
        pose.beginRelease();
        while (opModeIsActive() && pose.holdingTurret()) {
            status("쐐기 내리는 중", shooter);
        }
        ElapsedTime timer = new ElapsedTime();

        // 2) 발사 (슈터는 터렛이 도는 동안 미리 가속)
        if (SHOOT) {
            wedge.set(Wedge.State.SHOOT);
            shooter.setMode(Shooter.Mode.SHOOT);
            while (opModeIsActive() && timer.seconds() < TURRET_SETTLE_S) {
                shooter.update();
                status("터렛 가운데로 + 슈터 가속", shooter);
            }
            timer.reset();
            while (opModeIsActive() && !shooter.atShootSpeed() && timer.seconds() < SPINUP_TIMEOUT_S) {
                shooter.update();
                status("슈터 가속", shooter);
            }
            intake.openBlocker();
            timer.reset();
            while (opModeIsActive() && timer.seconds() < Shooter.OPEN_DELAY_S) {
                shooter.update();
                status("블로커 여는 중", shooter);
            }
            timer.reset();
            while (opModeIsActive() && timer.seconds() < FEED_TIME_S) {
                shooter.update();
                boolean speedOk = !Shooter.FEED_GATING
                        || shooter.velocity() >= Shooter.SHOOT_VELOCITY - Shooter.FEED_GATE_TOLERANCE;
                if (speedOk) intake.feed(); else intake.stop();
                status(speedOk ? "발사 중" : "속도 회복 대기", shooter);
            }
            intake.stop();
            intake.closeBlocker();
            shooter.stop();
            wedge.set(Wedge.State.STOW);
        } else {
            while (opModeIsActive() && timer.seconds() < TURRET_SETTLE_S) status("터렛 가운데로", shooter);
        }

        // 3) 대기 후 주차
        timed.pause(START_DELAY_S);
        if (LEG1_LEFT_IN != 0) timed.strafe(-LEG1_LEFT_IN);
        if (LEG2_FORWARD_IN != 0) timed.forward(LEG2_FORWARD_IN);
        if (LEG3_LEFT_IN != 0) timed.strafe(-LEG3_LEFT_IN);
        drive.stop();

        while (opModeIsActive()) {
            status("끝 (주차)", shooter);
        }
    }

    private void status(String step, Shooter shooter) {
        telemetry.addData("단계", step);
        telemetry.addData("슈터", "%.0f / %.0f", shooter.velocity(), shooter.targetVelocity());
        telemetry.update();
    }
}
