package org.firstinspires.ftc.teamcode.robot;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * 인테이크 모터(goBILDA 1150rpm)와, 공이 슈터로 넘어가지 않게 막는 블로커 서보(goBILDA 300° 토크 서보).
 * TODO(25323): 서보 위치와 파워는 임시값. Hardware Check로 찾아서 바꿀 것.
 */
@Configurable
public class Intake {
    /** 공을 빨아들일 때 Hardware Check에서 반대로 돌면 true. 25323: Hardware Check에서 B(−)가 빨아들임 → true (11/21). */
    public static boolean REVERSED = true;
    /** 공 줍기 파워 (0~1). */
    public static double INTAKE_POWER = 0.8;
    /** 공 뱉기 파워 (음수). */
    public static double OUTTAKE_POWER = -0.5;
    /** 발사할 때 슈터로 밀어 넣는 파워. */
    public static double FEED_POWER = 0.7;

    /**
     * 블로커 열림/닫힘 위치 (0~1, 300° 서보라 0.01 ≈ 3°).
     * 2026-11-21: 열림 0.700. 닫힘은 0.500에서 공 4개를 못 잡아서 0.600으로 변경.
     */
    public static double BLOCKER_OPEN = 0.700;
    public static double BLOCKER_CLOSED = 0.600;

    private final DcMotor motor;
    private final Servo blocker;
    private boolean blockerOpen;

    public Intake(HardwareMap hardwareMap) {
        motor = hardwareMap.get(DcMotor.class, HardwareNames.INTAKE);
        motor.setDirection(REVERSED ? DcMotorSimple.Direction.REVERSE : DcMotorSimple.Direction.FORWARD);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        blocker = hardwareMap.get(Servo.class, HardwareNames.BLOCKER);
    }

    public void intake() { motor.setPower(INTAKE_POWER); }
    public void outtake() { motor.setPower(OUTTAKE_POWER); }
    public void feed() { motor.setPower(FEED_POWER); }
    public void stop() { motor.setPower(0); }

    public void openBlocker() { blocker.setPosition(BLOCKER_OPEN); blockerOpen = true; }
    public void closeBlocker() { blocker.setPosition(BLOCKER_CLOSED); blockerOpen = false; }
    public boolean blockerOpen() { return blockerOpen; }
}
