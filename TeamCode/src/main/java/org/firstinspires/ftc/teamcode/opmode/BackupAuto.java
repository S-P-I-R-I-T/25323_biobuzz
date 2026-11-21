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
import org.firstinspires.ftc.teamcode.robot.Turret;
import org.firstinspires.ftc.teamcode.robot.Wedge;

/**
 * 25323 대회용 오토 (주차만). 위치 센서 없이 "시간 × 파워"로만 움직인다.
 * 2026-11-21: Pinpoint 좌우 데드휠 고장(여분 없음) → Pedro 경로 오토는 포기하고 이것을 쓰기로 함.
 * 기본값은 발사 없이 주차만 한다 (SHOOT_PRELOAD = false).
 *
 * <pre>
 * 1) (SHOOT_PRELOAD 면) 그 자리에서 미리 실은 공 발사: 슈터 가속 → 블로커 열고 FEED_TIME_S 동안 급탄
 * 2) START_DELAY_S 만큼 대기 (같은 편 로봇과 부딪히지 않게)
 * 3) 로봇 기준으로 DRIVE_FORWARD / DRIVE_STRAFE 파워로 DRIVE_TIME_S 동안 이동 → 정지
 * </pre>
 *
 * 모든 값은 Panels에서 바꿀 수 있다. 연습장에서 몇 번 돌려 보고 시간·파워를 맞출 것.
 * Panels에서 바꾼 값은 앱을 다시 켜면 사라지므로, 맞춘 값은 반드시 이 파일의 숫자로 옮겨 적을 것.
 * 배터리 전압에 따라 가는 거리가 조금씩 달라진다 → 충전된 배터리로 맞추고, 경기에도 충전된 배터리를 쓸 것.
 */
@Configurable
@Autonomous(name = "Park Auto (시간)", group = "Competition")
public class BackupAuto extends LinearOpMode {
    /** true 면 시작하자마자 미리 실은 공을 쏜다. 지금은 주차만 하기로 해서 false. */
    public static boolean SHOOT_PRELOAD = false;
    /** 슈터가 속도에 못 도달해도 이 시간(초)이 지나면 그냥 쏜다. */
    public static double SPINUP_TIMEOUT_S = 3.0;
    /** 급탄 시간(초). 공 전부 나갈 만큼. */
    public static double FEED_TIME_S = 3.0;
    /** 이동 시작 전 대기(초). */
    public static double START_DELAY_S = 0.0;
    /** 앞(+)/뒤(-) 파워. */
    public static double DRIVE_FORWARD = 0.4;
    /** 오른쪽(+)/왼쪽(-) 파워. 진영에 따라 부호를 바꿀 것. */
    public static double DRIVE_STRAFE = 0.0;
    /** 이동 시간(초). */
    public static double DRIVE_TIME_S = 1.0;

    @Override
    public void runOpMode() {
        for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }
        Drive drive = new Drive(hardwareMap);
        drive.setFieldCentric(false);
        Shooter shooter = new Shooter(hardwareMap);
        Intake intake = new Intake(hardwareMap);
        Turret turret = new Turret(hardwareMap);
        Wedge wedge = new Wedge(hardwareMap);

        // INIT: 시작 자세 (터렛 90° → 쐐기 최대). 오토 동안 그대로 유지하고, TeleOp START 때 풀린다.
        StartPose pose = new StartPose(turret, wedge);
        pose.begin();
        while (opModeInInit()) {
            pose.initLoop();
            telemetry.addData("시작 자세", pose.status());
            telemetry.addData("발사", SHOOT_PRELOAD ? "함 (급탄 %.1f초)" : "안 함", FEED_TIME_S);
            telemetry.addData("이동", "앞 %.2f  옆 %.2f  %.1f초 (대기 %.1f초)", DRIVE_FORWARD, DRIVE_STRAFE, DRIVE_TIME_S, START_DELAY_S);
            telemetry.addLine("값은 Panels에서 바꿀 수 있음.");
            telemetry.update();
        }
        waitForStart();
        if (isStopRequested()) return;

        turret.stop();
        intake.closeBlocker();
        ElapsedTime timer = new ElapsedTime();

        // 1) 미리 실은 공 발사
        if (SHOOT_PRELOAD) {
            shooter.setMode(Shooter.Mode.SHOOT);
            timer.reset();
            while (opModeIsActive() && !shooter.atShootSpeed() && timer.seconds() < SPINUP_TIMEOUT_S) {
                shooter.update();
                status("슈터 가속", shooter);
            }
            intake.openBlocker();
            intake.feed();
            timer.reset();
            while (opModeIsActive() && timer.seconds() < FEED_TIME_S) {
                shooter.update();
                status("발사 중", shooter);
            }
            intake.stop();
            intake.closeBlocker();
            shooter.stop();
        }

        // 2) 대기
        timer.reset();
        while (opModeIsActive() && timer.seconds() < START_DELAY_S) {
            status("대기", shooter);
        }

        // 3) 이동
        timer.reset();
        while (opModeIsActive() && timer.seconds() < DRIVE_TIME_S) {
            drive.drive(DRIVE_FORWARD, DRIVE_STRAFE, 0, false);
            status("이동", shooter);
        }
        drive.stop();

        while (opModeIsActive()) {
            status("끝", shooter);
        }
    }

    private void status(String step, Shooter shooter) {
        telemetry.addData("단계", step);
        telemetry.addData("슈터", "%.0f / %.0f", shooter.velocity(), shooter.targetVelocity());
        telemetry.update();
    }
}
