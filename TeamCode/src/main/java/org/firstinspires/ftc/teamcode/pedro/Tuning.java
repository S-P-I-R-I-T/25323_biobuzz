package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.pedropathing.tuning.autotune.Procedure;
import com.pedropathing.tuning.autotune.Tuner;

import org.firstinspires.ftc.teamcode.pedro.procedures.ForesightTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.MecanumTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.PinpointTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.ShooterTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.Tests;

/**
 * 튜닝 페이지(로봇 Wi-Fi 접속 후 http://192.168.43.1:10158)에 나오는 튜너 목록.
 * 순서: Mecanum → Pinpoint → Foresight → Tests. Shooter는 따로.
 */
public class Tuning {
    /** 1) 주행 모터 방향 찾기. */
    @Tuner
    public static Procedure mecanumTuner() {
        return new MecanumTuner();
    }

    /** 2) Pinpoint pod 방향·오프셋 찾기. */
    @Tuner
    public static Procedure pinpointTuner() {
        return new PinpointTuner();
    }

    /** 3) 경로 추종 값 찾기. 1), 2) 결과를 Constants에 넣은 뒤 실행. */
    @Tuner
    public static Procedure foresightTuner() {
        return new ForesightTuner(
                hardwareMap -> new PinpointLocalizer(hardwareMap, Constants.localizerConfig),
                hardwareMap -> new Mecanum(hardwareMap, Constants.driveConfig));
    }

    /** 4) 튜닝 결과 확인. 3) 결과를 Constants에 넣은 뒤 실행. */
    @Tuner
    public static Procedure tests() {
        return new Tests(
                hardwareMap -> new Mecanum(hardwareMap, Constants.driveConfig),
                hardwareMap -> new PinpointLocalizer(hardwareMap, Constants.localizerConfig),
                () -> new Foresight(Constants.foresightConfig));
    }

    /** 슈터 속도 PIDF 찾기 (모터 이름은 튜닝 페이지에서 입력). */
    @Tuner
    public static Procedure shooterTuner() {
        return new ShooterTuner();
    }
}
