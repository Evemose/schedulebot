package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.enums.InstanceAdditionStage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class GroupAdditionSupportingMessagesStorage implements AdditionSupportingMessagesStorage {
    @Override
    public Message getMessageByAdditionStage(InstanceAdditionStage instanceAdditionStage, Update update) {
        Message message = new Message();
        switch (instanceAdditionStage) {
            case GROUP_START -> message = getStartOfAdditionMessage();
            case GROUP_NAME -> message = getNameStageOfAdditionMessage();
        }
        return message;
    }

    private Message getNameStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Group added");
        return message;
    }

    private Message getStartOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter name");
        return message;
    }
}
