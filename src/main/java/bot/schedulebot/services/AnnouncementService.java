package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Announcement;
import bot.schedulebot.entities.Group;
import bot.schedulebot.entities.User;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.objectsunderconstruction.AnnouncementsUnderConstruction;
import bot.schedulebot.repositories.AnnouncementRepository;
import bot.schedulebot.repositories.GroupRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.*;
import org.hibernate.Session;
import org.springframework.dao.DataIntegrityViolationException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@org.springframework.stereotype.Service
public class AnnouncementService extends Service<Announcement> {
    private final UserRepository userRepository;
    private final ParseUtil parseUtil;
    private final GroupRepository groupRepository;
    private final MenuStorage menuStorage;
    private final AnnouncementRepository announcementRepository;
    private final Notificator notificator;
    private final BotConfig botConfig;

    protected AnnouncementService(ClassFieldsStorage classFieldsStorage, AnnouncementsUnderConstruction announcementsUnderConstruction, UserRepository userRepository, ParseUtil parseUtil, GroupRepository groupRepository, MenuStorage menuStorage, Converter converter, AnnouncementRepository announcementRepository, Notificator notificator, ThreadUtil threadUtil) {
        super(announcementRepository, threadUtil, parseUtil, announcementsUnderConstruction, menuStorage, converter, classFieldsStorage, null, userRepository);
        this.userRepository = userRepository;
        this.parseUtil = parseUtil;
        this.groupRepository = groupRepository;
        this.menuStorage = menuStorage;
        this.announcementRepository = announcementRepository;
        this.notificator = notificator;
        this.botConfig = new BotConfig();
    }

    @Override
    protected void persistEntity(Update update, Announcement announcement) {
        super.persistEntity(update, announcement);
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(announcement.getGroup().getId(), session);
        notificator.sendNotificationsToGroupUsersAboutNewAnnouncement(group.getUsers(), update, announcement);
        session.close();
    }

    public void handleAdditionStart(User user, Update update) {
        user.setGroupMode(true);
        user.setMode("Add");
        user.setInstanceAdditionStage(InstanceAdditionStage.ANNOUNCEMENT_FILE);
        userRepository.update(user);

        Announcement announcement = new Announcement();
        Group group = groupRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()));
        announcement.setGroup(group);

        addEntity(update, announcement);
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
    public void handleMarkAnnouncementAsImportant(Update update, List<Message> resultMessagesList, String callbackData) {
        try {
            markAnnouncementAsImportant(parseUtil.getTargetId(callbackData));
            Message message = new Message();
            message.setText("Announcement marked as important");
            resultMessagesList.add(message);
            resultMessagesList.add(menuStorage.getMenu(MenuMode.ANNOUNCEMENT_MANAGE_MENU, update, parseUtil.getTargetId(callbackData)));
        } catch (DataIntegrityViolationException e) {
            Message message = new Message();
            message.setText("Do not the bot!");
            resultMessagesList.add(message);
        }
    }




}
