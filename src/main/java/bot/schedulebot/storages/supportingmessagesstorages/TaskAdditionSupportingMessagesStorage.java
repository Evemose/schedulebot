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
public class TaskAdditionSupportingMessagesStorage implements AdditionSupportingMessagesStorage {
    private final KeyboardGenerator keyboardGenerator;
    private final GeneralAdditionSupportingMessagesStorage generalAdditionSupportingMessagesStorage;

    private TaskAdditionSupportingMessagesStorage(KeyboardGenerator keyboardGenerator, GeneralAdditionSupportingMessagesStorage generalAdditionSupportingMessagesStorage) {

        this.keyboardGenerator = keyboardGenerator;
        this.generalAdditionSupportingMessagesStorage = generalAdditionSupportingMessagesStorage;
    }

    @Override
    public Message getMessageByAdditionStage(InstanceAdditionStage instanceAdditionStage, Update update) {
        Message message = new Message();
        switch (instanceAdditionStage) {
            case TASK_START -> {
                message = getStartOfAdditionMessage();
            }
            case TASK_FILE, TASK_IMAGE_NO_FILE -> {
                message = getSkipDocumentStageOfAdditionMessage();
            }
            case TASK_IMAGE, TASK_NAME_NO_IMAGE -> {
                message = getSkipImageStageOfAdditionMessage();
            }
            case TASK_NAME -> {
                message = getNameStageOfAdditionMessage();
            }
            case TASK_DESCRIPTION -> {
                message = getDescriptionStageOfAdditionMessage();
            }
            case TASK_SUBJECT -> {
                message = getSubjectStageOfAdditionMessage();
            }
            case TASK_DEADLINE -> {
                message = getDeadlineStageOfAdditionMessage();
            }
        }
        return message;
    }

    private Message getSkipDocumentStageOfAdditionMessage() {
        return generalAdditionSupportingMessagesStorage.getYesNoMessage("image");
    }

    private Message getDeadlineStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Task added");
        return message;
    }

//    private Message getImagesStageOfAdditionMessage() {
//        Message message = new Message();
//        message.setText("Enter name");
//        return message;
//    }

    private Message getSubjectStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter deadline (dd.mm.yyyy)");
        return message;
    }

    private Message getNameStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter description");
        return message;
    }

    private Message getSkipImageStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter name");
        return message;
    }

    private Message getDescriptionStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Choose subject");
        return message;
    }

    private Message getStartOfAdditionMessage() {
        return generalAdditionSupportingMessagesStorage.getYesNoMessage("file");
    }


}
