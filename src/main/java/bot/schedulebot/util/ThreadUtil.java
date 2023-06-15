package bot.schedulebot.util;

import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;

@Component
public class ThreadUtil {

    private ThreadUtil() {
    }

    public void scheduleThreadKill(Thread thread) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                thread.interrupt();
                System.out.println("Thread " + thread.getName() + " was killed");
            }
        }, 36 * 1000 * 100);

    }
}
