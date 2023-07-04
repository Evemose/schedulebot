package bot.schedulebot.storages.menustorages;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Announcement;
import bot.schedulebot.repositories.AnnouncementRepository;
import bot.schedulebot.util.Converter;
import bot.schedulebot.util.ParseUtil;
import bot.schedulebot.util.generators.FileGenerator;
import bot.schedulebot.util.generators.KeyboardGenerator;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class AnnouncementMenuStorage {
    private final Converter converter;
    private final AnnouncementRepository announcementRepository;
    private final KeyboardGenerator keyboardGenerator;
    private final FileGenerator fileGenerator;
    private final BotConfig botConfig;
    private final ParseUtil parseUtil;

    public AnnouncementMenuStorage(Converter converter, AnnouncementRepository announcementRepository, KeyboardGenerator keyboardGenerator, FileGenerator fileGenerator, ParseUtil parseUtil) {
        this.converter = converter;
        this.announcementRepository = announcementRepository;
        this.keyboardGenerator = keyboardGenerator;
        this.fileGenerator = fileGenerator;
        this.botConfig = new BotConfig();
        this.parseUtil = parseUtil;
    }

    public Message getAnnouncementMenu(int announcementId, Update update, boolean isManageMode, boolean isImportantInfoMode) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Announcement announcement = announcementRepository.get(announcementId, session);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        if (isManageMode) {
            keyboard.add(keyboardGenerator.createSingleButtonRow("Edit", "Edit announcement " + announcementId));
            keyboard.add(keyboardGenerator.createSingleButtonRow("Delete", "Delete announcement " + announcementId));
            if (announcement.getGroup().getImportantInfo().contains(announcement)) {
                keyboard.add(keyboardGenerator.createSingleButtonRow("Remove from important", "Remove announcement from important " + announcementId));
            } else {
                keyboard.add(keyboardGenerator.createSingleButtonRow("Mark as important", "Mark announcement as important " + announcementId));
            }
        }
        if (!isImportantInfoMode)
            keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show announcements" + (isManageMode ? " menu" : "") + " in group " + announcement.getGroup().getId()));
        else {
            keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show important info in group " + announcement.getGroup().getId()));
        }

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        message = getAnnouncementMessage(update, message, announcement, markup);

        session.close();
        return message;
    }

    private Message getAnnouncementMessage(Update update, Message message, Announcement announcement, InlineKeyboardMarkup markup) {
        if (announcement.getFile() != null) {
            File file = fileGenerator.getFileFromByteArray(announcement.getFile().getFile(),
                    announcement.getTitle() + announcement.getFile().getFileType());
            botConfig.sendDocument(parseUtil.getChatId(update),
                    new InputFile(file),
                    "*Attachment*");
            file.delete();
        }

        if (announcement.getImage() != null) {
            File file = converter.convertJsonStringToFile(announcement.getImage());
            botConfig.sendPhoto(parseUtil.getChatId(update),
                    new InputFile(file),
                    announcement.toString(),
                    markup);
            message = null;
            file.delete();
        } else {
            message.setReplyMarkup(markup);
            message.setText(announcement.toString());
        }
        return message;
    }

    public Message getAnnouncementEditMenu(int announcementId, Update update) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Announcement announcement = announcementRepository.get(announcementId, session);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Change title", "Change announcement title " + announcementId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Change text", "Change announcement text " + announcementId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Change task image", "Change announcement image " + announcementId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Change task file", "Change announcement file " + announcementId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show announcement (manage) " + announcementId));
        markup.setKeyboard(keyboard);

        message = getAnnouncementMessage(update, message, announcement, markup);

        session.close();
        return message;
    }
}
