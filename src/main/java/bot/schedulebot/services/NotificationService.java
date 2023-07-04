//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Group;
import bot.schedulebot.entities.Notification;
import bot.schedulebot.entities.User;
import bot.schedulebot.enums.EditOrNew;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.objectsunderconstruction.NotificationsUnderConstruction;
import bot.schedulebot.repositories.GroupRepository;
import bot.schedulebot.repositories.NotificationRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.*;
import bot.schedulebot.util.generators.KeyboardGenerator;

import java.lang.reflect.Field;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Service
public class NotificationService extends bot.schedulebot.services.Service<Notification> {
    private final NotificationsUnderConstruction notificationsUnderConstruction;
    private final ParseUtil parseUtil;
    private final UserRepository userRepository;
    private final MenuStorage menuStorage;
    private final GroupRepository groupRepository;
    private final NotificationRepository notificationRepository;
    private final BotConfig botConfig;
    private final KeyboardGenerator keyboardGenerator;
    private final TimersStorage timersStorage;

    protected NotificationService(NotificationsUnderConstruction notificationsUnderConstruction, Converter converter, ParseUtil parseUtil, UserRepository userRepository, MenuStorage menuStorage, GroupRepository groupRepository, NotificationRepository notificationRepository, KeyboardGenerator keyboardGenerator, TimersStorage timersStorage, ThreadUtil threadUtil, ClassFieldsStorage classFieldsStorage) {
        super(notificationRepository, threadUtil, parseUtil, notificationsUnderConstruction, menuStorage, converter, null, classFieldsStorage, null, userRepository);
        this.notificationsUnderConstruction = notificationsUnderConstruction;
        this.parseUtil = parseUtil;
        this.userRepository = userRepository;
        this.menuStorage = menuStorage;
        this.groupRepository = groupRepository;
        this.notificationRepository = notificationRepository;
        this.botConfig = new BotConfig();
        this.keyboardGenerator = keyboardGenerator;
        this.timersStorage = timersStorage;
    }

    @Override
    public void handleAdditionStart(Update update) {
        Notification notification = new Notification();
        User user = userRepository.get(parseUtil.getTag(update));
        user.setGroupMode(true);
        user.setMode("Add");
        user.setInstanceAdditionStage(InstanceAdditionStage.NOTIFICATION_START);
        userRepository.update(user);
        notification.setGroup(groupRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData())));
        addEntity(update, notification);
    }

    private void startTimer(Notification notification) {
        Timer timer = new Timer();
        this.timersStorage.getRepeatedNotificationTimers().put(notification.getId(), timer);
        timer.schedule(this.getNotificationTask(notification, notification.getGroup().getUsers().stream().map(User::getChatId).collect(Collectors.toList())),
                Date.from(notification.getDate().atTime(notification.getTime()).atZone(ZoneId.systemDefault()).toInstant()),
                (long)notification.getFrequency() * 24L * 60L * 60L * 1000L);
    }

    private TimerTask getNotificationTask(final Notification notification, final List<String> chatIds) {
        return new TimerTask() {
            public void run() {
                chatIds.forEach((chatId) -> {
                    Message message = new Message();
                    message.setText(notification.getText());
                    message.setReplyMarkup(new InlineKeyboardMarkup(NotificationService.this.keyboardGenerator.getDeleteButtonKeyboard()));
                    notification.setDate(LocalDate.now().plusDays(notification.getFrequency()));
                    List<Message> messages = new ArrayList();
                    messages.add(message);
                    NotificationService.this.botConfig.sendMessagesList(chatId, messages);
                });
            }
        };
    }
    @Override
    protected Object parseUpdate(Field field, Update update) throws InterruptedException{
        try {
            return super.parseUpdate(field, update);
        } catch (NullPointerException e) {
            if (field.getType().equals(LocalDate.class)) {
                return LocalDate.parse(update.getCallbackQuery().getData().replace("Notification ", ""));
            }
            else throw e;
        }
    }
    public void handleNotificationDelete(String callbackData, Update update, List<Message> resultMessagesList, User u) {
        int groupId = this.stopNotification(this.parseUtil.getTargetId(callbackData));
        Message message = new Message();
        message.setText("Notification deleted");
        this.botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
        resultMessagesList.add(message);
        resultMessagesList.add(this.menuStorage.getMenu(MenuMode.SHOW_NOTIFICATIONS_IN_GROUP_MENU, update, groupId));
    }

    private int stopNotification(int id) {
        Session session = HibernateConfig.getSession();
        int groupId = this.notificationRepository.get(id, session).getGroup().getId();
        session.close();
        this.timersStorage.getRepeatedNotificationTimers().get(id).cancel();
        this.notificationRepository.delete(id);
        return groupId;
    }

    @Override
    protected void persistEntity(Update update, Notification notification) {
        super.persistEntity(update, notification);
        Session session = HibernateConfig.getSession();
        startTimer(notificationRepository.get(notification.getId(), session));
        session.close();
    }
}
