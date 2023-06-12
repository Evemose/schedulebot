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
import org.hibernate.Session;
import org.springframework.dao.DataIntegrityViolationException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
public class AnnouncementService implements Service {

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

    public AnnouncementService(AnnouncementsUnderConstruction announcementsUnderConstruction, UserRepository userRepository, ParseUtil parseUtil, GroupRepository groupRepository, MenuStorage menuStorage, Converter converter, AnnouncementRepository announcementRepository, Notificator notificator, FileRepository fileRepository) {
        this.announcementsUnderConstruction = announcementsUnderConstruction;
        this.userRepository = userRepository;
        this.parseUtil = parseUtil;
        this.groupRepository = groupRepository;
        this.menuStorage = menuStorage;
        this.converter = converter;
        this.announcementRepository = announcementRepository;
        this.notificator = notificator;
        this.fileRepository = fileRepository;
        this.botConfig = new BotConfig();
    }

    @Override
    public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, Object entity) {
        User user = userRepository.get(parseUtil.getTag(update));
        List<Message> messages = new ArrayList<>();
        switch (instanceAdditionStage) {
            case ANNOUNCEMENT_START -> {
                handleAnnouncementAdditionStart(user, update);
                messages.add(menuStorage.getMenu(MenuMode.ADD_ANNOUNCEMENT, update));
            }
            case ANNOUNCEMENT_FILE -> {
                handleAnnouncementFileSet(user, update);
                messages.add(menuStorage.getMenu(MenuMode.SET_ANNOUNCEMENT_FILE, update));
            }
            case ANNOUNCEMENT_IMAGE_SKIP_FILE -> {
                user.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_IMAGE);
                userRepository.update(user);
                messages.add(menuStorage.getMenu(MenuMode.SET_ANNOUNCEMENT_IMAGE_SKIP_FILE, update));
            }
            case ANNOUNCEMENT_IMAGE -> {
                handleAnnouncementImageSet(user, update);
                messages.add(menuStorage.getMenu(MenuMode.SET_ANNOUNCEMENT_IMAGE, update));
            }
            case ANNOUNCEMENT_TITLE_SKIP_IMAGE -> {
                user.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_TITLE);
                userRepository.update(user);
                messages.add(menuStorage.getMenu(MenuMode.SET_ANNOUNCEMENT_TITLE_SKIP_IMAGE, update));
            }
            case ANNOUNCEMENT_TITLE -> {
                handleAnnouncementTitleSet(user, update);
                messages.add(menuStorage.getMenu(MenuMode.SET_ANNOUNCEMENT_TITLE, update));
            }
            case ANNOUNCEMENT_TEXT -> {
                handleAnnouncementTextSet(user, update);
                persistAnnouncement(user, update);
                messages.add(menuStorage.getMenu(MenuMode.SET_ANNOUNCEMENT_TEXT, update));
                messages.add(menuStorage.getMenu(MenuMode.GROUP_ANNOUNCEMENTS_MENU_MANAGE, update,
                        announcementsUnderConstruction.getObjectsUnderConstructions().get(parseUtil.getTag(update)).getGroup().getId()));
                announcementsUnderConstruction.getObjectsUnderConstructions().remove(parseUtil.getTag(update));
            }
        }
        if (announcementsUnderConstruction.getEditOrNewTask().get(parseUtil.getTag(update)) != null &&
                announcementsUnderConstruction.getEditOrNewTask().get(parseUtil.getTag(update)).equals(EditOrNew.EDIT)) {
            user.setInstanceAdditionStage(InstanceAdditionStage.NONE);
            userRepository.update(user);
            Announcement announcement = announcementsUnderConstruction.getObjectsUnderConstructions().get(parseUtil.getTag(update));
            announcementRepository.update(announcement);
            messages.clear();
            Message message = new Message();
            message.setText("Announcement changed");
            messages.add(message);
            messages.add(menuStorage.getMenu(MenuMode.ANNOUNCEMENT_EDIT_MENU, update, announcement.getId()));
            announcementsUnderConstruction.getObjectsUnderConstructions().remove(parseUtil.getTag(update));
        }
        return messages;
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

    private void handleAnnouncementTextSet(User user, Update update) {
        user.setInstanceAdditionStage(InstanceAdditionStage.NONE);
        userRepository.update(user);
        announcementsUnderConstruction.getObjectsUnderConstructions().get(user.getTag()).setText(update.getMessage().getText());
    }

    private void handleAnnouncementTitleSet(User user, Update update) {
        user.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_TEXT);
        userRepository.update(user);
        announcementsUnderConstruction.getObjectsUnderConstructions().get(user.getTag()).setTitle(update.getMessage().getText());
    }

    private void handleAnnouncementImageSet(User user, Update update) {
        user.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_TITLE);
        userRepository.update(user);
        announcementsUnderConstruction.getObjectsUnderConstructions().get(user.getTag()).setImage(converter.convertFileToJsonString(parseUtil.getMessageImage(update)));
    }

    private void handleAnnouncementFileSet(User user, Update update) {
        user.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_IMAGE);
        userRepository.update(user);
        announcementsUnderConstruction.getObjectsUnderConstructions().get(user.getTag()).setFile(parseUtil.getMessageFile(update));
    }

    private void handleAnnouncementAdditionStart(User user, Update update) {
        Announcement announcement = new Announcement();
        Group group = groupRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()));
        announcement.setGroup(group);
        announcementsUnderConstruction.getObjectsUnderConstructions().put(user.getTag(), announcement);
        announcementsUnderConstruction.getEditOrNewTask().put(user.getTag(), EditOrNew.NEW);
        user.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_FILE);
        userRepository.update(user);
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

    public void handleAnnouncementSetDocumentYes(List<Message> resultMessagesList, User u) {
        u.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_FILE);
        Session session = HibernateConfig.getSession();
        userRepository.update(u, session);
        session.close();
        Message message = new Message();
        message.setText("Send document you want to attach");
        resultMessagesList.add(message);
    }

    public void handleAnnouncementSetImageYes(List<Message> resultMessagesList, User u) {
        u.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_IMAGE);
        Session session = HibernateConfig.getSession();
        userRepository.update(u, session);
        session.close();
        Message message = new Message();
        message.setText("Send image you want to attach");
        resultMessagesList.add(message);
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
