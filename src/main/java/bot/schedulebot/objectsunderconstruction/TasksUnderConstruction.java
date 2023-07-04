package bot.schedulebot.objectsunderconstruction;

import bot.schedulebot.entities.Task;
import bot.schedulebot.enums.TaskType;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Component
public class TasksUnderConstruction extends ObjectsUnderConstruction<Task> {
    private final Map<String, TaskType> taskTypes;
    private final Map<String, String> taskTargets;

    private TasksUnderConstruction() {
        taskTypes = new HashMap<>();
        taskTargets = new HashMap<>();
        messageIds = new HashMap<>();
    }
}
