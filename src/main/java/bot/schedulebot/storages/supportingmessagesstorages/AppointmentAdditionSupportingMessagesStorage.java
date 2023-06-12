package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.enums.InstanceAdditionStage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class AppointmentAdditionSupportingMessagesStorage implements AdditionSupportingMessagesStorage {
    @Override
    public Message getMessageByAdditionStage(InstanceAdditionStage instanceAdditionStage, Update update) {
        switch (instanceAdditionStage) {
            case APPOINTMENT_START -> {
                return getStartOfAdditionMessage();
            }
            case APPOINTMENT_DATE -> {
                return getDateStageOfAdditionMessage();
            }
            default -> throw new RuntimeException("Wrong appointment addition stage");
        }
    }

    private Message getDateStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Task appointed");
        return message;
    }

    private Message getStartOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter date to appoint (dd.mm.yyyy)");
        return message;
    }
}
