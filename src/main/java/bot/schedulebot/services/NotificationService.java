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
import bot.schedulebot.util.Converter;
import bot.schedulebot.util.ParseUtil;
import bot.schedulebot.util.ThreadUtil;
import bot.schedulebot.util.TimersStorage;
import bot.schedulebot.util.generators.KeyboardGenerator;
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

    protected NotificationService(NotificationsUnderConstruction notificationsUnderConstruction, Converter converter, ParseUtil parseUtil, UserRepository userRepository, MenuStorage menuStorage, GroupRepository groupRepository, NotificationRepository notificationRepository, KeyboardGenerator keyboardGenerator, TimersStorage timersStorage, ThreadUtil threadUtil) {
        super(notificationRepository, threadUtil, parseUtil, notificationsUnderConstruction, menuStorage, converter, null);
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

    public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, Notification entity) {
        User user = this.userRepository.get(this.parseUtil.getTag(update));
        List<Message> messages = new ArrayList<>();
        Message message;
        switch (instanceAdditionStage) {
            case NOTIFICATION_START -> {
                this.handleNotificationAdditionStart(user, update);
                messages.add(this.menuStorage.getMenu(MenuMode.ADD_NOTIFICATION, update));
            }
            case NOTIFICATION_TEXT -> {
                this.handleNotificationTextSet(user, update);
                messages.add(this.menuStorage.getMenu(MenuMode.SET_NOTIFICATION_TEXT, update));
            }
            case NOTIFICATION_DATE -> {
                this.handleNotificationDateSet(user, update);
                messages.add(this.menuStorage.getMenu(MenuMode.SET_NOTIFICATION_DATE, update));
            }
            case NOTIFICATION_TIME -> {
                try {
                    this.handleNotificationTimeSet(user, update);
                    messages.add(this.menuStorage.getMenu(MenuMode.SET_NOTIFICATION_TIME, update));
                } catch (DateTimeException var10) {
                    message = new Message();
                    message.setText("Incorrect time format. Try again");
                    messages.add(message);
                }
            }
            case NOTIFICATION_FREQUENCY -> {
                try {
                    int id = this.handleNotificationFrequencySet(user, update);
                    messages.add(this.menuStorage.getMenu(MenuMode.SET_NOTIFICATION_FREQUENCY, update));
                    messages.add(this.menuStorage.getMenu(MenuMode.SHOW_NOTIFICATION_MENU, update, id));
                } catch (RuntimeException var9) {
                    message = new Message();
                    message.setText(var9.getMessage());
                    messages.add(message);
                }
            }
            default -> throw new RuntimeException("Wrong addition stage (notification)");
        }

        if (this.notificationsUnderConstruction.getEditOrNewNotification().get(this.parseUtil.getTag(update)) != null && ((EditOrNew)this.notificationsUnderConstruction.getEditOrNewNotification().get(this.parseUtil.getTag(update))).equals(EditOrNew.EDIT)) {
            Session session = HibernateConfig.getSession();
            user.setInstanceAdditionStage(InstanceAdditionStage.NONE);
            this.userRepository.update(user);
            Notification notification = (Notification)this.notificationsUnderConstruction.getObjectsUnderConstructions().get(this.parseUtil.getTag(update));
            this.notificationRepository.update(notification);
            this.startTimer((Notification)this.notificationRepository.get(((Notification)this.notificationsUnderConstruction.getObjectsUnderConstructions().get(this.parseUtil.getTag(update))).getId(), session));
            messages.clear();
            Message message1 = new Message();
            message1.setText("Notification changed");
            messages.add(message1);
            messages.add(this.menuStorage.getMenu(MenuMode.NOTIFICATION_EDIT_MENU, update, notification.getId()));
            this.notificationsUnderConstruction.getObjectsUnderConstructions().remove(this.parseUtil.getTag(update));
            this.notificationsUnderConstruction.getEditOrNewNotification().remove(this.parseUtil.getTag(update));
            session.close();
        }

        return messages;
    }

    private int handleNotificationFrequencySet(User user, Update update) throws RuntimeException {
        Session session = HibernateConfig.getSession();

        int frequency;
        try {
            frequency = Integer.parseInt(update.getMessage().getText());
        } catch (NumberFormatException var7) {
            throw new RuntimeException("Not a number. Try again");
        }

        if (frequency < 1) {
            throw new RuntimeException("Invalid frequency value");
        } else {
            ((Notification)this.notificationsUnderConstruction.getObjectsUnderConstructions().get(this.parseUtil.getTag(update))).setFrequency(Integer.parseInt(update.getMessage().getText()));
            user.setInstanceAdditionStage(InstanceAdditionStage.NONE);
            this.userRepository.update(user);
            Notification notification = (Notification)this.notificationsUnderConstruction.getObjectsUnderConstructions().get(user.getTag());
            if (this.notificationsUnderConstruction.getEditOrNewNotification().get(user.getTag()) == null || !((EditOrNew)this.notificationsUnderConstruction.getEditOrNewNotification().get(user.getTag())).equals(EditOrNew.EDIT)) {
                this.notificationRepository.add(notification);
                this.notificationsUnderConstruction.getObjectsUnderConstructions().remove(this.parseUtil.getTag(update));
            }

            int id = notification.getId();
            this.startTimer((Notification)this.notificationRepository.get(id, session));
            session.close();
            return id;
        }
    }

    private void startTimer(Notification notification) {
        Timer timer = new Timer();
        this.timersStorage.getRepeatedNotificationTimers().put(notification.getId(), timer);
        timer.schedule(this.getNotificationTask(notification, (List)notification.getGroup().getUsers().stream().map(User::getChatId).collect(Collectors.toList())), Date.from(notification.getDate().atTime(notification.getTime()).atZone(ZoneId.systemDefault()).toInstant()), (long)notification.getFrequency() * 24L * 60L * 60L * 1000L);
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

    private void handleNotificationTimeSet(User user, Update update) throws DateTimeException {
        ((Notification)this.notificationsUnderConstruction.getObjectsUnderConstructions().get(this.parseUtil.getTag(update))).setTime(LocalTime.parse(update.getMessage().getText()));
        user.setInstanceAdditionStage(InstanceAdditionStage.NOTIFICATION_FREQUENCY);
        this.userRepository.update(user);
    }

    private void handleNotificationDateSet(User user, Update update) {
        try {
            ((Notification)this.notificationsUnderConstruction.getObjectsUnderConstructions().get(this.parseUtil.getTag(update))).setDate(LocalDate.parse(update.getCallbackQuery().getData().replace("Notification ", "")));
            user.setInstanceAdditionStage(InstanceAdditionStage.NOTIFICATION_TIME);
            this.userRepository.update(user);
        } catch (DateTimeParseException var4) {
        }

    }

    private void handleNotificationTextSet(User user, Update update) {
        ((Notification)this.notificationsUnderConstruction.getObjectsUnderConstructions().get(this.parseUtil.getTag(update))).setText(update.getMessage().getText());

        try {
            Notification var10000 = (Notification)this.notificationsUnderConstruction.getObjectsUnderConstructions().get(this.parseUtil.getTag(update));
            String var10001 = update.getMessage().getText().trim();
            var10000.setTitle(var10001.substring(0, 20) + "...");
        } catch (IndexOutOfBoundsException var4) {
            ((Notification)this.notificationsUnderConstruction.getObjectsUnderConstructions().get(this.parseUtil.getTag(update))).setTitle(update.getMessage().getText().trim());
        }

        user.setInstanceAdditionStage(InstanceAdditionStage.NOTIFICATION_DATE);
        this.userRepository.update(user);
    }

    private void handleNotificationAdditionStart(User user, Update update) {
        Notification notification = new Notification();
        this.notificationsUnderConstruction.getObjectsUnderConstructions().put(this.parseUtil.getTag(update), notification);
        notification.setGroup((Group)this.groupRepository.get(this.parseUtil.getTargetId(update.getCallbackQuery().getData())));
        user.setInstanceAdditionStage(InstanceAdditionStage.NOTIFICATION_TEXT);
        this.userRepository.update(user);
    }

    public void handleNotificationPropertyChange(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        Notification notification = (Notification)this.notificationRepository.get(this.parseUtil.getTargetId(callbackData));
        this.notificationsUnderConstruction.getObjectsUnderConstructions().put(this.parseUtil.getTag(update), notification);
        this.notificationsUnderConstruction.getEditOrNewNotification().put(this.parseUtil.getTag(update), EditOrNew.EDIT);
        String propertyToChange = callbackData.substring("Change notification ".length()).trim();
        if (propertyToChange.matches("text \\d+")) {
            resultMessagesList.add(this.menuStorage.getMenu(MenuMode.ADD_NOTIFICATION, update));
            propertyToChange = "TEXT";
        } else if (propertyToChange.matches("frequency \\d+")) {
            resultMessagesList.add(this.menuStorage.getMenu(MenuMode.SET_NOTIFICATION_TIME, update));
            propertyToChange = "FREQUENCY";
        } else if (propertyToChange.matches("time \\d+")) {
            resultMessagesList.add(this.menuStorage.getMenu(MenuMode.SET_NOTIFICATION_DATE, update));
            propertyToChange = "TIME";
        } else {
            if (!propertyToChange.matches("date \\d+")) {
                throw new RuntimeException(propertyToChange);
            }

            resultMessagesList.add(this.menuStorage.getMenu(MenuMode.SET_NOTIFICATION_TEXT, update));
            propertyToChange = "DATE";
        }

        u.setInstanceAdditionStage(InstanceAdditionStage.valueOf("NOTIFICATION_" + propertyToChange));
        ((Timer)this.timersStorage.getRepeatedNotificationTimers().get(notification.getId())).cancel();
        Session session = HibernateConfig.getSession();
        this.userRepository.update(u, session);
        session.close();
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
        int groupId = ((Notification)this.notificationRepository.get(id, session)).getGroup().getId();
        session.close();
        ((Timer)this.timersStorage.getRepeatedNotificationTimers().get(id)).cancel();
        this.notificationRepository.delete(id);
        return groupId;
    }
}
