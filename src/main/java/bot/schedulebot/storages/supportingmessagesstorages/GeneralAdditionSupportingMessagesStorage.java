package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.util.generators.KeyboardGenerator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
public class GeneralAdditionSupportingMessagesStorage {
    private final KeyboardGenerator keyboardGenerator;

    private GeneralAdditionSupportingMessagesStorage(KeyboardGenerator keyboardGenerator) {
        this.keyboardGenerator = keyboardGenerator;
    }

    public Message getYesNoMessage(String base, String name) {
        Message message = new Message();
        message.setText("Do you have any " + base + " for this " + name + "?");
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getYesNoKeyboard()));
        return message;
    }
}
