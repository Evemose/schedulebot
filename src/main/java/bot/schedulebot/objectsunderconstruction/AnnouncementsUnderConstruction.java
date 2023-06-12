package bot.schedulebot.objectsunderconstruction;

import bot.schedulebot.entities.Announcement;
import bot.schedulebot.enums.EditOrNew;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AnnouncementsUnderConstruction extends ObjectsUnderConstruction<Announcement> {
    @Getter
    Map<String, EditOrNew> editOrNewTask;

    public AnnouncementsUnderConstruction() {
        editOrNewTask = new HashMap<>();
    }
}
