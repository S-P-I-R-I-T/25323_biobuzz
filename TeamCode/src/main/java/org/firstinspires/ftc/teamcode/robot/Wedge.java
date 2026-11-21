package org.firstinspires.ftc.teamcode.robot;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * 플라워에서 공을 빼오는 쐐기 (goBILDA 2000-0025-0502, 5-turn 서보).
 * 5-turn 서보는 0~1이 5바퀴(약 1800°)라서 0.01만 바꿔도 약 18° 움직인다. 값을 조금씩 바꿀 것.
 *
 * 상태 3가지 (2026-11-21 Hardware Check 측정):
 *   EXTEND = 내민 위치 (플라워에서 공을 빼올 때)          0.519 (플라워 단차 때문에 0.512에서 올림)
 *   SHOOT  = 낮게 접은 위치 (슈터로 발사할 때)            0.577
 *   STOW   = 높게 접은 위치 (평소 이동할 때, 충돌·기동성)  0.595 (터렛이 가운데일 때 가장 위)
 *   INIT   = 시작 자세 (인스펙션 크기 규정)                  0.618 (터렛을 90° 돌렸을 때만 가능한 최대 위치)
 *            ⚠ 터렛이 가운데일 때 INIT 위치로 가면 부딪힐 수 있다. 반드시 터렛을 먼저 돌린 뒤에 보낼 것.
 * 숫자가 커질수록 접히는 방향이다 (EXTEND 0.519 → SHOOT 0.577 → STOW 0.595 → INIT 0.618).
 */
@Configurable
public class Wedge {
    public enum State { EXTEND, SHOOT, STOW, INIT }

    public static double EXTEND = 0.519;   // 11/21: 플라워 단차 때문에 0.512 → 0.519
    public static double SHOOT = 0.577;
    public static double STOW = 0.595;
    public static double INIT = 0.618;

    private final Servo servo;
    private State state;

    public Wedge(HardwareMap hardwareMap) {
        servo = hardwareMap.get(Servo.class, HardwareNames.WEDGE);
    }

    /** 위치를 직접 보낸다 (Start Pose Check 에서 값 찾을 때만). */
    public void setRaw(double pos) { servo.setPosition(pos); }

    public void set(State s) {
        state = s;
        servo.setPosition(positionOf(s));
    }

    /** 한 단계 더 내밀기: (INIT/)STOW → SHOOT → EXTEND. */
    public void stepOut() {
        if (state == null || state == State.STOW || state == State.INIT) set(State.SHOOT);
        else set(State.EXTEND);
    }

    /** 한 단계 더 접기: EXTEND → SHOOT → STOW. */
    public void stepIn() {
        if (state == null || state == State.EXTEND) set(State.SHOOT);
        else set(State.STOW);
    }

    /** 아직 한 번도 명령을 안 보냈으면 null. */
    public State state() { return state; }

    public static double positionOf(State s) {
        switch (s) {
            case EXTEND: return EXTEND;
            case SHOOT: return SHOOT;
            case INIT: return INIT;
            default: return STOW;
        }
    }

    public static String label(State s) {
        if (s == null) return "-";
        switch (s) {
            case EXTEND: return "내밂(빼오기)";
            case SHOOT: return "낮게 접음(발사)";
            case INIT: return "시작 자세";
            default: return "높게 접음(이동)";
        }
    }
}
