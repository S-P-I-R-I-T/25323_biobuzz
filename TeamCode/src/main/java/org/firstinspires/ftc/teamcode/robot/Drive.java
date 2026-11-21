package org.firstinspires.ftc.teamcode.robot;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/**
 * 메카넘 필드 기준 주행 (허브 내장 IMU 사용).
 * 모터 방향은 Constants.driveConfig 한 곳에서 가져온다 (Mecanum Tuner 결과를 거기에만 넣으면 됨).
 */
@Configurable
public class Drive {
    /**
     * 허브 로고가 향하는 방향. 25323은 Control Hub를 로봇 왼쪽 옆면에 세워서 달았고,
     * 로고 면이 바깥(왼쪽)을 본다 (2026-11-21 사진 확인).
     * 확인: Hardware Check에서 평평할 때 pitch·roll ≈ 0, 반시계로 돌리면 yaw 증가.
     */
    public static RevHubOrientationOnRobot.LogoFacingDirection HUB_LOGO = RevHubOrientationOnRobot.LogoFacingDirection.LEFT;
    /** 허브 USB 포트가 향하는 방향. 25323은 USB 쪽 끝이 로봇 뒤를 본다. */
    public static RevHubOrientationOnRobot.UsbFacingDirection HUB_USB = RevHubOrientationOnRobot.UsbFacingDirection.BACKWARD;

    /** 옆으로 갈 때 덜 가는 것 보정 (메카넘 특성). 1.0 = 보정 없음. */
    public static double STRAFE_CORRECTION = 1.1;
    /** TeleOp 전체 속도 배율 (이동·회전 모두). 11/21: 너무 빠르다고 해서 1.0 → 0.8. */
    public static double MAX_SCALE = 0.7;
    /** 느린 모드 속도 배율 (0~1). MAX_SCALE 에 한 번 더 곱해진다 (0.8 × 0.4 = 0.32). */
    public static double SLOW_SCALE = 0.714285;
    /** 스틱이 이 값보다 작게 움직이면 0으로 본다. */
    public static double STICK_DEADBAND = 0.05;

    // ── 슬루 레이트 리미터 (로봇 약 7.3kg: 급출발·급정거로 미끄러지거나 들리는 것 방지) ──
    /** 이동: 빨라질 때 1초에 바뀌는 양. 3 = 정지 → 최고속까지 약 0.33초. 크면 민첩, 작으면 부드러움. */
    public static double MOVE_ACCEL = 3.0;
    /** 이동: 느려질 때. 멈출 때는 조금 더 빨리 반응하도록 크게. 6 = 최고속 → 정지 약 0.17초. */
    public static double MOVE_DECEL = 6.0;
    /** 회전: 빨라질 때 / 느려질 때. */
    public static double TURN_ACCEL = 4.0, TURN_DECEL = 8.0;

    // ── 회전 조작 ──
    /** D-pad 좌/우 제자리 회전 파워 (0~1). */
    public static double DPAD_TURN_POWER = 0.6;
    /** 오른쪽 스틱을 이만큼 이상 밀어야 "그 방향을 바라봐라"로 본다 (가운데 근처 떨림 무시). */
    public static double AIM_STICK_MIN = 0.6;
    /** 방향 맞추기: 각도 오차 1°당 회전 파워. 0.02 = 50° 이상 차이나면 최대 파워. */
    public static double AIM_KP = 0.02;
    /** 방향 맞추기 최대 회전 파워. */
    public static double AIM_MAX_TURN = 0.7;
    /** 방향 맞추기: 이 오차(°) 안이면 도착으로 본다. */
    public static double AIM_TOLERANCE_DEG = 2.0;
    /** 방향 맞추기 최소 회전 파워 (오차가 작아도 바닥 마찰을 이기고 끝까지 돌도록). */
    public static double AIM_MIN_TURN = 0.06;

    private final DcMotor fl, fr, rl, rr;
    private final IMU imu;
    private boolean fieldCentric = true;
    private final SlewRateLimiter forwardSlew = new SlewRateLimiter();
    private final SlewRateLimiter strafeSlew = new SlewRateLimiter();
    private final SlewRateLimiter turnSlew = new SlewRateLimiter();
    private double aimTargetDeg = Double.NaN;

    public Drive(HardwareMap hardwareMap) {
        fl = hardwareMap.get(DcMotor.class, HardwareNames.FRONT_LEFT);
        fr = hardwareMap.get(DcMotor.class, HardwareNames.FRONT_RIGHT);
        rl = hardwareMap.get(DcMotor.class, HardwareNames.BACK_LEFT);
        rr = hardwareMap.get(DcMotor.class, HardwareNames.BACK_RIGHT);

        fl.setDirection(Constants.driveConfig.frontLeftDirection.get());
        fr.setDirection(Constants.driveConfig.frontRightDirection.get());
        rl.setDirection(Constants.driveConfig.backLeftDirection.get());
        rr.setDirection(Constants.driveConfig.backRightDirection.get());

        for (DcMotor m : new DcMotor[]{fl, fr, rl, rr}) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        imu = hardwareMap.get(IMU.class, HardwareNames.IMU);
        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(HUB_LOGO, HUB_USB)));
        imu.resetYaw();
    }

    /** 지금 로봇이 보는 방향을 "앞"으로 정한다 (드라이버 쪽에서 로봇을 경기장 안쪽으로 향하게 놓고 누를 것). */
    public void resetHeading() { imu.resetYaw(); }

    public void setFieldCentric(boolean on) { fieldCentric = on; }
    public boolean isFieldCentric() { return fieldCentric; }

    /** 로봇 방향 (도, 반시계 = +). */
    public double headingDeg() { return imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES); }

    /**
     * @param forward 앞(+)/뒤(-), 보통 -gamepad.left_stick_y
     * @param strafe  오른쪽(+)/왼쪽(-), 보통 gamepad.left_stick_x
     * @param turn    시계(+)/반시계(-), 보통 gamepad.right_stick_x
     * @param slow    true면 SLOW_SCALE 배로 느리게
     */
    public void drive(double forward, double strafe, double turn, boolean slow) {
        forward = deadband(forward);
        strafe = deadband(strafe);
        turn = deadband(turn);

        double x = strafe, y = forward;
        if (fieldCentric) {
            double h = Math.toRadians(headingDeg());
            // 경기장 기준 입력을 로봇 기준으로 돌린다
            x = strafe * Math.cos(-h) - forward * Math.sin(-h);
            y = strafe * Math.sin(-h) + forward * Math.cos(-h);
        }
        x *= STRAFE_CORRECTION;

        double denom = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(turn), 1);
        double scale = slow ? SLOW_SCALE : 1.0;
        fl.setPower((y + x + turn) / denom * scale);
        rl.setPower((y - x + turn) / denom * scale);
        fr.setPower((y - x - turn) / denom * scale);
        rr.setPower((y + x - turn) / denom * scale);
    }

    public void stop() {
        forwardSlew.reset(0);
        strafeSlew.reset(0);
        turnSlew.reset(0);
        drive(0, 0, 0, false);
    }

    /**
     * TeleOp 주행 (드라이버 gamepad1).
     * - 왼쪽 스틱: 이동 (필드 기준이면 경기장 기준)
     * - 오른쪽 스틱: 그 방향을 로봇이 바라보도록 회전 ("스틱 방향 바라보기").
     *   스틱 위 = 시작할 때(또는 OPTIONS 누를 때) 로봇이 보던 쪽, 왼쪽 = 거기서 반시계 90°.
     *   스틱을 놓으면 그 자리에서 회전을 멈춘다.
     * - D-pad 좌/우: 제자리 회전 (오른쪽 스틱보다 우선)
     * 이동·회전 모두 슬루 레이트 리미터를 거친다.
     *
     * @param forward  -gamepad.left_stick_y
     * @param strafe   gamepad.left_stick_x
     * @param aimX     gamepad.right_stick_x
     * @param aimY     -gamepad.right_stick_y
     */
    public void teleOp(double forward, double strafe, double aimX, double aimY,
                       boolean dpadLeft, boolean dpadRight, boolean slow) {
        double turn;
        aimTargetDeg = Double.NaN;
        if (dpadLeft || dpadRight) {
            turn = (dpadRight ? DPAD_TURN_POWER : 0) - (dpadLeft ? DPAD_TURN_POWER : 0);   // + = 시계
        } else if (Math.hypot(aimX, aimY) >= AIM_STICK_MIN) {
            aimTargetDeg = Math.toDegrees(Math.atan2(-aimX, aimY));   // 위 = 0°, 왼쪽 = +90° (반시계 +)
            double error = wrapDeg(aimTargetDeg - headingDeg());
            if (Math.abs(error) <= AIM_TOLERANCE_DEG) turn = 0;
            else {
                turn = clamp(-AIM_KP * error, AIM_MAX_TURN);      // 오차 + (반시계로 가야 함) → turn − (반시계)
                if (Math.abs(turn) < AIM_MIN_TURN) turn = Math.signum(turn) * AIM_MIN_TURN;
            }
        } else {
            turn = 0;
        }

        double f = forwardSlew.calculate(deadband(forward), MOVE_ACCEL, MOVE_DECEL);
        double s = strafeSlew.calculate(deadband(strafe), MOVE_ACCEL, MOVE_DECEL);
        double t = turnSlew.calculate(turn, TURN_ACCEL, TURN_DECEL);
        drive(f * MAX_SCALE, s * MAX_SCALE, t * MAX_SCALE, slow);
    }

    /** 오른쪽 스틱으로 바라보려는 방향 (°). 스틱을 안 밀고 있으면 NaN. */
    public double aimTargetDeg() { return aimTargetDeg; }

    private static double wrapDeg(double d) {
        d = Math.IEEEremainder(d, 360);   // -180 ~ 180
        return d;
    }

    private static double clamp(double v, double max) { return Math.max(-max, Math.min(max, v)); }

    private static double deadband(double v) { return Math.abs(v) < STICK_DEADBAND ? 0 : v; }
}
