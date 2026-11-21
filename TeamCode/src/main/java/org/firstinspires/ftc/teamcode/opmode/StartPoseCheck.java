package org.firstinspires.ftc.teamcode.opmode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.StartPose;
import org.firstinspires.ftc.teamcode.robot.Turret;
import org.firstinspires.ftc.teamcode.robot.Wedge;

/**
 * 시작 자세(INIT) 검증 · 값 찾기. 오토·TeleOp 과 똑같은 순서(StartPose)로 움직인다.
 *
 * <pre>
 * gamepad1 (START 누른 뒤)
 *   A        시작 자세 만들기: 쐐기 내림 → 터렛 INIT 위치 → 쐐기 INIT 위치  (오토 INIT 과 같음)
 *   B        풀기: 쐐기 STOW 로 내림 → 터렛 가운데  (TeleOp START 와 같음)
 *   X        쐐기만 STOW 로 내리기 (안전 자세)
 *   LB / RB  터렛 INIT 위치 −0.005 / +0.005  — 쐐기가 내려가 있을 때만 (쐐기가 올라간 채로 터렛을 돌리면 안 됨)
 *   D-pad 하 / 상  쐐기 INIT 위치 −0.001 / +0.001  — 시작 자세가 완료된 상태에서만
 * </pre>
 * 바꾼 값은 이 앱이 켜져 있는 동안만 남는다. 맞는 값을 찾으면 화면의 숫자를 코드에 고정할 것.
 */
@TeleOp(name = "Start Pose Check", group = "Test")
public class StartPoseCheck extends LinearOpMode {
    @Override
    public void runOpMode() {
        Turret turret = new Turret(hardwareMap);
        Wedge wedge = new Wedge(hardwareMap);
        StartPose pose = new StartPose(turret, wedge);

        while (opModeInInit()) {
            telemetry.addLine("Start Pose Check — START 후 A = 시작 자세, B = 풀기, X = 쐐기 내림");
            telemetry.addData("자세 값", StartPose.values());
            telemetry.addLine("INIT 동안에는 움직이지 않음. 쐐기·터렛 주변에 손 없게 하고 START.");
            telemetry.update();
        }
        waitForStart();
        if (isStopRequested()) return;

        String note = "-";
        boolean running = false;     // A 로 시작 자세를 만드는 중
        boolean releasing = false;   // B 로 푸는 중
        while (opModeIsActive()) {
            Wedge.State ws = wedge.state();
            boolean wedgeUp = ws == Wedge.State.INIT;
            // "WasPressed" 는 한 번 읽으면 사라지므로 루프마다 한 번만 읽어 둔다
            boolean lb = gamepad1.leftBumperWasPressed(), rb = gamepad1.rightBumperWasPressed();
            boolean up = gamepad1.dpadUpWasPressed(), down = gamepad1.dpadDownWasPressed();

            if (gamepad1.aWasPressed()) {
                pose.begin();
                running = true;
                releasing = false;
                note = "시작 자세 만드는 중";
            }
            if (gamepad1.bWasPressed()) {
                pose.beginRelease();
                running = false;
                releasing = true;
                note = "푸는 중";
            }
            if (gamepad1.xWasPressed()) {
                wedge.set(Wedge.State.STOW);
                running = false;
                note = "쐐기 STOW";
            }
            if ((lb || rb) && !Turret.ENABLED) {
                note = "터렛 고정 중 (Turret.ENABLED = false) — 터렛은 조절하지 않음";
            } else if (lb || rb) {
                if (wedgeUp || running) {
                    note = "⚠ 쐐기가 올라가 있거나 동작 중 → 먼저 X 로 쐐기를 내리세요";
                } else {
                    Turret.INIT_POS = clamp01(Turret.INIT_POS + (rb ? 0.005 : -0.005));
                    turret.goTo(Turret.INIT_POS);
                    note = String.format("터렛 → %.3f", Turret.INIT_POS);
                }
            }
            if (up || down) {
                if (!pose.done() || !wedgeUp) {
                    note = "⚠ 쐐기 조절은 A 로 시작 자세가 완료된 뒤에만";
                } else {
                    Wedge.INIT = clamp01(Wedge.INIT + (up ? 0.001 : -0.001));
                    wedge.setRaw(Wedge.INIT);
                    note = String.format("쐐기 → %.3f", Wedge.INIT);
                }
            }

            if (running) {
                pose.initLoop();
                if (pose.done()) { running = false; note = "시작 자세 완료"; }
            }
            if (releasing && !pose.holdingTurret()) { releasing = false; note = "풀기 완료 (터렛 가운데)"; }

            telemetry.addData("상태", running ? pose.status() : note);
            telemetry.addData("터렛 목표", "%.3f  (INIT %.3f / 가운데 %.3f)", turret.targetPosition(), Turret.INIT_POS, Turret.CENTER_POS);
            telemetry.addData("쐐기", "%s  (INIT %.3f / STOW %.3f)", Wedge.label(ws), Wedge.INIT, Wedge.STOW);
            telemetry.addData("자세 값", StartPose.values());
            telemetry.addLine(Turret.ENABLED
                    ? "A 시작 자세 | B 풀기 | X 쐐기 내림 | LB/RB 터렛 ±0.005 | D-pad 하/상 쐐기 ±0.001"
                    : "A 시작 자세(쐐기 최대) | B 풀기(쐐기 STOW) | D-pad 하/상 쐐기 ±0.001  — 터렛 고정");
            telemetry.update();
        }
    }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
}
