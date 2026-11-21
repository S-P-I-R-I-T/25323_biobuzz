package org.firstinspires.ftc.teamcode.opmode;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.robot.Drive;
import org.firstinspires.ftc.teamcode.robot.Intake;
import org.firstinspires.ftc.teamcode.robot.StartPose;
import org.firstinspires.ftc.teamcode.robot.TimedDrive;
import org.firstinspires.ftc.teamcode.robot.Turret;
import org.firstinspires.ftc.teamcode.robot.Wedge;

/**
 * 백업 오토 (RED / BLUE 공용): 발사 없이 LOADING ZONE 옆 구석에서 시작해 앞으로 조금 가서 주차만 한다.
 * Shoot + Park Auto 가 안 될 때(조준이 안 맞음, 같은 편과 자리 겹침 등) 쓴다.
 *
 * 동선은 ftc-tactic-sim 경기 파일 tacticsim-match_20261121-1353_RED_15pts 의 R2 를 재생해 뽑은 것:
 *   RED 시작 (12, 9) 정면 = 필드 +y → 앞으로 8 in → (12, 17) 에서 LOADING ZONE(y 24~47)에 2 in 걸쳐 PARK.
 *   2 in 는 시간 × 파워 오차에 너무 빠듯해서, 기본값은 앞으로 24 in (로봇 중심 y 33, ZONE 가운데 근처).
 *   로봇 중심이 시작점에서 6 in ~ 47 in 사이로 가면 PARK 이 된다. 시작 때 벽에서 떨어지므로 LEAVE 도 받는다 (3 + 5 = 8점).
 * 놓는 법: 뒤 = 우리 GARDEN 반대편 벽에 붙임, 오른쪽 = 우리 LOADING ZONE 벽 (3 in 정도 띄움, 로봇 중심 ~ 벽 12 in).
 * BLUE 는 RED 의 점대칭이라 로봇 기준 동작(앞으로)이 같다.
 * INIT 시작 자세(터렛 90°, 쐐기 최대)는 오토 내내 그대로 둔다 — 터렛을 돌리지 않으므로 안전.
 */
@Configurable
@Autonomous(name = "Park Only Auto (로딩존 옆, RED/BLUE)", group = "Competition")
public class BackupAuto extends LinearOpMode {
    /** 주행 전 대기 (초). 같은 편 로봇과 부딪히지 않게. */
    public static double START_DELAY_S = 0.0;
    /** 앞(+) 거리 (inch). 6 in 이상이어야 ZONE 에 걸치고, 47 in 넘게 가면 ZONE 을 지나친다. */
    public static double FORWARD_IN = 24;
    /** 옆 거리 (inch, 오른쪽 + / 왼쪽 −). 보통 0. */
    public static double STRAFE_IN = 0;

    @Override
    public void runOpMode() {
        for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }
        Drive drive = new Drive(hardwareMap);
        Intake intake = new Intake(hardwareMap);
        Turret turret = new Turret(hardwareMap);
        Wedge wedge = new Wedge(hardwareMap);

        // INIT: 시작 자세 (쐐기 내림 → 터렛 90° → 쐐기 최대). 오토 동안 그대로 유지하고, TeleOp START 때 풀린다.
        StartPose pose = new StartPose(turret, wedge);
        pose.begin();
        while (opModeInInit()) {
            pose.initLoop();
            telemetry.addData("시작 자세", pose.status());
            telemetry.addData("자세 값", StartPose.values());
            telemetry.addLine("놓는 법: 뒤 = 우리 GARDEN 반대편 벽, 오른쪽 = 우리 LOADING ZONE 벽에서 3 in 띄움.");
            telemetry.addData("주행", "대기 %.1f초 → 앞 %.0f in, 옆 %.0f in  (속도 앞 %.0f / 옆 %.0f in/s)",
                    START_DELAY_S, FORWARD_IN, STRAFE_IN, TimedDrive.FORWARD_IPS, TimedDrive.STRAFE_IPS);
            telemetry.update();
        }
        waitForStart();
        if (isStopRequested()) return;

        drive.resetHeading();
        TimedDrive timed = new TimedDrive(drive, this);
        intake.closeBlocker();

        timed.pause(START_DELAY_S);
        if (FORWARD_IN != 0) timed.forward(FORWARD_IN);
        if (STRAFE_IN != 0) timed.strafe(STRAFE_IN);
        drive.stop();

        while (opModeIsActive()) {
            telemetry.addData("단계", "끝 (주차)");
            telemetry.update();
        }
    }
}
