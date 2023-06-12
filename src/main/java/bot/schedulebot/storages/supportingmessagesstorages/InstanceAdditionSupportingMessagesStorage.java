package bot.schedulebot.storages.supportingmessagesstorages;

import bot.schedulebot.enums.InstanceAdditionStage;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Controller
public class InstanceAdditionSupportingMessagesStorage {
    private final AdditionSupportingMessagesStorage subjectCreationFormStorage;
    private final AdditionSupportingMessagesStorage groupAdditionSupportingMessagesStorage;
    private final AdditionSupportingMessagesStorage taskAdditionSupportingMessagesStorage;
    private final AdditionSupportingMessagesStorage appointmentAdditionSupportingMessagesStorage;
    private final AnnouncementAdditionSupportingMessagesStorage announcementAdditionSupportingMessagesStorage;
    private final NotificationAdditionSupportingMessagesStorage notificationAdditionSupportingMessagesStorage;

    public InstanceAdditionSupportingMessagesStorage(SubjectAdditionSupportingMessagesStorage subjectCreationFormStorage, GroupAdditionSupportingMessagesStorage groupAdditionFormStorage, TaskAdditionSupportingMessagesStorage taskAdditionFormStorage, AppointmentAdditionSupportingMessagesStorage appointmentAdditionFormStorage, AnnouncementAdditionSupportingMessagesStorage announcementAdditionSupportingMessagesStorage, NotificationAdditionSupportingMessagesStorage notificationAdditionSupportingMessagesStorage) {
        this.subjectCreationFormStorage = subjectCreationFormStorage;
        this.groupAdditionSupportingMessagesStorage = groupAdditionFormStorage;
        this.taskAdditionSupportingMessagesStorage = taskAdditionFormStorage;
        this.appointmentAdditionSupportingMessagesStorage = appointmentAdditionFormStorage;
        this.announcementAdditionSupportingMessagesStorage = announcementAdditionSupportingMessagesStorage;
        this.notificationAdditionSupportingMessagesStorage = notificationAdditionSupportingMessagesStorage;
    }

    public Message getMessageByStage(InstanceAdditionStage instanceAdditionStage, Update update) {
        if (instanceAdditionStage.toString().startsWith("SUBJECT")) {
            return subjectCreationFormStorage.getMessageByAdditionStage(instanceAdditionStage, update);
        } else if (instanceAdditionStage.toString().startsWith("GROUP")) {
            return groupAdditionSupportingMessagesStorage.getMessageByAdditionStage(instanceAdditionStage, update);
        } else if (instanceAdditionStage.toString().startsWith("TASK")) {
            return taskAdditionSupportingMessagesStorage.getMessageByAdditionStage(instanceAdditionStage, update);
        } else if (instanceAdditionStage.toString().startsWith("APPOINTMENT")) {
            return appointmentAdditionSupportingMessagesStorage.getMessageByAdditionStage(instanceAdditionStage, update);
        } else if (instanceAdditionStage.toString().startsWith("ANNOUNCEMENT")) {
            return announcementAdditionSupportingMessagesStorage.getMessageByAdditionStage(instanceAdditionStage, update);
        } else if (instanceAdditionStage.toString().startsWith("NOTIFICATION")) {
            return notificationAdditionSupportingMessagesStorage.getMessageByAdditionStage(instanceAdditionStage, update);
        } else throw new RuntimeException("Wrong stage");
    }
}
