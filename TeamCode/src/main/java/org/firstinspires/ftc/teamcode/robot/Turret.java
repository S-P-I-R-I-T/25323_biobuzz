package org.firstinspires.ftc.teamcode.robot;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PwmControl;

/**
 * 터렛 회전 (Axon MAX MK2).
 *
 * 두 가지 방식을 지원한다. Axon 서보 자체의 모드에 맞춰 POSITION_MODE 를 고른다.
 *
 * 1) POSITION_MODE = true (Axon "standard"/위치 모드) — 2026-11-21 현재 25323 서보는 이 모드다.
 *    서보가 스스로 "몇 번 위치"를 기억하고 지킨다. 그래서 피드백 선 없이도 터렛 위치를 안다.
 *    - 스틱을 밀면 목표 위치가 그 방향으로 계속 움직이고, 놓으면 그 자리에 멈춰 있다.
 *    - CENTER_POS = 터렛이 인테이크 방향을 볼 때의 위치. center() 로 돌아간다. START 하면 여기로 간다.
 *    - MIN_POS ~ MAX_POS 밖으로는 안 간다 (선 꼬임·충돌 방지). Hardware Check로 측정해서 넣을 것.
 *    - 위치 값은 Hardware Check 화면의 터렛 "위치"와 같은 숫자다 (0~1).
 *
 * 2) POSITION_MODE = false (Axon "continuous"/연속 회전 모드)
 *    스틱 = 회전 속도. 피드백 선(Analog Input "turretEnc")이 있고 GEAR_RATIO 를 넣으면
 *    각도·한계각(LIMIT_DEG)·가운데 복귀가 동작한다. INIT 때 터렛이 가운데에 있어야 한다.
 */
@Configurable
public class Turret {
    /**
     * 터렛 사용 여부. false 면 이 클래스는 서보에 아무 명령도 보내지 않는다 (모든 동작이 무시됨).
     * 2026-11-21: 서보 기어가 헛돌아(유격, 대회 전 수리 불가) 터렛을 정면(인테이크 방향)에 기계적으로 고정 → false.
     * 인스펙션은 "터렛 정면 + 쐐기 0.618" 자세로 통과. 고치면 true 로 바꾸면 예전 기능이 그대로 돌아온다.
     */
    public static boolean ENABLED = false;
    /** true = Axon 위치(standard) 모드, false = 연속 회전(continuous) 모드. */
    public static boolean POSITION_MODE = true;
    /**
     * 스틱을 오른쪽으로 밀 때 터렛이 반대로 돌면 true.
     * 25323: 위치 숫자가 커지면 반시계(위에서 봐서) → 스틱 오른쪽 = 시계 방향이 되도록 true.
     */
    public static boolean REVERSED = true;

    // ── 서보 신호 범위 ──
    /**
     * 서보 신호(PWM) 범위 (마이크로초). FTC 기본은 600~2400인데, Axon MAX MK2는 500~2500을 받는다.
     * 넓히면 같은 서보로 약 11% 더 돈다 (터렛 커버리지 증가). 위치 0 = PWM_MIN_US, 1 = PWM_MAX_US, 0.5 = 1500(서보 정중앙).
     */
    public static int PWM_MIN_US = 500, PWM_MAX_US = 2500;

    // ── 위치 모드 값 (0~1, Hardware Check의 터렛 "위치"와 같은 숫자) ──
    /**
     * 터렛이 인테이크 방향(가운데)을 볼 때의 위치.
     * 2026-11-21: 시작 자세(반시계 90°)를 위해 기어를 비대칭으로 다시 끼움 → 0.268.
     * 그래서 반시계 쪽(숫자 증가, 0.268→1.0)으로는 많이, 시계 쪽(0.268→0)으로는 적게 돈다.
     */
    public static double CENTER_POS = 0.268;
    /** 터렛이 갈 수 있는 끝 위치. 25323은 기구 한계보다 서보 범위가 먼저 끝나므로 서보 전체 범위를 쓴다 (11/21 재확인). */
    public static double MIN_POS = 0.0, MAX_POS = 1.0;
    /**
     * 시작 자세(INIT) 위치: 인스펙션 크기 규정 때문에 터렛을 반시계로 90° 돌린 위치 = 서보 끝 1.00 (11/21 측정).
     * 음수(-1)로 두면 시작 자세 기능이 꺼진다.
     */
    public static double INIT_POS = 1.00;
    /** 스틱 끝까지 밀었을 때 1초에 움직이는 위치 양. 0.5 = 1초에 범위의 절반. */
    public static double MOVE_RATE = 0.5;
    /** 정밀 버튼(LB/RB)으로 1초에 움직이는 위치 양. */
    public static double FINE_RATE = 0.08;

    // ── 연속 회전 모드 값 ──
    /** 스틱 끝까지 밀었을 때 파워 (0~1). */
    public static double MAX_POWER = 0.5;
    /** 정밀 조작(버튼) 파워. */
    public static double FINE_POWER = 0.15;
    /** 기어비 = 서보 회전수 ÷ 터렛 회전수 (Turret Ratio Test). 0 = 아직 모름 → 각도 기능 꺼짐. */
    public static double GEAR_RATIO = 0;
    /** 스틱을 오른쪽으로 돌렸는데 화면의 터렛 각도가 줄어들면 true. */
    public static boolean FEEDBACK_REVERSED = false;
    /** 서보가 한 바퀴 돌 때 피드백 전압의 최고값 (V). Turret Ratio Test 화면의 "최고 전압"을 보고 맞출 것. */
    public static double FEEDBACK_MAX_V = 3.3;
    /** 가운데(0°)에서 좌우로 이 각도까지만 돈다. 0 = 한계각 꺼짐. */
    public static double LIMIT_DEG = 0;
    /** 가운데 복귀: 오차 1°당 파워. */
    public static double CENTER_KP = 0.01;
    /** 가운데 복귀: 최대 파워. */
    public static double CENTER_MAX_POWER = 0.35;
    /** 가운데 복귀: 이 오차(°) 안에 들어오면 끝. */
    public static double CENTER_TOLERANCE_DEG = 2;

    private final CRServo servo;
    private final AnalogInput feedback;
    private double power;              // 연속 모드: 파워
    private double target;             // 위치 모드: 목표 위치 (0~1)
    private boolean started;           // 위치 모드: 첫 update 전에는 서보를 움직이지 않음
    /** 마지막으로 보낸 위치 (OpMode 가 바뀌어도 앱이 켜져 있는 동안 남음). 오토 → TeleOp 때 터렛 위치를 알기 위해. */
    private static double lastSent = Double.NaN;
    private final boolean knowsPosition;
    private long lastNs = System.nanoTime();
    private double servoDeg;           // 연속 모드: INIT 이후 서보가 돈 누적 각도
    private double lastServoAngle;
    private boolean centering;
    private boolean atLimit;

    public Turret(HardwareMap hardwareMap) {
        servo = hardwareMap.get(CRServo.class, HardwareNames.TURRET);
        applyPwmRange(servo);
        // 위치 모드에서는 방향을 뒤집으면 위치 숫자까지 뒤집히므로 항상 정방향으로 두고, 스틱 부호만 바꾼다.
        servo.setDirection(!POSITION_MODE && REVERSED ? DcMotorSimple.Direction.REVERSE : DcMotorSimple.Direction.FORWARD);
        if (ENABLED && !POSITION_MODE) servo.setPower(0);   // 위치 모드는 INIT 때 움직이지 않도록 아무 명령도 안 보냄
        knowsPosition = !Double.isNaN(lastSent);
        target = knowsPosition ? lastSent : CENTER_POS;   // 이전 OpMode 가 보낸 위치를 이어받음 (모르면 가운데)
        feedback = hardwareMap.tryGet(AnalogInput.class, HardwareNames.TURRET_FEEDBACK);
        if (feedback != null) lastServoAngle = servoAngle();
    }

    /** 서보 신호 범위를 PWM_MIN_US~PWM_MAX_US 로 설정한다. Hardware Check도 같은 범위를 쓴다. */
    public static void applyPwmRange(CRServo s) {
        if (s instanceof PwmControl) {
            ((PwmControl) s).setPwmRange(new PwmControl.PwmRange(PWM_MIN_US, PWM_MAX_US));
        }
    }

    /** 피드백 선이 연결·등록되어 있는지. */
    public boolean hasFeedback() { return feedback != null; }

    /** 각도(위치) 기능을 쓸 수 있는지. 위치 모드는 항상 true. */
    public boolean angleKnown() { return POSITION_MODE || (feedback != null && GEAR_RATIO > 0); }

    /** 연속 모드 터렛 각도 (°, 0 = INIT 때 방향). 위치 모드나 각도를 모르면 NaN. */
    public double angleDeg() { return !POSITION_MODE && angleKnown() ? servoDeg / GEAR_RATIO : Double.NaN; }

    /** 위치 모드 목표 위치 (0~1). */
    public double targetPosition() { return target; }

    /** 시작 자세 위치가 측정되어 들어가 있는지. */
    public static boolean initPoseSet() { return ENABLED && POSITION_MODE && INIT_POS >= 0 && INIT_POS <= 1; }

    /** 위치 모드: 목표를 pos 로 정하고 바로 보낸다 (시작 자세 등). */
    public void goTo(double pos) {
        if (!ENABLED || !POSITION_MODE) return;
        double lo = Math.min(MIN_POS, MAX_POS), hi = Math.max(MIN_POS, MAX_POS);
        target = Math.max(lo, Math.min(hi, pos));
        send();
    }

    private void send() {
        started = true;
        lastSent = target;
        servo.setPower(2 * target - 1);    // CR 서보 파워 -1~1 = 위치 0~1 과 같은 신호
    }

    /** 이전 OpMode 가 터렛에 보낸 위치를 알고 있는지 (모르면 지금 위치를 모르는 것). */
    public boolean knowsPosition() { return knowsPosition; }

    /** 가운데로 돌아가기. 위치 모드는 즉시 목표를 가운데로, 연속 모드는 스틱을 움직이면 취소된다. */
    public void center() {
        if (!ENABLED) return;
        if (POSITION_MODE) goTo(CENTER_POS);   // 목표만 바꾸지 않고 바로 보낸다 (오토처럼 update 를 안 부르는 곳에서도 움직이도록)
        else if (angleKnown()) centering = true;
    }
    public boolean isCentering() { return centering; }
    public boolean atLimit() { return atLimit; }

    /**
     * 매 루프 한 번 호출.
     * @param stick -1(왼쪽) ~ 1(오른쪽), 스틱 값
     * @param fine  -1, 0, 1 (정밀 버튼)
     */
    public void update(double stick, int fine) {
        if (!ENABLED) return;   // 터렛 고정 — 아무 명령도 안 보냄
        long now = System.nanoTime();
        double dt = Math.min((now - lastNs) / 1e9, 0.1);
        lastNs = now;
        if (POSITION_MODE) updatePosition(stick, fine, dt);
        else updateContinuous(stick, fine);
    }

    private void updatePosition(double stick, int fine, double dt) {
        double rate = fine != 0 ? fine * FINE_RATE : Math.max(-1, Math.min(1, stick)) * MOVE_RATE;
        if (REVERSED) rate = -rate;
        double lo = Math.min(MIN_POS, MAX_POS), hi = Math.max(MIN_POS, MAX_POS);
        double next = target + rate * dt;
        atLimit = (next <= lo && rate < 0) || (next >= hi && rate > 0);
        target = Math.max(lo, Math.min(hi, next));
        power = rate;
        send();
    }

    private void updateContinuous(double stick, int fine) {
        trackAngle();

        double want;
        if (fine != 0) want = fine * FINE_POWER;
        else want = Math.max(-1, Math.min(1, stick)) * MAX_POWER;

        if (want != 0) centering = false;          // 사람이 조작하면 자동 복귀 취소
        if (centering) {
            double error = -angleDeg();
            if (Math.abs(error) <= CENTER_TOLERANCE_DEG) {
                centering = false;
                want = 0;
            } else {
                want = Math.max(-CENTER_MAX_POWER, Math.min(CENTER_MAX_POWER, error * CENTER_KP));
            }
        }

        atLimit = false;
        if (angleKnown() && LIMIT_DEG > 0) {
            double a = angleDeg();
            if ((a >= LIMIT_DEG && want > 0) || (a <= -LIMIT_DEG && want < 0)) {
                want = 0;
                atLimit = true;
            }
        }

        power = want;
        servo.setPower(power);
    }

    /** 멈춤. 위치 모드는 지금 위치를 그대로 지킨다. */
    public void stop() {
        centering = false;
        power = 0;
        if (!ENABLED) return;
        if (!POSITION_MODE) servo.setPower(0);
        else if (started) servo.setPower(2 * target - 1);
    }

    public double power() { return power; }
    /** 피드백 전압 (V). 피드백 없으면 NaN. */
    public double feedbackVoltage() { return feedback != null ? feedback.getVoltage() : Double.NaN; }

    private void trackAngle() {
        if (feedback == null) return;
        double angle = servoAngle();
        double step = angle - lastServoAngle;
        if (step > 180) step -= 360;        // 359° → 1° 처럼 한 바퀴 넘어간 경우
        if (step < -180) step += 360;
        lastServoAngle = angle;
        servoDeg += FEEDBACK_REVERSED ? -step : step;
    }

    /** 피드백 전압 → 서보 각도 (0~360°). */
    private double servoAngle() { return voltageToDeg(feedback.getVoltage()); }

    /** 피드백 전압 → 서보 각도 (0~360°). Turret Ratio Test도 이 계산을 쓴다. */
    public static double voltageToDeg(double volts) {
        double max = FEEDBACK_MAX_V > 0 ? FEEDBACK_MAX_V : 3.3;
        return Math.max(0, Math.min(1, volts / max)) * 360.0;
    }
}
