package org.firstinspires.ftc.teamcode.opmode;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.teamcode.robot.Drive;
import org.firstinspires.ftc.teamcode.robot.HardwareNames;
import org.firstinspires.ftc.teamcode.robot.Intake;
import org.firstinspires.ftc.teamcode.robot.Turret;
import org.firstinspires.ftc.teamcode.robot.Wedge;

import java.util.ArrayList;
import java.util.List;

/**
 * 배선·방향·서보 위치 확인용. 장치를 하나씩 골라서 움직여 본다. 방향 반전은 적용하지 않은 "날것" 그대로다.
 *
 * <pre>
 * gamepad1
 *   D-pad 상 / 하   장치 고르기
 *   모터            A = 정방향 30%, B = 역방향 30% (누르는 동안)
 *   서보·터렛       X = 켜기(코드의 기본 위치로 이동), D-pad 좌/우 = ∓0.01, LB/RB = ∓0.001
 *                   (터렛은 Turret.POSITION_MODE = true 일 때 위치로 조작. false 면 A/B 파워)
 * 화면 아래에 IMU와 Pinpoint 값이 항상 나온다.
 * </pre>
 *
 * 5-turn 쐐기 서보는 0.01이 약 18°다. 처음 켤 때는 쐐기 링크를 빼 두고 위치를 찾은 뒤 다시 달 것.
 */
@TeleOp(name = "Hardware Check", group = "Test")
public class HardwareCheck extends LinearOpMode {
    private static final double TEST_POWER = 0.3;

    private static class Device {
        final String name, label;
        DcMotorEx motor;
        CRServo crServo;
        Servo servo;
        double servoPos;
        boolean servoOn;

        Device(String name, String label) { this.name = name; this.label = label; }
        boolean missing() { return motor == null && crServo == null && servo == null; }
    }

    @Override
    public void runOpMode() {
        List<Device> devices = new ArrayList<>();
        String[][] motors = {
                {HardwareNames.FRONT_LEFT, "구동 앞-왼쪽 (CH 모터 0)"},
                {HardwareNames.BACK_LEFT, "구동 뒤-왼쪽 (CH 모터 1)"},
                {HardwareNames.FRONT_RIGHT, "구동 앞-오른쪽 (CH 모터 2)"},
                {HardwareNames.BACK_RIGHT, "구동 뒤-오른쪽 (CH 모터 3)"},
                {HardwareNames.SHOOTER, "슈터 (EH 모터 0)"},
                {HardwareNames.INTAKE, "인테이크 (EH 모터 1)"},
        };
        for (String[] m : motors) {
            Device d = new Device(m[0], m[1]);
            d.motor = hardwareMap.tryGet(DcMotorEx.class, m[0]);
            devices.add(d);
        }
        Device turret = new Device(HardwareNames.TURRET,
                Turret.POSITION_MODE ? "터렛 Axon 위치 모드 (CH 서보 0)" : "터렛 Axon 연속 회전 (CH 서보 0)");
        turret.crServo = hardwareMap.tryGet(CRServo.class, HardwareNames.TURRET);
        if (turret.crServo != null) Turret.applyPwmRange(turret.crServo);   // TeleOp과 같은 신호 범위
        turret.servoPos = Turret.CENTER_POS;
        if (Turret.ENABLED) devices.add(turret);   // 터렛 고정 중(ENABLED=false)이면 목록에서 빼서 실수로 돌리지 않게 함
        Device blocker = new Device(HardwareNames.BLOCKER, "블로커 서보 (CH 서보 1)");
        blocker.servo = hardwareMap.tryGet(Servo.class, HardwareNames.BLOCKER);
        blocker.servoPos = Intake.BLOCKER_CLOSED;
        devices.add(blocker);
        Device wedge = new Device(HardwareNames.WEDGE, "쐐기 5-turn 서보 (CH 서보 2)");
        wedge.servo = hardwareMap.tryGet(Servo.class, HardwareNames.WEDGE);
        wedge.servoPos = Wedge.SHOOT;
        devices.add(wedge);

        IMU imu = hardwareMap.tryGet(IMU.class, HardwareNames.IMU);
        if (imu != null) {
            imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(Drive.HUB_LOGO, Drive.HUB_USB)));
            imu.resetYaw();
        }
        GoBildaPinpointDriver pinpoint = hardwareMap.tryGet(GoBildaPinpointDriver.class, HardwareNames.PINPOINT);
        if (pinpoint != null) pinpoint.resetPosAndIMU();

        telemetry.addLine("Hardware Check 준비. START 후 D-pad 상/하로 장치 선택.");
        telemetry.update();
        waitForStart();

        int selected = 0;
        while (opModeIsActive()) {
            int prev = selected;
            if (gamepad1.dpadUpWasPressed()) selected = (selected + devices.size() - 1) % devices.size();
            if (gamepad1.dpadDownWasPressed()) selected = (selected + 1) % devices.size();
            if (prev != selected) stopMoving(devices.get(prev));

            Device d = devices.get(selected);
            boolean positional = d.servo != null || (d.crServo != null && Turret.POSITION_MODE);
            if (!positional && (d.motor != null || d.crServo != null)) {
                double power = gamepad1.a ? TEST_POWER : gamepad1.b ? -TEST_POWER : 0;
                if (d.motor != null) d.motor.setPower(power);
                if (d.crServo != null) d.crServo.setPower(power);
            } else if (positional) {
                if (gamepad1.xWasPressed()) d.servoOn = true;
                if (gamepad1.dpadLeftWasPressed()) d.servoPos -= 0.01;
                if (gamepad1.dpadRightWasPressed()) d.servoPos += 0.01;
                if (gamepad1.leftBumperWasPressed()) d.servoPos -= 0.001;
                if (gamepad1.rightBumperWasPressed()) d.servoPos += 0.001;
                d.servoPos = Math.max(0, Math.min(1, d.servoPos));
                if (d.servoOn) {
                    if (d.servo != null) d.servo.setPosition(d.servoPos);
                    else d.crServo.setPower(2 * d.servoPos - 1);   // Axon 위치 모드: 파워 -1~1 = 위치 0~1
                }
            }

            for (int i = 0; i < devices.size(); i++) {
                Device x = devices.get(i);
                String mark = i == selected ? "▶ " : "   ";
                String info;
                if (x.missing()) info = "없음 (Configuration에 \"" + x.name + "\" 등록 확인)";
                else if (x.motor != null) info = String.format("엔코더 %d  속도 %.0f", x.motor.getCurrentPosition(), x.motor.getVelocity());
                else if (x.crServo != null && !Turret.POSITION_MODE) info = String.format("파워 %.2f", x.crServo.getPower());
                else info = String.format("위치 %.3f  %s", x.servoPos, x.servoOn ? "" : "(X로 켜기)");
                telemetry.addData(mark + x.name, "%s | %s", x.label, info);
            }

            telemetry.addLine();
            if (imu != null) {
                YawPitchRollAngles a = imu.getRobotYawPitchRollAngles();
                telemetry.addData("IMU", "yaw %.1f  pitch %.1f  roll %.1f", a.getYaw(AngleUnit.DEGREES),
                        a.getPitch(AngleUnit.DEGREES), a.getRoll(AngleUnit.DEGREES));
                telemetry.addLine("  확인: 로봇을 반시계로 돌리면 yaw 증가, 평평하면 pitch·roll ≈ 0");
            } else {
                telemetry.addData("IMU", "없음");
            }
            if (pinpoint != null) {
                pinpoint.update();
                telemetry.addData("Pinpoint", "x %.1f in  y %.1f in  heading %.1f°  (엔코더 X %d, Y %d)",
                        pinpoint.getPosX(DistanceUnit.INCH), pinpoint.getPosY(DistanceUnit.INCH),
                        pinpoint.getHeading(AngleUnit.DEGREES), pinpoint.getEncoderX(), pinpoint.getEncoderY());
            } else {
                telemetry.addData("Pinpoint", "없음 (Configuration에 \"" + HardwareNames.PINPOINT + "\" 등록 확인)");
            }
            telemetry.update();
        }
        for (Device d : devices) stopMoving(d);
    }

    private static void stopMoving(Device d) {
        if (d.motor != null) d.motor.setPower(0);
        // 위치 모드 터렛은 파워 0 = "가운데 위치로 가라"라서 보내지 않는다 (그 자리를 지킴).
        if (d.crServo != null && !Turret.POSITION_MODE) d.crServo.setPower(0);
    }
}
