package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Announcement;
import bot.schedulebot.entities.Group;
import bot.schedulebot.entities.User;
import bot.schedulebot.enums.EditOrNew;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.objectsunderconstruction.AnnouncementsUnderConstruction;
import bot.schedulebot.repositories.AnnouncementRepository;
import bot.schedulebot.repositories.FileRepository;
import bot.schedulebot.repositories.GroupRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.Converter;
import bot.schedulebot.util.Notificator;
import bot.schedulebot.util.ParseUtil;
import bot.schedulebot.util.ThreadUtil;
import bot.schedulebot.util.generators.TextGenerator;
import org.hibernate.Session;
import org.springframework.dao.DataIntegrityViolationException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.concurrent.Exchanger;

@org.springframework.stereotype.Service
public class AnnouncementService extends Service<Announcement> {

    private final AnnouncementsUnderConstruction announcementsUnderConstruction;
    private final UserRepository userRepository;
    private final ParseUtil parseUtil;
    private final GroupRepository groupRepository;
    private final MenuStorage menuStorage;
    private final Converter converter;
    private final AnnouncementRepository announcementRepository;
    private final Notificator notificator;
    private final FileRepository fileRepository;
    private final BotConfig botConfig;
    private final ThreadUtil threadUtil;
    private final TextGenerator textGenerator;

    public AnnouncementService(AnnouncementsUnderConstruction announcementsUnderConstruction, UserRepository userRepository, ParseUtil parseUtil, GroupRepository groupRepository, MenuStorage menuStorage, Converter converter, AnnouncementRepository announcementRepository, Notificator notificator, FileRepository fileRepository, ThreadUtil threadUtil, TextGenerator textGenerator) {
        super(announcementRepository, parseUtil, announcementsUnderConstruction);
        this.announcementsUnderConstruction = announcementsUnderConstruction;
        this.userRepository = userRepository;
        this.parseUtil = parseUtil;
        this.groupRepository = groupRepository;
        this.menuStorage = menuStorage;
        this.converter = converter;
        this.announcementRepository = announcementRepository;
        this.notificator = notificator;
        this.fileRepository = fileRepository;
        this.threadUtil = threadUtil;
        this.botConfig = new BotConfig();
        this.textGenerator = textGenerator;
    }

    @Override
    public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, Announcement entity) {
        new Thread(() -> {
            try {
                String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
                threadUtil.scheduleThreadKill(Thread.currentThread());
                Exchanger<Update> exchanger = announcementsUnderConstruction.getExchangers().get(parseUtil.getTag(update));
                botConfig.sendMessage(chatId, menuStorage.getMenu(MenuMode.ADD_ANNOUNCEMENT, update));
                Update temp = exchanger.exchange(null);
                while (!temp.hasCallbackQuery()) {
                    Message message = new Message();
                    message.setText(textGenerator.getAnswerTheQuestionRequest());
                    botConfig.sendMessage(chatId, message);
                    temp = exchanger.exchange(null);
                }
                if(temp.getCallbackQuery().getData().contains("yes")) {
                    Message message = new Message();
                    message.setText("Send document you want to attach");
                    botConfig.sendMessage(chatId, message);
                    temp = exchanger.exchange(null);
                    while (!temp.getMessage().hasDocument()) {
                        message.setText("Message does not contain document. Send document you want to attach");
                        botConfig.sendMessage(chatId, message);
                        temp = exchanger.exchange(null);
                    }
                    entity.setFile(parseUtil.getMessageFile(temp));
                }
                botConfig.sendMessage(chatId, menuStorage.getMenu(MenuMode.SET_ANNOUNCEMENT_FILE, update));
                temp = exchanger.exchange(null);
                while (!temp.hasCallbackQuery()) {
                    Message message = new Message();
                    message.setText(textGenerator.getAnswerTheQuestionRequest());
                    botConfig.sendMessage(chatId, message);
                    temp = exchanger.exchange(null);
                }
                if(temp.getCallbackQuery().getData().contains("yes")) {
                    Message message = new Message();
                    message.setText("Send image you want to attach");
                    botConfig.sendMessage(chatId, message);
                    temp = exchanger.exchange(null);
                    while (!temp.getMessage().hasDocument()) {
                        message.setText("Message does not contain image. Send document you want to attach");
                        botConfig.sendMessage(chatId, message);
                        temp = exchanger.exchange(null);
                    }
                    entity.setImage(converter.convertFileToJsonString(parseUtil.getMessageImage(update)));
                }
                botConfig.sendMessage(chatId, menuStorage.getMenu(MenuMode.SET_ANNOUNCEMENT_IMAGE, update));
                temp = exchanger.exchange(null);
                entity.setTitle(temp.getMessage().getText());
                botConfig.sendMessage(chatId, menuStorage.getMenu(MenuMode.SET_ANNOUNCEMENT_TITLE, update));
                temp = exchanger.exchange(null);
                entity.setText(temp.getMessage().getText());
                persistAnnouncement(userRepository.get(parseUtil.getTag(update)), update);
                botConfig.sendMessage(chatId, menuStorage.getMenu(MenuMode.SET_ANNOUNCEMENT_TEXT, update));
                botConfig.sendMessage(chatId, menuStorage.getMenu(MenuMode.ANNOUNCEMENT_MENU_MANAGE, update, entity.getId()));
            } catch (InterruptedException ignored) {
            }
        }, "Announcement addition thread of " + update.getCallbackQuery().getFrom().getUserName()).start();
        return null;
    }

    private void persistAnnouncement(User user, Update update) {
        Announcement announcement = announcementsUnderConstruction.getObjectsUnderConstructions().get(user.getTag());
        if (announcement.getFile() != null) {
            fileRepository.add(announcement.getFile());
        }
        announcementsUnderConstruction.getEditOrNewTask().remove(user.getTag());
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(announcement.getGroup().getId(), session);
        announcementRepository.add(announcement, session);
        notificator.sendNotificationsToGroupUsersAboutNewAnnouncement(group.getUsers(), update, announcement);
        session.close();
    }

    public void handleAdditionStart(User user, Update update) {
        user.setGroupMode(true);
        user.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_FILE);
        userRepository.update(user);

        Announcement announcement = new Announcement();
        Group group = groupRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()));
        announcement.setGroup(group);

        announcementsUnderConstruction.getObjectsUnderConstructions().put(user.getTag(), announcement);
        announcementsUnderConstruction.getEditOrNewTask().put(user.getTag(), EditOrNew.NEW);
        announcementsUnderConstruction.getExchangers().put(user.getTag(), new Exchanger<>());

        handleAddition(InstanceAdditionStage.NONE, update, announcement);
    }

    private void markAnnouncementAsImportant(int announcementId) {
        Session session = HibernateConfig.getSession();
        Announcement announcement = announcementRepository.get(announcementId, session);
        Group group = groupRepository.get(announcement.getGroup().getId(), session);
        group.getImportantInfo().add(announcement);
        groupRepository.update(group, session);
        session.close();
    }

    public void removeAnnouncementFromImportant(int announcementId) {
        Session session = HibernateConfig.getSession();
        Announcement announcement = announcementRepository.get(announcementId, session);
        Group group = announcement.getGroup();
        group.getImportantInfo().remove(announcement);
        session.close();
        groupRepository.update(group);
    }

    public void handleAnnouncementDeletion(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        u.setGroupMode(true);
        userRepository.update(u);
        Session session = HibernateConfig.getSession();
        int id = announcementRepository.get(parseUtil.getTargetId(callbackData), session).getGroup().getId();
        session.close();
        announcementRepository.delete(parseUtil.getTargetId(callbackData));
        botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
        Message message = new Message();
        message.setText("Notification deleted");
        resultMessagesList.add(message);
        resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_ANNOUNCEMENTS_MENU_MANAGE, update, id));
    }

    public void handleAnnouncementPropertyChange(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        Announcement announcement = announcementRepository.get(parseUtil.getTargetId(callbackData));
        String propertyToChange = callbackData.substring("Change announcement".length()).trim();
        announcementsUnderConstruction.getObjectsUnderConstructions().put(parseUtil.getTag(update), announcement);

        announcementsUnderConstruction.getEditOrNewTask().put(parseUtil.getTag(update), EditOrNew.EDIT);

        if (propertyToChange.matches("title \\d+")) {
            resultMessagesList.add(menuStorage.getMenu(MenuMode.SET_TASK_IMAGE, update));
            propertyToChange = "NAME";
        } else if (propertyToChange.matches("text \\d+")) {
            resultMessagesList.add(menuStorage.getMenu(MenuMode.SET_TASK_NAME, update));
            propertyToChange = "DESCRIPTION";
        } else if (propertyToChange.matches("image \\d+")) {
            Message message = new Message();
            message.setText("Send image you want to attach");
            resultMessagesList.add(message);
            propertyToChange = "IMAGE";
        } else if (propertyToChange.matches("file \\d+")) {
            Message message = new Message();
            message.setText("Send file you want to attach");
            resultMessagesList.add(message);
            propertyToChange = "FILE";
        } else throw new RuntimeException(propertyToChange);

        u.setInstanceAdditionStage(InstanceAdditionStage.valueOf("ANNOUNCEMENT_" + propertyToChange));

        Session session = HibernateConfig.getSession();
        userRepository.update(u, session);
        session.close();
    }
    public void handleMarkAnnouncementAsImportant(Update update, List<Message> resultMessagesList, String callbackData) {
        try {
            markAnnouncementAsImportant(parseUtil.getTargetId(callbackData));
            Message message = new Message();
            message.setText("Announcement marked as important");
            resultMessagesList.add(message);
            resultMessagesList.add(menuStorage.getMenu(MenuMode.ANNOUNCEMENT_MENU_MANAGE, update, parseUtil.getTargetId(callbackData)));
        } catch (DataIntegrityViolationException e) {
            Message message = new Message();
            message.setText("Do not the bot!");
            resultMessagesList.add(message);
        }
    }

}
