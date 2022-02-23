
package com.jiajun.concurrent.timer;

public interface TimerTask {

    void run(Timeout timeout) throws Exception;
}