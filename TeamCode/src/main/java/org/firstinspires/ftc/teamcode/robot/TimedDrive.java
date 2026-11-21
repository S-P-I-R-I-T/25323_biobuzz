package org.firstinspires.ftc.teamcode.robot;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * 위치 센서 없이 "시간 × 파워"로 정해진 거리만큼 가는 오토용 주행 (Pinpoint 고장 대비, 11/21).
 * - 거리(inch) → 시간 = 거리 ÷ 실측 속도(FORWARD_IPS / STRAFE_IPS). 속도는 Auto Drive Calibration 으로 잰다.
 * - 허브 IMU로 방향을 시작 방향에 고정한다 (옆으로 밀리거나 비틀어지는 것 방지).
 * - 출발·정지는 슬루 레이트 리미터로 부드럽게 (로봇 7.3kg, 미끄러짐 방지). 보정 때도 같은 방식이라 거리 오차가 상쇄된다.
 */
@Configurable
public class TimedDrive {
    /**
     * 오토 이동 파워 (0~1). 이 파워로 잰 속도가 아래 IPS 값이다 — 파워를 바꾸면 다시 잴 것.
     * 11/21: 너무 빠르다고 해서 0.5 → 0.4 (80%).
     */
    public static double MOVE_POWER = 0.4;
    /**
     * MOVE_POWER 로 앞으로 갈 때 실제 속도 (inch/s).
     * 11/21 실측 (파워 0.5): 2초에 202·204·203 cm → 79.9 in ÷ 2 = 40.0.
     * 파워 0.4 값은 0.8배로 추정한 32.0 — TODO(25323): Auto Drive Calibration 으로 다시 잴 것 (실제로는 조금 더 느릴 수 있음).
     */
    public static double FORWARD_IPS = 32.0;
    /**
     * MOVE_POWER 로 옆(왼쪽)으로 갈 때 실제 속도 (inch/s).
     * 11/21 실측 (파워 0.5): 2초에 155.5·153.7·154.9 cm → 60.9 in ÷ 2 = 30.5.
     * 파워 0.4 값은 0.8배로 추정한 24.4 — TODO(25323): 다시 잴 것.
     */
    public static double STRAFE_IPS = 24.4;
    /** 방향 유지: 각도 오차 1°당 회전 파워. */
    public static double HEADING_KP = 0.02;
    /** 방향 유지 최대 회전 파워. */
    public static double HEADING_MAX_TURN = 0.3;
    /** 출발·정지 부드럽게: 1초에 바뀌는 파워 양. */
    public static double RAMP_PER_S = 2.5;
    /** 한 구간이 끝나고 다음 구간 전에 멈춰 있는 시간 (초). */
    public static double PAUSE_S = 0.3;

    private final Drive drive;
    private final LinearOpMode op;
    private final double holdHeadingDeg;
    private final SlewRateLimiter fSlew = new SlewRateLimiter();
    private final SlewRateLimiter sSlew = new SlewRateLimiter();

    /** 만든 순간의 로봇 방향을 계속 유지한다 (보통 START 직후에 만든다). */
    public TimedDrive(Drive drive, LinearOpMode op) {
        this.drive = drive;
        this.op = op;
        drive.setFieldCentric(false);
        this.holdHeadingDeg = drive.headingDeg();
    }

    /** 앞(+)/뒤(−)로 inches 만큼. */
    public void forward(double inches) { move(Math.signum(inches), 0, Math.abs(inches) / FORWARD_IPS); }

    /** 오른쪽(+)/왼쪽(−)으로 inches 만큼. */
    public void strafe(double inches) { move(0, Math.signum(inches), Math.abs(inches) / STRAFE_IPS); }

    /**
     * 정해진 방향(로봇 기준, 각 −1/0/1)으로 seconds 동안 MOVE_POWER 로 간 뒤 부드럽게 멈춘다.
     * 감속 구간은 가속 구간과 같은 길이라 거리 = 속도 × 시간에 가깝다.
     */
    public void move(double fwdDir, double strafeDir, double seconds) {
        ElapsedTime t = new ElapsedTime();
        fSlew.reset(0);
        sSlew.reset(0);
        while (op.opModeIsActive() && t.seconds() < seconds) {
            step(fwdDir * MOVE_POWER, strafeDir * MOVE_POWER);
        }
        // 정지: 0이 될 때까지 감속
        while (op.opModeIsActive() && (Math.abs(fSlew.value()) > 1e-3 || Math.abs(sSlew.value()) > 1e-3)) {
            step(0, 0);
        }
        drive.stop();
        pause(PAUSE_S);
    }

    public void pause(double seconds) {
        ElapsedTime t = new ElapsedTime();
        while (op.opModeIsActive() && t.seconds() < seconds) {
            drive.drive(0, 0, headingTurn(), false);
            op.idle();
        }
        drive.stop();
    }

    private void step(double fwd, double str) {
        double f = fSlew.calculate(fwd, RAMP_PER_S, RAMP_PER_S);
        double s = sSlew.calculate(str, RAMP_PER_S, RAMP_PER_S);
        drive.drive(f, s, headingTurn(), false);
        op.telemetry.addData("오토 주행", "앞 %.2f  옆 %.2f  방향 %.1f° (유지 %.1f°)", f, s, drive.headingDeg(), holdHeadingDeg);
        op.telemetry.update();
    }

    /** 시작 방향에서 벗어난 만큼 되돌리는 회전 파워 (+ = 시계). */
    private double headingTurn() {
        double error = Math.IEEEremainder(holdHeadingDeg - drive.headingDeg(), 360);   // + = 반시계로 가야 함
        double turn = -HEADING_KP * error;
        return Math.max(-HEADING_MAX_TURN, Math.min(HEADING_MAX_TURN, turn));
    }
}
