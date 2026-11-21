package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.follower.Follower;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.robot.HardwareNames;

/**
 * 25323 로봇 설정.
 *
 * 튜닝 순서 (전부 Tuning.java에 등록된 튜너로 진행, 결과 코드를 아래에 붙여넣기):
 *   1) Mecanum Tuner   → driveConfig 의 모터 방향
 *   2) Pinpoint Tuner  → localizerConfig 의 pod 방향·오프셋
 *   3) Foresight Tuner → foresightConfig 전체, 끝나면 FORESIGHT_TUNED = true
 */
public class Constants {
    /**
     * 주행 모터. 포트 이름은 확정(fl, fr, rl, rr).
     * 방향은 2026-11-21 Mecanum Tuner 결과 (왼쪽 2개 REVERSE, 오른쪽 2개 FORWARD).
     * TeleOp(robot/Drive)과 Park Auto도 이 값을 쓴다.
     */
    public static MecanumConfig driveConfig = new MecanumConfig(c -> {
        c.frontLeftName.set(HardwareNames.FRONT_LEFT);
        c.backLeftName.set(HardwareNames.BACK_LEFT);
        c.frontRightName.set(HardwareNames.FRONT_RIGHT);
        c.backRightName.set(HardwareNames.BACK_RIGHT);

        c.frontLeftDirection.set(DcMotorSimple.Direction.REVERSE);
        c.backLeftDirection.set(DcMotorSimple.Direction.REVERSE);
        c.frontRightDirection.set(DcMotorSimple.Direction.FORWARD);
        c.backRightDirection.set(DcMotorSimple.Direction.FORWARD);
    });

    /**
     * Pinpoint 위치 센서.
     * 이름 "pinpoint", 4-bar pod 확정. I2C는 Control Hub bus 1 (bus 0은 허브 내장 IMU 자리).
     * ⚠ 2026-11-21: 좌우(Y) 데드휠 고장, 여분 없음 → Pedro 경로 오토는 쓰지 않음 (opmode/BackupAuto 사용).
     *   데드휠을 교체하면 Pinpoint Tuner → Foresight Tuner 순서로 다시 진행할 것.
     * TODO(25323): 방향·오프셋은 임시값. Pinpoint Tuner 결과로 교체.
     *  방법: https://pedropathing.com/docs/pathing/tuning/localization/pinpoint
     */
    public static PinpointConfig localizerConfig = new PinpointConfig(c -> {
        c.name.set(HardwareNames.PINPOINT);
        c.podType.set(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        c.xPodOffset.set(0.0);                                                        // TODO(25323): 튜너로 측정
        c.yPodOffset.set(0.0);                                                        // TODO(25323): 튜너로 측정
        c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);          // TODO(25323): 튜너 결과
        c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);          // TODO(25323): 튜너 결과
        c.globalDistanceUnit.set(DistanceUnit.INCH);
        c.offsetUnits.set(DistanceUnit.INCH);
    });

    /**
     * 경로 추종(Foresight) 값. 로봇마다 다르므로 25324 값을 쓰지 않는다.
     * Foresight Tuner 결과를 이 안에 붙여넣고 FORESIGHT_TUNED 를 true 로 바꾼다.
     */
    public static ForesightConfig foresightConfig = new ForesightConfig(c -> {
        // TODO(25323): Foresight Tuner 결과 붙여넣기
    });

    /** Foresight 튜닝 결과를 붙여넣었으면 true. false인 동안 경로 주행(오토)은 시작되지 않는다. */
    public static final boolean FORESIGHT_TUNED = false;

    public static Follower create(HardwareMap h) {
        if (!FORESIGHT_TUNED) {
            throw new IllegalStateException(
                    "Foresight 튜닝 전입니다. Tuning 페이지에서 Foresight Tuner를 돌리고 "
                            + "Constants.foresightConfig 에 결과를 넣은 뒤 FORESIGHT_TUNED = true 로 바꾸세요.");
        }
        return new Follower(
                new PinpointLocalizer(h, localizerConfig),
                new Mecanum(h, driveConfig),
                new Foresight(foresightConfig)
        );
    }
}
