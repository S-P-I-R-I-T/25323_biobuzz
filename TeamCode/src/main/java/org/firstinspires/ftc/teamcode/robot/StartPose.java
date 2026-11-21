package org.firstinspires.ftc.teamcode.robot;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * 시작 자세 (인스펙션 크기 규정).
 *
 * ▶ 지금 (2026-11-21~, Turret.ENABLED = false): 터렛은 정면(인테이크 방향)에 기계적으로 고정.
 *   INIT  : 쐐기만 최대(Wedge.INIT 0.618)로 올린다 → WEDGE_SETTLE_S 뒤 "③ 시작 자세 완료".
 *   START : 쐐기를 STOW(0.595)로 내린다. 터렛 명령은 없음.
 *   인스펙션은 "터렛 정면 + 쐐기 0.618" 로 통과함.
 *
 * ▼ 터렛을 다시 쓰게 되면 (Turret.ENABLED = true): 터렛을 반시계 90°(Turret.INIT_POS) 돌리고 쐐기를 최대로 올린 상태.
 *
 * ⚠ 규칙: 쐐기가 올라가 있는 동안(STOW보다 위)에는 터렛을 돌리면 안 된다 (11/21 확인).
 *   그래서 터렛이 움직이기 전에는 항상 쐐기를 STOW(0.595)로 먼저 내린다.
 *
 *   INIT  : ① 쐐기 STOW 로 내림 → WEDGE_DOWN_DELAY_S 뒤 ② 터렛을 INIT_POS 로 → WEDGE_UP_DELAY_S 뒤 ③ 쐐기 INIT 로 올림
 *   START : ① 쐐기 STOW 로 내림 → TURRET_RETURN_DELAY_S 뒤 ② 터렛을 가운데로
 * Turret.INIT_POS 가 음수면 아무것도 하지 않는다 (예전처럼 START 때 쐐기 STOW, 터렛 가운데).
 */
@Configurable
public class StartPose {
    /** INIT: 쐐기를 내리기 시작하고 이 시간(초) 뒤에 터렛을 돌린다. */
    public static double WEDGE_DOWN_DELAY_S = 0.5;
    /** INIT: 터렛을 돌리기 시작하고 이 시간(초) 뒤에 쐐기를 올린다. 터렛이 다 돌기 전에 쐐기가 올라가면 부딪힘. */
    public static double WEDGE_UP_DELAY_S = 2.0;   // 11/21: 1.0초로는 터렛이 다 못 돌아서 늘림
    /** START: 쐐기를 내리기 시작하고 이 시간(초) 뒤에 터렛을 가운데로 돌린다. */
    public static double TURRET_RETURN_DELAY_S = 0.5;
    /** 터렛 고정일 때 INIT: 쐐기를 올리고 이 시간(초) 뒤에 "완료"로 표시 (5-turn 서보가 다 올라갈 시간). */
    public static double WEDGE_SETTLE_S = 1.0;

    private enum Phase { OFF, WEDGE_DOWN, TURRET_MOVING, WEDGE_ONLY, DONE }

    // 코드에 적힌 값 (로봇 앱이 켜질 때 읽어 둠). Panels 에서 값을 바꾸면 앱을 다시 켤 때까지 그 값이 남아 있으므로 비교용.
    private static final double CODE_TURRET_INIT = Turret.INIT_POS, CODE_TURRET_CENTER = Turret.CENTER_POS;
    private static final double CODE_WEDGE_INIT = Wedge.INIT, CODE_WEDGE_STOW = Wedge.STOW;

    private final Turret turret;
    private final Wedge wedge;
    private final ElapsedTime timer = new ElapsedTime();
    private Phase phase = Phase.OFF;
    private boolean active, releasing;

    public StartPose(Turret turret, Wedge wedge) {
        this.turret = turret;
        this.wedge = wedge;
    }

    /** INIT 직후 한 번 호출. 시작 자세 값이 없으면 아무것도 안 한다. */
    public void begin() {
        if (!Turret.ENABLED) {
            // 터렛 고정: 쐐기만 최대로. 터렛이 안 돌아가므로 순서 걱정 없음.
            active = true;
            wedge.set(Wedge.State.INIT);
            phase = Phase.WEDGE_ONLY;
            timer.reset();
            return;
        }
        active = Turret.initPoseSet();
        if (!active) {
            phase = Phase.OFF;
            return;
        }
        wedge.set(Wedge.State.STOW);       // ① 터렛을 돌리기 전에 쐐기부터 내린다
        phase = Phase.WEDGE_DOWN;
        timer.reset();
    }

    /** INIT 동안 매 루프 호출. */
    public void initLoop() {
        if (phase == Phase.WEDGE_DOWN && timer.seconds() >= WEDGE_DOWN_DELAY_S) {
            turret.goTo(Turret.INIT_POS);  // ② 쐐기가 내려간 뒤 터렛 회전
            phase = Phase.TURRET_MOVING;
            timer.reset();
        } else if (phase == Phase.TURRET_MOVING && timer.seconds() >= WEDGE_UP_DELAY_S) {
            wedge.set(Wedge.State.INIT);   // ③ 터렛이 다 돈 뒤 쐐기 최대
            phase = Phase.DONE;
        } else if (phase == Phase.WEDGE_ONLY && timer.seconds() >= WEDGE_SETTLE_S) {
            phase = Phase.DONE;
        }
    }

    /**
     * START 직후 한 번 호출 (TeleOp). 쐐기를 먼저 내리고, TURRET_RETURN_DELAY_S 뒤에 터렛을 가운데로.
     * INIT 때 begin() 을 안 했어도(= 오토에서 넘어와 시작 자세가 남아 있을 수 있음) 항상 이 순서를 지킨다.
     */
    public void beginRelease() {
        wedge.set(Wedge.State.STOW);
        releasing = Turret.initPoseSet();
        timer.reset();
    }

    /** INIT 때 시작 자세를 만들기 시작했는지. */
    public boolean started() { return phase != Phase.OFF; }

    /**
     * START 후 매 루프 호출 (TeleOp). true 인 동안은 터렛을 시작 자세에 잡아 두어야 한다 (스틱 무시).
     * 시간이 지나면 터렛을 가운데로 보내고 false 를 돌려준다.
     */
    public boolean holdingTurret() {
        if (!releasing) return false;
        if (timer.seconds() < TURRET_RETURN_DELAY_S) return true;
        releasing = false;
        turret.center();
        return false;
    }

    /** 지금 쓰는 자세 값. 코드값과 다르면(= Panels 에서 바뀜) 경고를 붙인다. */
    public static String values() {
        if (!Turret.ENABLED) {
            String w = String.format("터렛 고정(정면, 명령 안 보냄) / 쐐기 INIT %.3f · STOW %.3f", Wedge.INIT, Wedge.STOW);
            boolean wChanged = Wedge.INIT != CODE_WEDGE_INIT || Wedge.STOW != CODE_WEDGE_STOW;
            return wChanged ? w + String.format("  ⚠ Panels에서 바뀜! 코드값 = 쐐기 %.3f · %.3f → 로봇 앱을 다시 켜면 코드값으로",
                    CODE_WEDGE_INIT, CODE_WEDGE_STOW) : w + "  (코드값 그대로)";
        }
        String now = String.format("터렛 INIT %.3f · 가운데 %.3f / 쐐기 INIT %.3f · STOW %.3f",
                Turret.INIT_POS, Turret.CENTER_POS, Wedge.INIT, Wedge.STOW);
        boolean changed = Turret.INIT_POS != CODE_TURRET_INIT || Turret.CENTER_POS != CODE_TURRET_CENTER
                || Wedge.INIT != CODE_WEDGE_INIT || Wedge.STOW != CODE_WEDGE_STOW;
        if (!changed) return now + "  (코드값 그대로)";
        return now + String.format("  ⚠ Panels에서 바뀜! 코드값 = 터렛 %.3f · %.3f / 쐐기 %.3f · %.3f → 로봇 앱을 다시 켜면 코드값으로",
                CODE_TURRET_INIT, CODE_TURRET_CENTER, CODE_WEDGE_INIT, CODE_WEDGE_STOW);
    }

    public boolean done() { return phase == Phase.DONE; }

    public String status() {
        switch (phase) {
            case OFF: return "시작 자세 꺼짐 (Turret.INIT_POS 음수)";
            case WEDGE_ONLY: return "쐐기 올리는 중... (터렛 고정)";
            case WEDGE_DOWN: return "① 쐐기 내리는 중...";
            case TURRET_MOVING: return "② 터렛 돌리는 중...";
            default: return Turret.ENABLED ? "③ 시작 자세 완료 (터렛 반시계 90°, 쐐기 최대)"
                    : "③ 시작 자세 완료 (터렛 정면 고정, 쐐기 최대)";
        }
    }
}
