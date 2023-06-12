//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package bot.schedulebot.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import org.springframework.stereotype.Component;

@Component
public class TimersStorage {
    private final Map<Integer, Timer> repeatedNotificationTimers = new HashMap();

    public TimersStorage() {
    }

    public Map<Integer, Timer> getRepeatedNotificationTimers() {
        return this.repeatedNotificationTimers;
    }
}
