package bot.schedulebot.util;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.entities.*;
import bot.schedulebot.objectsunderconstruction.TasksUnderConstruction;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.storages.menustorages.GeneralMenuStorage;
import bot.schedulebot.util.generators.KeyboardGenerator;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class Notificator {
    private final KeyboardGenerator keyboardGenerator;
    private final BotConfig botConfig;
    private final UserRepository userRepository;
    private final TasksUnderConstruction tasksUnderConstruction;
    private final ParseUtil parseUtil;
    private final GeneralMenuStorage generalMenuStorage;

    public Notificator(KeyboardGenerator keyboardGenerator, UserRepository userRepository, TasksUnderConstruction tasksUnderConstruction, ParseUtil parseUtil, GeneralMenuStorage generalMenuStorage) {
        this.keyboardGenerator = keyboardGenerator;
        this.botConfig = new BotConfig();
        this.userRepository = userRepository;
        this.tasksUnderConstruction = tasksUnderConstruction;
        this.parseUtil = parseUtil;
        this.generalMenuStorage = generalMenuStorage;
    }

    public void sendNotificationsToGroupUsersAboutNewAppointment(Group group, Update update, Task task) {
        group.getUsers().stream().filter(user -> !user.getTag().equals(parseUtil.getTag(update)) && user.isWantToGenNotifications()).forEach(user -> {
            sendNotificationToUserAboutNewAppointment(
                    user.getUnappointedTasks().stream().filter(unappointedTask -> unappointedTask.getTask().equals(task)).toList().get(0).getId(),
                    user.getChatId());
        });
    }

    public void sendNotificationsToGroupUsersAboutNewAnnouncement(List<User> users, Update update, Announcement announcement) {
        users.stream().filter(user -> !user.getTag().equals(parseUtil.getTag(update)) && user.isWantToGenNotifications()).forEach(user -> {
            if (user.isWantToGenNotifications())
                sendNotificationToUserAboutNewAnnouncement(announcement.getId(), user.getChatId());
        });
    }

    public void sendPersonalNotificationAboutNewTask(User user, UnappointedTask unappointedTask) {
        User u = userRepository.get(tasksUnderConstruction.getTaskTargets().get(user.getTag()));
        sendNotificationToUserAboutNewAppointment(unappointedTask.getId(), u.getChatId());
    }

    private void sendNotificationToUserAboutNewAnnouncement(int announcementId, String chatId) {
        List<Message> notification = new ArrayList<>();
        Message message = new Message();
        message.setText("There is a new announcement for you");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Check it",
                "Show announcement " + announcementId));
        keyboard.addAll(keyboardGenerator.getDeleteButtonKeyboard());
        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);

        notification.add(message);

        try {
            botConfig.sendMessagesList(chatId, notification);
        } catch (RuntimeException e) {
            //in case bot is blocked
        }
    }

    private void sendNotificationToUserAboutNewAppointment(int unappointedTaskId, String chatId) {
        List<Message> notification = new ArrayList<>();
        Message message = new Message();
        message.setText("You have new task");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Check it",
                "Show unappointed task " + unappointedTaskId));
        keyboard.addAll(keyboardGenerator.getDeleteButtonKeyboard());
        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);

        notification.add(message);

        try {
            botConfig.sendMessagesList(chatId, notification);
        } catch (RuntimeException e) {
            //in case bot is blocked
        }
    }

    public void sendEverydayTasksNotification(String chatId, TodayTasksInfo todayTasksInfo, Session session) {
        try {
            List<Message> notification = new ArrayList<>();
            notification.add(generalMenuStorage.getTasksForTodayMenu(todayTasksInfo, session));
            List<Integer> messageIds = botConfig.sendMessagesList(chatId, notification);
            botConfig.pinMessage(chatId, messageIds.get(0));
            todayTasksInfo.setMessageId(messageIds.get(0));
        } catch (RuntimeException e) {
            userRepository.get(chatId).setWantToGenNotifications(false);
        }
    }
}
