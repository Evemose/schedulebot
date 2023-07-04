package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.enums.InstanceAdditionStage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class SubjectAdditionSupportingMessagesStorage implements AdditionSupportingMessagesStorage {
    public Message getMessageByAdditionStage(InstanceAdditionStage instanceAdditionStage, Update update) {
        Message message = new Message();
        switch (instanceAdditionStage) {
            case SUBJECT_START -> message = getStartOfAdditionMessage();
            case SUBJECT_NAME -> message = getNameStageOfAdditionMessage();
        }
        return message;
    }

    private Message getNameStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Subject added");
        return message;
    }

    private Message getStartOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter name");
        return message;
    }
}
