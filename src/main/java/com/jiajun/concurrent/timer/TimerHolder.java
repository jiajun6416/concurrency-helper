package com.jiajun.concurrent.timer;

import java.util.concurrent.TimeUnit;

/**
 * @author jiajun
 */
public class TimerHolder {

    private static Timer DEFAULT_HASHED_WHEEL_TIMER;

    public static Timer instance() {
        if (DEFAULT_HASHED_WHEEL_TIMER != null) {
            return DEFAULT_HASHED_WHEEL_TIMER;
        }
        synchronized (TimerHolder.class) {
            if (DEFAULT_HASHED_WHEEL_TIMER == null) {
                // 最大误差是10ms
                DEFAULT_HASHED_WHEEL_TIMER = new HashedWheelTimer(10, TimeUnit.MILLISECONDS, 512);
            }
        }
        return DEFAULT_HASHED_WHEEL_TIMER;
    }
}
