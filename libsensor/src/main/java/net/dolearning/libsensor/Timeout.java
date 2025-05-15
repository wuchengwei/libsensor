package net.dolearning.libsensor;

import java.util.Timer;
import java.util.TimerTask;

public class Timeout {
    private Timer timer;

    public Timeout() {}

    public Timeout(TimeoutCallback callback, long delay) {
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                callback.run();
            }
        };
        timer.schedule(task, delay);
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
