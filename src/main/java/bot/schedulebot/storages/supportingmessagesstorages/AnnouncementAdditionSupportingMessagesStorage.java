package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.util.generators.KeyboardGenerator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class AnnouncementAdditionSupportingMessagesStorage implements AdditionSupportingMessagesStorage {
    private final KeyboardGenerator keyboardGenerator;

    public AnnouncementAdditionSupportingMessagesStorage(KeyboardGenerator keyboardGenerator) {
        this.keyboardGenerator = keyboardGenerator;
    }

    @Override
    public Message getMessageByAdditionStage(InstanceAdditionStage instanceAdditionStage, Update update) {
        Message message = new Message();
        switch (instanceAdditionStage) {
            case ANNOUNCEMENT_START -> {
                message = getStartOfAdditionMessage();
            }
            case ANNOUNCEMENT_FILE, ANNOUNCEMENT_IMAGE_SKIP_FILE -> {
                message = getSkipDocumentStageOfAdditionMessage();
            }
            case ANNOUNCEMENT_IMAGE, ANNOUNCEMENT_TITLE_SKIP_IMAGE -> {
                message = getSkipImageStageOfAdditionMessage();
            }
            case ANNOUNCEMENT_TITLE -> {
                message = getTitleStageOfAdditionMessage();
            }
            case ANNOUNCEMENT_TEXT -> {
                message = getTextStageOfAdditionMessage();
            }
        }
        return message;
    }

    private Message getSkipDocumentStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter title");
        return message;
    }

    private Message getTitleStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter text");
        return message;
    }

    private Message getSkipImageStageOfAdditionMessage() {
        return getYesNoMessage("document");
    }

    private Message getTextStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Announcement added");
        return message;
    }

    private Message getStartOfAdditionMessage() {
        return getYesNoMessage("image");
    }

    public Message getYesNoMessage(String base) {
        Message message = new Message();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> text = new ArrayList<>();
        List<String> callBack = new ArrayList<>();
        text.add("Yes");
        text.add("No");
        callBack.add("Announcement set " + base + " yes");
        callBack.add("Announcement set " + base + " no");
        keyboard.add(keyboardGenerator.createManyButtonsRow(text, callBack));
        markup.setKeyboard(keyboard);
        message.setText("Do you have any " + base + " for this task?");
        message.setReplyMarkup(markup);
        return message;
    }

}
