package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.util.generators.KeyboardGenerator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
public class NotificationAdditionSupportingMessagesStorage implements AdditionSupportingMessagesStorage {
    private final KeyboardGenerator keyboardGenerator;

    public NotificationAdditionSupportingMessagesStorage(KeyboardGenerator keyboardGenerator) {
        this.keyboardGenerator = keyboardGenerator;
    }

    @Override
    public Message getMessageByAdditionStage(InstanceAdditionStage instanceAdditionStage, Update update) {
        switch (instanceAdditionStage) {
            case NOTIFICATION_START -> {
                return getStartOfAdditionMessage();
            }
            case NOTIFICATION_TEXT -> {
                return getTextStageOfAdditionMessage();
            }
            case NOTIFICATION_TITLE -> {
                return getTitleStageOfAdditionMessage();
            }
            case NOTIFICATION_DATE -> {
                return getDateStageOfAdditionMessage();
            }
            case NOTIFICATION_TIME -> {
                return getTimeStageOfAdditionMessage();
            }
            case NOTIFICATION_FREQUENCY -> {
                return getFrequencyStageOfAdditionMessage();
            }
            default -> throw new RuntimeException("Wrong instance addition stage (notification supporting message)");
        }
    }

    private Message getFrequencyStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Notification configured!");
        return message;
    }

    private Message getTimeStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter text of notification");
        return message;
    }

    private Message getDateStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter time (hh:mm)");
        return message;
    }

    private Message getTextStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter title");
        return message;
    }

    private Message getTitleStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter frequency (in days)");
        return message;
    }

    private Message getStartOfAdditionMessage() {
        Message message = new Message();
        message.setText("Choose day of current or next week when it will be sent first");
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getNextSevenDaysKeyboard()));
        return message;
    }
}
