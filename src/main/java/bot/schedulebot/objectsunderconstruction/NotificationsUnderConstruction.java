//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package bot.schedulebot.objectsunderconstruction;

import bot.schedulebot.entities.Notification;
import bot.schedulebot.enums.EditOrNew;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationsUnderConstruction extends ObjectsUnderConstruction<Notification> {
    private final Map<String, EditOrNew> editOrNewNotification = new HashMap();

    public NotificationsUnderConstruction() {
    }

    public Map<String, EditOrNew> getEditOrNewNotification() {
        return this.editOrNewNotification;
    }
}
