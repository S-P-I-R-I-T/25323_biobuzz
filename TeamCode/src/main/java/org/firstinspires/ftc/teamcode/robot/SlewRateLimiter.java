package org.firstinspires.ftc.teamcode.robot;

/**
 * 슬루 레이트 리미터: 명령 값이 갑자기 바뀌지 않고 정해진 속도로만 따라가게 한다.
 * 무거운 로봇(25323 약 7.3kg)이 스틱을 확 밀었을 때 바퀴가 미끄러지거나 앞이 들리는 것을 막는다.
 *
 * accelPerSec: 0에서 멀어질 때(빨라질 때) 1초에 바뀔 수 있는 양. 3이면 0 → 1 까지 약 0.33초.
 * decelPerSec: 0으로 가까워질 때(느려질 때). 멈출 때는 조금 더 빨리 반응하도록 보통 더 크게 둔다.
 */
public class SlewRateLimiter {
    private double value;
    private long lastNs = System.nanoTime();

    public double calculate(double target, double accelPerSec, double decelPerSec) {
        long now = System.nanoTime();
        double dt = Math.min((now - lastNs) / 1e9, 0.1);   // 루프가 잠깐 멈췄다가 다시 돌 때 한 번에 튀지 않게
        lastNs = now;

        boolean speedingUp = target * value >= 0 && Math.abs(target) > Math.abs(value);
        double maxStep = (speedingUp ? accelPerSec : decelPerSec) * dt;
        double diff = target - value;
        if (Math.abs(diff) > maxStep) diff = Math.signum(diff) * maxStep;
        value += diff;
        return value;
    }

    public void reset(double v) {
        value = v;
        lastNs = System.nanoTime();
    }

    public double value() { return value; }
}
