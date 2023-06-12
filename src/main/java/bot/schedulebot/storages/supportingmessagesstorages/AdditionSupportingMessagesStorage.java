package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.enums.InstanceAdditionStage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface AdditionSupportingMessagesStorage {
    Message getMessageByAdditionStage(InstanceAdditionStage instanceAdditionStage, Update update);
}
