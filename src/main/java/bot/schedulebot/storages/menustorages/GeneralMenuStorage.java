package bot.schedulebot.storages.menustorages;

import bot.schedulebot.entities.TodayTasksInfo;
import bot.schedulebot.entities.User;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.services.TodayTasksInfoService;
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
public class GeneralMenuStorage {
    private final TextGenerator textGenerator;
    private final KeyboardGenerator keyboardGenerator;
    private final TodayTasksInfoService todayTasksInfoService;
    private final UserRepository userRepository;

    public GeneralMenuStorage(TextGenerator textGenerator, KeyboardGenerator keyboardGenerator, TodayTasksInfoService todayTasksInfoService, UserRepository userRepository) {
        this.textGenerator = textGenerator;
        this.keyboardGenerator = keyboardGenerator;
        this.todayTasksInfoService = todayTasksInfoService;
        this.userRepository = userRepository;
    }

    public Message getTasksForTodayMenu(TodayTasksInfo todayTasksInfo, Session session) {
        Message message = new Message();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
        message.setText(textGenerator.getMessageTextFromTodayTasksInfo(todayTasksInfo));
        markup.setKeyboard(keyboardGenerator.getEverydayTaskNotificationKeyboard(todayTasksInfo));
        message.setReplyMarkup(markup);

        return message;
    }

    public Message getUnhandledMessageReply() {
        Message message = new Message();
        message.setText("I cant get what you are saying");
        return message;
    }

//    public Message getManual(int userId) {
//        Message message = new Message();
//        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
//        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show main menu " + userId));
//        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
//        message.setText(textGenerator.getManualText());
//        return message;
//    }

    public Message getMainMenu(int targetId) {
        User user = userRepository.get(targetId);
        Message message = new Message();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> menuInlineKeyboard = new ArrayList<>();
        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("Groups menu", "Show groups menu"));
        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("My appointments", "Show appointments of " + targetId));
        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("My unappointed tasks", "Show unappointed tasks of " + targetId));
        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("My subjects", "Show subjects of " + targetId));
        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("Turn " + (user.isWantToGenNotifications() ? "off \uD83D\uDD34" : "on \uD83D\uDFE2") + " notifications", "Turn " + (user.isWantToGenNotifications() ? "off" : "on") + " notifications for " + targetId));
//        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("Manual", "Show manual"));

        markup.setKeyboard(menuInlineKeyboard);

        message.setText("Hello!\nChoose what you want to view:");

        message.setReplyMarkup(markup);

        return message;
    }
}
