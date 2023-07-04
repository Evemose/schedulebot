package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.storages.menustorages.GroupMenuStorage;
import bot.schedulebot.storages.menustorages.SubjectMenuStorage;
import bot.schedulebot.util.ParseUtil;
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
    private final GroupMenuStorage groupMenuStorage;
    private final ParseUtil parseUtil;
    private final SubjectMenuStorage subjectMenuStorage;

    private TaskAdditionSupportingMessagesStorage(KeyboardGenerator keyboardGenerator, GeneralAdditionSupportingMessagesStorage generalAdditionSupportingMessagesStorage, GroupMenuStorage groupMenuStorage, ParseUtil parseUtil, SubjectMenuStorage subjectMenuStorage) {

        this.keyboardGenerator = keyboardGenerator;
        this.generalAdditionSupportingMessagesStorage = generalAdditionSupportingMessagesStorage;
        this.groupMenuStorage = groupMenuStorage;
        this.parseUtil = parseUtil;
        this.subjectMenuStorage = subjectMenuStorage;
    }

    @Override
    public Message getMessageByAdditionStage(InstanceAdditionStage instanceAdditionStage, Update update) {
        Message message = new Message();
        switch (instanceAdditionStage) {
            case TASK_START -> {
                message = getStartOfAdditionMessage();
            }
            case TASK_IMAGE, TASK_NAME_NO_IMAGE -> {
                message = getImageStageOfAdditionMessage();
            }
            case TASK_FILE, TASK_IMAGE_NO_FILE -> {
                message = getFileStageOfAdditionMessage();
            }
            case TASK_NAME -> {
                message = getNameStageOfAdditionMessage();
            }
            case TASK_DESCRIPTION -> {
                message = getDescriptionStageOfAdditionMessage(update);
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

    private Message getImageStageOfAdditionMessage() {
        return getYesNoMessage("document");
    }

    private Message getDeadlineStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter name");
        return message;
    }

    private Message getSubjectStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Task added");
        return message;
    }

    private Message getNameStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter description");
        return message;
    }

    private Message getFileStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Enter deadline (dd.mm.yyyy)");
        return message;
    }

    private Message getDescriptionStageOfAdditionMessage(Update update) {
        Message message = new Message();
        message.setText("Choose subject");
        if (update.getCallbackQuery().getData().contains("group") || update.getCallbackQuery().getData().contains(" in ")) {
            message.setReplyMarkup(groupMenuStorage.getSubjectsMenuToAlterGroup(
                    parseUtil.getTargetId(update.getCallbackQuery().getData()), "Set subject").getReplyMarkup());
        } else {
            message.setReplyMarkup(subjectMenuStorage.getUserSubjectsListToAlterUser(
                    parseUtil.getTag(update), "Set subject").getReplyMarkup());
        }
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
        callBack.add("Task set " + base + " yes");
        callBack.add("Task set " + base + " no");
        keyboard.add(keyboardGenerator.createManyButtonsRow(text, callBack));
        markup.setKeyboard(keyboard);
        message.setText("Do you have any " + base + " for this task?");
        message.setReplyMarkup(markup);
        return message;
    }


}
