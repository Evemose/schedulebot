package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.enums.InstanceAdditionStage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class AnnouncementAdditionSupportingMessagesStorage implements AdditionSupportingMessagesStorage {
    private final GeneralAdditionSupportingMessagesStorage generalAdditionSupportingMessagesStorage;

    public AnnouncementAdditionSupportingMessagesStorage(GeneralAdditionSupportingMessagesStorage generalAdditionSupportingMessagesStorage) {
        this.generalAdditionSupportingMessagesStorage = generalAdditionSupportingMessagesStorage;
    }

    @Override
    public Message getMessageByAdditionStage(InstanceAdditionStage instanceAdditionStage, Update update) {
        Message message = new Message();
        switch (instanceAdditionStage) {
            case ANNOUNCEMENT_START -> message = getStartOfAdditionMessage();
            case ANNOUNCEMENT_FILE, ANNOUNCEMENT_IMAGE_SKIP_FILE -> message = getSkipDocumentStageOfAdditionMessage();
            case ANNOUNCEMENT_IMAGE, ANNOUNCEMENT_TITLE_SKIP_IMAGE -> message = getSkipImageStageOfAdditionMessage();
            case ANNOUNCEMENT_TITLE -> message = getTitleStageOfAdditionMessage();
            case ANNOUNCEMENT_TEXT -> message = getTextStageOfAdditionMessage();
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
        return generalAdditionSupportingMessagesStorage.getYesNoMessage("document", "announcement");
    }

    private Message getTextStageOfAdditionMessage() {
        Message message = new Message();
        message.setText("Announcement added");
        return message;
    }

    private Message getStartOfAdditionMessage() {
        return generalAdditionSupportingMessagesStorage.getYesNoMessage("image", "announcement");
    }
}
