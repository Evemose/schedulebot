package bot.schedulebot.handlers;

import bot.schedulebot.entities.User;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.objectsunderconstruction.AnnouncementsUnderConstruction;
import bot.schedulebot.objectsunderconstruction.SubjectsUnderConstruction;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.services.*;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.ParseUtil;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;

@Controller
public class ServiceController {
    private final SubjectService subjectService;
    private final TaskService taskService;
    private final GroupService groupService;
    private final Service appointmentService;
    private final UserRepository userRepository;
    private final ParseUtil parseUtil;
    private final MenuStorage menuStorage;
    private final AnnouncementService announcementService;
    private final NotificationService notificationService;
    private final SubjectsUnderConstruction subjectsUnderConstruction;
    private final AnnouncementsUnderConstruction announcementsUnderConstruction;

    public ServiceController(SubjectService subjectAdditionHandler, TaskService taskAdditionHandler, GroupService groupAdditionHandler, AppointmentService appointmentAdditionHandler, UserRepository userRepository, ParseUtil parseUtil, MenuStorage menuStorage, AnnouncementService announcementService, NotificationService notificationService, SubjectsUnderConstruction subjectsUnderConstruction, AnnouncementsUnderConstruction announcementsUnderConstruction) {
        this.subjectService = subjectAdditionHandler;
        this.taskService = taskAdditionHandler;
        this.groupService = groupAdditionHandler;
        this.appointmentService = appointmentAdditionHandler;
        this.userRepository = userRepository;
        this.parseUtil = parseUtil;
        this.menuStorage = menuStorage;
        this.announcementService = announcementService;
        this.notificationService = notificationService;
        this.subjectsUnderConstruction = subjectsUnderConstruction;
        this.announcementsUnderConstruction = announcementsUnderConstruction;
    }


    public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, Object entity) {
        List<Message> messages = new ArrayList<>();
        if (update.hasCallbackQuery() || (update.hasMessage() && update.getMessage().getText() != null) &&
                !(instanceAdditionStage.equals(InstanceAdditionStage.TASK_IMAGE)
                        || instanceAdditionStage.equals(InstanceAdditionStage.TASK_FILE)) || instanceAdditionStage.toString().startsWith("ANNOUNCEMENT")) {
            if (instanceAdditionStage.toString().startsWith("SUBJECT")) {
                Exchanger<Update> exchanger = subjectsUnderConstruction.getExchangers().get(parseUtil.getTag(update));
                try {
                    exchanger.exchange(update);
                } catch (InterruptedException ignored) {
                }
            } else if (instanceAdditionStage.toString().startsWith("GROUP")) {
                messages = groupService.handleAddition(instanceAdditionStage, update, null);
            } else if (instanceAdditionStage.toString().startsWith("APPOINTMENT")) {
                messages = appointmentService.handleAddition(instanceAdditionStage, update, null);
            } else if (instanceAdditionStage.toString().startsWith("TASK")) {
                messages = taskService.handleAddition(instanceAdditionStage, update, null);
            } else if (instanceAdditionStage.toString().startsWith("ANNOUNCEMENT")) {
                Exchanger<Update> exchanger = announcementsUnderConstruction.getExchangers().get(parseUtil.getTag(update));
                try {
                    exchanger.exchange(update);
                } catch (InterruptedException ignored) {
                }
            } else if (instanceAdditionStage.toString().startsWith("NOTIFICATION")) {
                messages = notificationService.handleAddition(instanceAdditionStage, update, null);
            }
        } else if ((instanceAdditionStage.equals(InstanceAdditionStage.TASK_IMAGE) || instanceAdditionStage.equals(InstanceAdditionStage.ANNOUNCEMENT_IMAGE)) && update.getMessage().getPhoto() != null
                || (instanceAdditionStage.equals(InstanceAdditionStage.TASK_FILE) || instanceAdditionStage.equals(InstanceAdditionStage.ANNOUNCEMENT_FILE)) && update.getMessage().getDocument() != null) {
            messages = instanceAdditionStage.toString().startsWith("TASK") ?
                    taskService.handleAddition(instanceAdditionStage, update, null) :
                    announcementService.handleAddition(instanceAdditionStage, update, null);
        } else {
            Message message = new Message();
            message.setText("Do not the bot. Go to the door and think about your actions");
            messages.add(message);
            messages.add(menuStorage.getMenu(MenuMode.MAIN_MENU, update, userRepository.get(parseUtil.getTag(update)).getId()));
        }
        return messages;
    }

    public void handlePropertyChange(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        if (callbackData.substring("Change ".length()).startsWith("task")) {
            taskService.handleTaskPropertyChange(update, resultMessagesList, callbackData, u);
        } else if (callbackData.substring("Change ".length()).startsWith("announcement")) {
            announcementService.handleAnnouncementPropertyChange(update, resultMessagesList, callbackData, u);
        } else if (callbackData.substring("Change ".length()).startsWith("notification")) {
            notificationService.handleNotificationPropertyChange(update, resultMessagesList, callbackData, u);
        }
    }
}