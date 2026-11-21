package org.firstinspires.ftc.teamcode.robot;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.VoltageSensor;

/**
 * 플라이휠 슈터 (goBILDA 6000rpm, 엔코더 28 ticks/회전 → 최고 약 2800 ticks/s).
 * 속도 제어(PIDF)로 목표 속도를 유지하고, 배터리 전압이 떨어지면 F를 자동으로 올린다.
 * PIDF는 Shooter PIDF Tuner 결과 (11/21). 발사 속도는 실제 발사로 맞출 것.
 */
@Configurable
public class Shooter {
    public enum Mode { OFF, IDLE, SHOOT }

    /** 앞으로 쏠 때 Hardware Check에서 속도가 음수로 나오면 true. 25323: B(−)가 쏘는 방향, 속도 음수 → true (11/21). */
    public static boolean REVERSED = true;
    /** 발사 속도 (ticks/s). 이 로봇의 12V 최고속은 약 2210 (튜너 측정) → 1900 이하 권장. */
    public static double SHOOT_VELOCITY = 1350;   // 11/21 실제 발사로 찾은 최적값 (공 3개 모두 들어감)
    /** 평소 예열 속도 (ticks/s). 발사 버튼을 누르면 SHOOT_VELOCITY까지 빨리 올라간다. */
    public static double IDLE_VELOCITY = 650;     // 11/21: 1350을 찾을 때 쓰던 값으로 되돌림 (900에서 더 세게 느껴짐)
    /** 목표 속도에서 이만큼 모자라도 "준비됨"으로 본다 (ticks/s). */
    public static double READY_TOLERANCE = 50;

    // ── 발사 순서 (TeleOp RT) ──
    /** 블로커를 연 뒤 이 시간(초)만큼 기다렸다가 급탄 시작 (블로커가 다 열리기 전에 공을 밀면 걸리거나 튐). */
    public static double OPEN_DELAY_S = 0.15;
    /** true: 슈터가 목표 속도 근처일 때만 급탄하고, 한 발 쏘고 속도가 떨어지면 회복될 때까지 급탄을 멈춘다. */
    public static boolean FEED_GATING = true;
    /** 급탄 허용: 목표 속도에서 이만큼 이내일 때만 급탄 (ticks/s). 작을수록 공 간격이 벌어지고 일정해짐. */
    public static double FEED_GATE_TOLERANCE = 75;

    /**
     * 속도 PIDF. F는 12V 기준 값 (배터리 전압에 맞춰 자동 보정됨).
     * 2026-11-21 Shooter PIDF Tuner 결과 (목표 1500): 12V 최고속 2210 ticks/s, 0→목표 0.89초,
     * 60%→목표 회복 0.52초, 오버슈트 1.9%, 정상상태 오차 8 ticks/s.
     * P는 튜너 상한(256)에 닿았지만 회복이 모터 최대 파워에서 이미 포화라 더 키워도 빨라지지 않는다.
     */
    public static double P = 256, I = 0, D = 0, F = 15.1386;

    private static final long VOLTAGE_REFRESH_NS = 500_000_000L;

    private final DcMotorEx motor;
    private final HardwareMap hardwareMap;
    private Mode mode = Mode.OFF;
    private long nextVoltageNs;
    private double voltage = 12;
    private double appliedP = Double.NaN, appliedI = Double.NaN, appliedD = Double.NaN, appliedF = Double.NaN;

    public Shooter(HardwareMap hardwareMap) {
        this.hardwareMap = hardwareMap;
        motor = hardwareMap.get(DcMotorEx.class, HardwareNames.SHOOTER);
        motor.setDirection(REVERSED ? DcMotorSimple.Direction.REVERSE : DcMotorSimple.Direction.FORWARD);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);   // 멈출 때 플라이휠이 천천히 서도록
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motor.setVelocity(0);
    }

    public void setMode(Mode mode) { this.mode = mode; }
    public Mode mode() { return mode; }

    /** 매 루프 호출. */
    public void update() {
        refreshPidf();
        motor.setVelocity(targetVelocity());
    }

    public double targetVelocity() {
        switch (mode) {
            case SHOOT: return SHOOT_VELOCITY;
            case IDLE: return IDLE_VELOCITY;
            default: return 0;
        }
    }

    public double velocity() { return motor.getVelocity(); }

    /** 발사 속도에 도달했는지. */
    public boolean atShootSpeed() { return velocity() >= SHOOT_VELOCITY - READY_TOLERANCE; }

    public void stop() {
        mode = Mode.OFF;
        motor.setVelocity(0);
    }

    private void refreshPidf() {
        long now = System.nanoTime();
        if (now >= nextVoltageNs) {
            double lowest = Double.POSITIVE_INFINITY;
            for (VoltageSensor s : hardwareMap.voltageSensor) {
                double v = s.getVoltage();
                if (v > 0 && v < lowest) lowest = v;
            }
            if (!Double.isInfinite(lowest)) voltage = lowest;
            nextVoltageNs = now + VOLTAGE_REFRESH_NS;
        }
        double f = F * (12.0 / voltage);
        if (P != appliedP || I != appliedI || D != appliedD || Math.abs(f - appliedF) >= 0.01) {
            motor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, new PIDFCoefficients(P, I, D, f));
            appliedP = P; appliedI = I; appliedD = D; appliedF = f;
        }
    }
}
