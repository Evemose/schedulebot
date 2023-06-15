package bot.schedulebot.handlers;

import bot.schedulebot.entities.User;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.objectsunderconstruction.AnnouncementsUnderConstruction;
import bot.schedulebot.objectsunderconstruction.NotificationsUnderConstruction;
import bot.schedulebot.objectsunderconstruction.SubjectsUnderConstruction;
import bot.schedulebot.objectsunderconstruction.TasksUnderConstruction;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.services.*;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.ParseUtil;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final TasksUnderConstruction tasksUnderConstruction;
    private final NotificationsUnderConstruction notificationsUnderConstruction;

    public ServiceController(SubjectService subjectAdditionHandler, TaskService taskAdditionHandler, GroupService groupAdditionHandler, AppointmentService appointmentAdditionHandler, UserRepository userRepository, ParseUtil parseUtil, MenuStorage menuStorage, AnnouncementService announcementService, NotificationService notificationService, SubjectsUnderConstruction subjectsUnderConstruction, AnnouncementsUnderConstruction announcementsUnderConstruction, TasksUnderConstruction tasksUnderConstruction, NotificationsUnderConstruction notificationsUnderConstruction) {
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
        this.tasksUnderConstruction = tasksUnderConstruction;
        this.notificationsUnderConstruction = notificationsUnderConstruction;
    }


    public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, String mode) {
        List<Message> messages = new ArrayList<>();
        try {
            if (Objects.equals(mode, "Add")) {
                if (update.hasCallbackQuery() || (update.hasMessage() && update.getMessage().getText() != null) &&
                        !(instanceAdditionStage.equals(InstanceAdditionStage.TASK_IMAGE)
                                || instanceAdditionStage.equals(InstanceAdditionStage.TASK_FILE)) || instanceAdditionStage.toString().startsWith("ANNOUNCEMENT")) {
                    if (instanceAdditionStage.toString().startsWith("SUBJECT")) {
                        Exchanger<Update> exchanger = subjectsUnderConstruction.getExchangers().get(parseUtil.getTag(update));
                        exchanger.exchange(update);
                    } else if (instanceAdditionStage.toString().startsWith("GROUP")) {
                        messages = groupService.handleAddition(instanceAdditionStage, update, null);
                    } else if (instanceAdditionStage.toString().startsWith("APPOINTMENT")) {
                        messages = appointmentService.handleAddition(instanceAdditionStage, update, null);
                    } else if (instanceAdditionStage.toString().startsWith("TASK")) {
                        messages = taskService.handleAddition(instanceAdditionStage, update, null);
                    } else if (instanceAdditionStage.toString().startsWith("ANNOUNCEMENT")) {
                        Exchanger<Update> exchanger = announcementsUnderConstruction.getExchangers().get(parseUtil.getTag(update));
                        exchanger.exchange(update);
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
            } else if (mode.equals("Edit")) {
                if (instanceAdditionStage.toString().startsWith("ANNOUNCEMENT")) {
                    Exchanger<Update> exchanger = announcementsUnderConstruction.getEditExchangers().get(parseUtil.getTag(update));
                    exchanger.exchange(update);
                } else if (instanceAdditionStage.toString().startsWith("TASK")) {
                    Exchanger<Update> exchanger = tasksUnderConstruction.getEditExchangers().get(parseUtil.getTag(update));
                    exchanger.exchange(update);
                } else if (instanceAdditionStage.toString().startsWith("NOTIFICATION")) {
                    Exchanger<Update> exchanger = notificationsUnderConstruction.getEditExchangers().get(parseUtil.getTag(update));
                    exchanger.exchange(update);
                }
                else {
                    throw new IllegalStateException("Unexpected value: " + instanceAdditionStage);
                }
            }
        } catch (InterruptedException ignored) {
        }
        return messages;
    }

    public void handlePropertyChange(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        User user = userRepository.get(parseUtil.getTag(update));
        user.setMode("Edit");
        if (callbackData.substring("Change ".length()).startsWith("task")) {
            user.setInstanceAdditionStage(InstanceAdditionStage.TASK_START);
            user.setGroupMode(true);
            userRepository.update(user);
            taskService.editEntity(update,
                    update.getCallbackQuery().getData().substring("Change task ".length(),
                            update.getCallbackQuery().getData().indexOf(String.valueOf(parseUtil.getTargetId(update.getCallbackQuery().getData()))) - 1));
        } else if (callbackData.substring("Change ".length()).startsWith("announcement")) {
            user.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_START);
            user.setGroupMode(true);
            userRepository.update(user);
            announcementService.editEntity(update,
                    update.getCallbackQuery().getData().substring("Change announcement ".length(),
                            update.getCallbackQuery().getData().indexOf(String.valueOf(parseUtil.getTargetId(update.getCallbackQuery().getData()))) - 1));
        } else if (callbackData.substring("Change ".length()).startsWith("notification")) {
            user.setInstanceAdditionStage(InstanceAdditionStage.NOTIFICATION_START);
            user.setGroupMode(true);
            userRepository.update(user);
            notificationService.editEntity(update,
                    update.getCallbackQuery().getData().substring("Change notification ".length(),
                            update.getCallbackQuery().getData().indexOf(String.valueOf(parseUtil.getTargetId(update.getCallbackQuery().getData()))) - 1));
        }
    }
}
