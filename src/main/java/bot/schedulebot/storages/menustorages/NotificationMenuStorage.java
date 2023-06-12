package bot.schedulebot.storages.menustorages;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Notification;
import bot.schedulebot.repositories.NotificationRepository;
import bot.schedulebot.util.generators.KeyboardGenerator;
import bot.schedulebot.util.generators.TextGenerator;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class NotificationMenuStorage {
    private final KeyboardGenerator keyboardGenerator;
    private final TextGenerator textGenerator;
    private final NotificationRepository notificationRepository;

    public NotificationMenuStorage(KeyboardGenerator keyboardGenerator, TextGenerator textGenerator, NotificationRepository notificationRepository) {
        this.keyboardGenerator = keyboardGenerator;
        this.textGenerator = textGenerator;
        this.notificationRepository = notificationRepository;
    }

    public Message getNotificationMenu(int id) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Notification notification = notificationRepository.get(id, session);
        message.setText(textGenerator.getNotificationMenuText(notification));
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getNotificationMenuKeyboard(notification)));
        session.close();
        return message;
    }

    public Message getNotificationEditMenu(int id) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Notification notification = notificationRepository.get(id, session);
        message.setText(textGenerator.getNotificationMenuText(notification));
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(keyboardGenerator.createSingleButtonRow("Edit text", "Change notification text " + id));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Edit time", "Change notification time " + id));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Edit frequency", "Change notification frequency " + id));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Edit next date", "Change notification date " + id));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show notification " + id));
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        session.close();
        return message;
    }
}
