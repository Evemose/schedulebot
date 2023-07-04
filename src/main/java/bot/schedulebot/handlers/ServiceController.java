package bot.schedulebot.handlers;

import bot.schedulebot.entities.User;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.objectsunderconstruction.*;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.services.*;
import bot.schedulebot.util.ParseUtil;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Exchanger;

@Controller
public class ServiceController {
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final ParseUtil parseUtil;
    private final AnnouncementService announcementService;
    private final NotificationService notificationService;
    private final Map<String, Map<String, Exchanger<Update>>> addExchangers;
    private final Map<String, Map<String, Exchanger<Update>>> editExchangers;

    public ServiceController(TaskService taskAdditionHandler, UserRepository userRepository, ParseUtil parseUtil, AnnouncementService announcementService, NotificationService notificationService, AnnouncementsUnderConstruction announcementsUnderConstruction, TasksUnderConstruction tasksUnderConstruction, NotificationsUnderConstruction notificationsUnderConstruction, GroupsUnderConstruction groupsUnderConstruction, AppointmentsUnderConstruction appointmentsUnderConstruction) {
        this.taskService = taskAdditionHandler;
        this.userRepository = userRepository;
        this.parseUtil = parseUtil;
        this.announcementService = announcementService;
        this.notificationService = notificationService;
        this.addExchangers = Map.of("TASK", tasksUnderConstruction.getExchangers(),
                "ANNOUNCEMENT", announcementsUnderConstruction.getExchangers(),
                "NOTIFICATION", notificationsUnderConstruction.getExchangers(),
                "APPOINTMENT", appointmentsUnderConstruction.getExchangers(),
                "GROUP", groupsUnderConstruction.getExchangers());
        this.editExchangers = Map.of("TASK", tasksUnderConstruction.getEditExchangers(),
                "ANNOUNCEMENT", announcementsUnderConstruction.getEditExchangers(),
                "NOTIFICATION", notificationsUnderConstruction.getEditExchangers(),
                "APPOINTMENT", appointmentsUnderConstruction.getEditExchangers());
    }


    public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, String mode) {
        List<Message> messages = new ArrayList<>();
        try {
            String stageName = instanceAdditionStage.toString();
            if (Objects.equals(mode, "Add")) {
                addExchangers.get(stageName.substring(0, stageName.indexOf("_"))).get(parseUtil.getTag(update)).exchange(update);
            }
            else if (Objects.equals(mode, "Edit")) {
                editExchangers.get(stageName.substring(0, stageName.indexOf("_"))).get(parseUtil.getTag(update)).exchange(update);
            }
            else throw new IllegalStateException("Unexpected mode: " + mode);
        } catch (InterruptedException ignored) {
        }
        return messages;
    }

    public void handlePropertyChange(Update update, String callbackData, User u) {
        u.setMode("Edit");
        String strippedCallbackData = callbackData.substring("Change ".length());
        if (strippedCallbackData.startsWith("task")) {
            u.setInstanceAdditionStage(InstanceAdditionStage.TASK_START);
            u.setGroupMode(true);
            userRepository.update(u);
            taskService.editEntity(update,
                    update.getCallbackQuery().getData().substring("Change task ".length(),
                            update.getCallbackQuery().getData().indexOf(String.valueOf(parseUtil.getTargetId(update.getCallbackQuery().getData()))) - 1));
        } else if (strippedCallbackData.startsWith("announcement")) {
            u.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_START);
            u.setGroupMode(true);
            userRepository.update(u);
            announcementService.editEntity(update,
                    update.getCallbackQuery().getData().substring("Change announcement ".length(),
                            update.getCallbackQuery().getData().indexOf(String.valueOf(parseUtil.getTargetId(update.getCallbackQuery().getData()))) - 1));
        } else if (strippedCallbackData.startsWith("notification")) {
            u.setInstanceAdditionStage(InstanceAdditionStage.NOTIFICATION_START);
            u.setGroupMode(true);
            userRepository.update(u);
            notificationService.editEntity(update,
                    update.getCallbackQuery().getData().substring("Change notification ".length(),
                            update.getCallbackQuery().getData().indexOf(String.valueOf(parseUtil.getTargetId(update.getCallbackQuery().getData()))) - 1));
        }
    }
}
