package bot.schedulebot.objectsunderconstruction;

import bot.schedulebot.entities.Task;
import bot.schedulebot.enums.EditOrNew;
import bot.schedulebot.enums.TaskType;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TasksUnderConstruction extends ObjectsUnderConstruction<Task> {
    @Getter
    Map<String, TaskType> taskTypes;
    @Getter
    Map<String, String> taskTargets;
    @Getter
    Map<String, EditOrNew> editOrNewTask;
    @Getter
    Map<String, Integer> messageIds;

    public TasksUnderConstruction() {
        taskTypes = new HashMap<>();
        taskTargets = new HashMap<>();
        editOrNewTask = new HashMap<>();
        messageIds = new HashMap<>();
    }
}
