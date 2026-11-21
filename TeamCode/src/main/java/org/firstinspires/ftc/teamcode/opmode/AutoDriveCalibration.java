package org.firstinspires.ftc.teamcode.opmode;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.Drive;
import org.firstinspires.ftc.teamcode.robot.TimedDrive;

/**
 * 오토 주행 속도 재기 (TimedDrive.FORWARD_IPS / STRAFE_IPS).
 * START → A: 앞으로 CAL_SECONDS 동안 이동 → 줄자로 거리 측정 → FORWARD_IPS = 거리 ÷ CAL_SECONDS
 *         B: 왼쪽으로 CAL_SECONDS 동안 이동 → 거리 측정 → STRAFE_IPS = 거리 ÷ CAL_SECONDS (오토가 왼쪽으로 가므로 왼쪽으로 잰다)
 * 오토와 같은 파워·출발/정지 방식·방향 유지를 쓴다. 충전된 배터리로, 실제 경기장 매트에서 잴 것. 각각 2~3번 재서 평균.
 */
@Configurable
@TeleOp(name = "Auto Drive Calibration", group = "Test")
public class AutoDriveCalibration extends LinearOpMode {
    /** 측정용 이동 시간 (초). 2초면 약 40~60 in 이동. 공간이 부족하면 줄일 것. */
    public static double CAL_SECONDS = 2.0;

    @Override
    public void runOpMode() {
        for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }
        Drive drive = new Drive(hardwareMap);
        telemetry.addLine("앞쪽·왼쪽에 공간을 확보하고 START.");
        telemetry.update();
        waitForStart();
        drive.resetHeading();
        TimedDrive timed = new TimedDrive(drive, this);

        String last = "-";
        while (opModeIsActive()) {
            if (gamepad1.aWasPressed()) {
                timed.move(1, 0, CAL_SECONDS);
                last = "앞으로 " + CAL_SECONDS + "초 이동함";
            } else if (gamepad1.bWasPressed()) {
                timed.move(0, -1, CAL_SECONDS);
                last = "왼쪽으로 " + CAL_SECONDS + "초 이동함";
            }
            telemetry.addLine("A = 앞으로, B = 왼쪽으로 " + CAL_SECONDS + "초 (파워 " + TimedDrive.MOVE_POWER + ")");
            telemetry.addData("마지막", last);
            telemetry.addLine("잰 거리(inch) ÷ " + CAL_SECONDS + " = FORWARD_IPS / STRAFE_IPS");
            telemetry.addData("지금 값", "FORWARD_IPS %.1f  STRAFE_IPS %.1f", TimedDrive.FORWARD_IPS, TimedDrive.STRAFE_IPS);
            telemetry.update();
        }
        drive.stop();
    }
}
