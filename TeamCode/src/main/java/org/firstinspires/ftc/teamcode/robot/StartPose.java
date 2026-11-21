package org.firstinspires.ftc.teamcode.robot;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * 시작 자세 (인스펙션 크기 규정): 터렛을 반시계 90°(Turret.INIT_POS) 돌리고 쐐기를 최대(Wedge.INIT)로 올린 상태.
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

    private enum Phase { OFF, WEDGE_DOWN, TURRET_MOVING, DONE }

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
        }
    }

    /** START 직후 한 번 호출 (TeleOp). 쐐기를 먼저 내린다. */
    public void beginRelease() {
        wedge.set(Wedge.State.STOW);
        releasing = active;
        timer.reset();
    }

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

    public String status() {
        switch (phase) {
            case OFF: return "시작 자세 꺼짐 (Turret.INIT_POS 음수)";
            case WEDGE_DOWN: return "① 쐐기 내리는 중...";
            case TURRET_MOVING: return "② 터렛 돌리는 중...";
            default: return "③ 시작 자세 완료 (터렛 반시계 90°, 쐐기 최대)";
        }
    }
}
