package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.util.generators.KeyboardGenerator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class GeneralAdditionSupportingMessagesStorage {
    private final KeyboardGenerator keyboardGenerator;

    private GeneralAdditionSupportingMessagesStorage(KeyboardGenerator keyboardGenerator) {
        this.keyboardGenerator = keyboardGenerator;
    }

    public Message getYesNoMessage(String base) {
        Message message = new Message();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> text = new ArrayList<>();
        List<String> callBack = new ArrayList<>();
        text.add("Yes");
        text.add("No");
        callBack.add("Entity yes");
        callBack.add("Entity no");
        keyboard.add(keyboardGenerator.createManyButtonsRow(text, callBack));
        markup.setKeyboard(keyboard);
        message.setText("Do you have any " + base + " for this task?");
        message.setReplyMarkup(markup);
        return message;
    }
}
