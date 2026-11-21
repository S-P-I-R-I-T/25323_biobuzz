package org.firstinspires.ftc.teamcode.opmode;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.robot.HardwareNames;
import org.firstinspires.ftc.teamcode.robot.Turret;

/**
 * 터렛 기어비 실측 (서보가 몇 바퀴 돌 때 터렛이 1바퀴 도는지).
 *
 * <pre>
 * 공통 조작 (gamepad1)
 *   오른쪽 스틱 X   터렛 돌리기 (최대 MAX_POWER)
 *   LB / RB         터렛 아주 천천히 왼쪽 / 오른쪽 (누르는 동안)
 *   D-pad 상 / 하   측정할 터렛 바퀴 수 N  +1 / -1  (기본 1, 여러 바퀴일수록 정확)
 *   A               측정 시작 (지금 위치 = 0, 카운터 초기화)
 *
 * 방법 1: 피드백 선이 있을 때 (Configuration에 Analog Input "turretEnc" 등록)
 *   서보가 돈 각도를 코드가 자동으로 누적한다. 터렛을 N바퀴 돌려 표시선에 다시 맞추면 화면에 기어비가 나온다.
 *
 * 방법 2: 피드백 선이 없을 때 (눈으로 세기)
 *   서보 쪽 기어에 테이프 깃발을 붙이고, 깃발이 기준점을 지날 때마다 Y (한 바퀴 +1).
 *   마지막에 남은 조각은 D-pad 좌/우로 1/4바퀴씩 -/+ 해서 맞춘다. X = 카운트만 초기화.
 * </pre>
 *
 * 결과(기어비)는 "서보 회전수 ÷ 터렛 회전수"다. 예: 2.5 → 서보 2.5바퀴에 터렛 1바퀴.
 */
@Configurable
@TeleOp(name = "Turret Ratio Test", group = "Test")
public class TurretRatioTest extends LinearOpMode {
    /** 스틱 끝까지 밀었을 때 터렛 파워. 측정은 천천히 해야 정확하다. */
    public static double MAX_POWER = 0.3;
    public static double FINE_POWER = 0.08;

    @Override
    public void runOpMode() {
        CRServo turret = hardwareMap.get(CRServo.class, HardwareNames.TURRET);
        turret.setDirection(Turret.REVERSED ? DcMotorSimple.Direction.REVERSE : DcMotorSimple.Direction.FORWARD);   // TeleOp과 같은 방향
        AnalogInput feedback = hardwareMap.tryGet(AnalogInput.class, HardwareNames.TURRET_FEEDBACK);

        telemetry.addLine("Turret Ratio Test");
        telemetry.addLine(feedback != null ? "피드백 선 발견 → 방법 1 (자동)" : "피드백 선 없음 → 방법 2 (눈으로 세기)");
        telemetry.addLine("터렛과 몸체에 같은 위치로 표시선을 긋고 START.");
        telemetry.update();
        waitForStart();

        int turretTurns = 1;
        double manualServoTurns = 0;      // 방법 2: Y로 센 서보 바퀴 수 (+ 1/4 조각)
        double servoDeg = 0;              // 방법 1: 누적 서보 각도
        double lastAngle = feedback != null ? angleOf(feedback) : 0;
        double maxStepDeg = 0;            // 한 루프에 가장 많이 돈 각도 (너무 크면 측정이 틀림)
        double minVolt = Double.POSITIVE_INFINITY, maxVolt = 0;   // 서보 한 바퀴 동안 본 전압 범위

        while (opModeIsActive()) {
            // ── 조작 ──
            double power;
            if (gamepad1.left_bumper) power = -FINE_POWER;
            else if (gamepad1.right_bumper) power = FINE_POWER;
            else power = Math.abs(gamepad1.right_stick_x) < 0.05 ? 0 : gamepad1.right_stick_x * MAX_POWER;
            turret.setPower(power);

            if (gamepad1.dpadUpWasPressed()) turretTurns++;
            if (gamepad1.dpadDownWasPressed()) turretTurns = Math.max(1, turretTurns - 1);
            if (gamepad1.aWasPressed()) {
                servoDeg = 0;
                manualServoTurns = 0;
                maxStepDeg = 0;
                if (feedback != null) lastAngle = angleOf(feedback);
            }
            if (gamepad1.yWasPressed()) manualServoTurns += 1;
            if (gamepad1.dpadRightWasPressed()) manualServoTurns += 0.25;
            if (gamepad1.dpadLeftWasPressed()) manualServoTurns = Math.max(0, manualServoTurns - 0.25);
            if (gamepad1.xWasPressed()) manualServoTurns = 0;

            // ── 방법 1: 피드백 누적 ──
            if (feedback != null) {
                double angle = angleOf(feedback);
                double step = angle - lastAngle;
                if (step > 180) step -= 360;       // 359° → 1° 처럼 한 바퀴 넘어간 경우
                if (step < -180) step += 360;
                lastAngle = angle;
                servoDeg += Turret.FEEDBACK_REVERSED ? -step : step;
                maxStepDeg = Math.max(maxStepDeg, Math.abs(step));

                double v = feedback.getVoltage();
                minVolt = Math.min(minVolt, v);
                maxVolt = Math.max(maxVolt, v);
                double servoTurns = Math.abs(servoDeg) / 360.0;
                telemetry.addData("방법 1 (자동)", "");
                telemetry.addData("  피드백 전압", "%.3f V  (각도 %.1f°)", v, angle);
                telemetry.addData("  지금까지 최저 / 최고 전압", "%.3f / %.3f V  → Turret.FEEDBACK_MAX_V 를 최고값에 맞출 것 (지금 %.2f)",
                        minVolt, maxVolt, Turret.FEEDBACK_MAX_V);
                telemetry.addData("  서보 누적", "%.1f°  = %.3f 바퀴", servoDeg, servoTurns);
                telemetry.addLine("  확인: 스틱 오른쪽으로 돌릴 때 누적이 늘어야 함. 줄어들면 Turret.FEEDBACK_REVERSED = true");
                telemetry.addData("  기어비", "%.3f  (터렛 %d바퀴 기준)", servoTurns / turretTurns, turretTurns);
                if (maxStepDeg > 90) telemetry.addLine("  ⚠ 너무 빨리 돌렸음. A로 다시 시작하고 천천히 돌릴 것");
            }

            // ── 방법 2: 눈으로 센 값 ──
            telemetry.addData("방법 2 (세기)", "서보 %.2f 바퀴 ÷ 터렛 %d 바퀴 = 기어비 %.3f",
                    manualServoTurns, turretTurns, manualServoTurns / turretTurns);
            telemetry.addLine();
            telemetry.addData("터렛 파워", "%.2f", power);
            telemetry.addLine("A 시작 | 스틱/LB/RB 돌리기 | D-pad 상/하 터렛 바퀴 수 | Y 서보 +1 | D-pad 좌/우 ∓1/4");
            telemetry.update();
        }
        turret.setPower(0);
    }

    /** 피드백 전압 → 서보 각도 (0~360°). TeleOp 터렛과 같은 계산 (Turret.FEEDBACK_MAX_V 사용). */
    private static double angleOf(AnalogInput in) { return Turret.voltageToDeg(in.getVoltage()); }
}
