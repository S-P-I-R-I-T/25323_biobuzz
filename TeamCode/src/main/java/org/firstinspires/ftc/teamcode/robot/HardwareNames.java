package org.firstinspires.ftc.teamcode.robot;

/**
 * 25323 로봇의 하드웨어 이름과 포트. 이름은 여기 한 곳에서만 정한다.
 * Driver Station > Configure Robot 에서 아래 표 그대로 등록할 것 (이름은 대소문자까지 같아야 함).
 *
 * <pre>
 * Control Hub (허브 "Control Hub Portal" 안의 "Control Hub")
 *   Motors     0  fl        goBILDA 5203 (구동, 앞-왼쪽)
 *              1  rl        goBILDA 5203 (구동, 뒤-왼쪽)
 *              2  fr        goBILDA 5203 (구동, 앞-오른쪽)
 *              3  rr        goBILDA 5203 (구동, 뒤-오른쪽)
 *   Servos     0  turret    Continuous Rotation Servo  (Axon MAX MK2, continuous 모드)
 *              1  blocker   Servo                      (goBILDA 2000-0025-0002, 300°)
 *              2  wedge     Servo                      (goBILDA 2000-0025-0502, 5-turn)
 *   Analog 0      turretEnc Analog Input  (선택: Axon 위치 피드백 선. 없으면 등록 안 해도 됨)
 *   I2C bus 0     imu       (허브 내장 IMU. 기본으로 등록되어 있음)
 *   I2C bus 1     pinpoint  goBILDA Pinpoint Odometry Computer
 *
 * Expansion Hub
 *   Motors     0  shooter   goBILDA 5203 6000rpm (5203-2402-0001), 체인으로 플라이휠
 *              1  intake    goBILDA 5203 1150rpm (5203-2402-0005)
 * </pre>
 */
public final class HardwareNames {
    private HardwareNames() { }

    // 구동 (Control Hub 모터 0~3)
    public static final String FRONT_LEFT = "fl";
    public static final String BACK_LEFT = "rl";
    public static final String FRONT_RIGHT = "fr";
    public static final String BACK_RIGHT = "rr";

    // 센서
    public static final String IMU = "imu";
    public static final String PINPOINT = "pinpoint";

    // 메커니즘 (Expansion Hub 모터)
    public static final String SHOOTER = "shooter";
    public static final String INTAKE = "intake";

    // 서보 (Control Hub 서보 0~2)
    public static final String TURRET = "turret";
    public static final String BLOCKER = "blocker";
    public static final String WEDGE = "wedge";

    // 선택 장치: 터렛 Axon 서보의 위치 피드백 선 (Control Hub Analog 0)
    public static final String TURRET_FEEDBACK = "turretEnc";
}
