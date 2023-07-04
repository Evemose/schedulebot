package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Group;
import bot.schedulebot.entities.User;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.enums.Role;
import bot.schedulebot.objectsunderconstruction.GroupsUnderConstruction;
import bot.schedulebot.repositories.*;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.*;
import org.hibernate.Session;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
public class GroupService extends Service<Group> {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MenuStorage menuStorage;
    private final ParseUtil parseUtil;
    private final TaskService taskService;
    private final SubjectRepository subjectRepository;
    private final AnnouncementRepository announcementRepository;
    private final TimersStorage timersStorage;
    private final NotificationRepository notificationRepository;
    private final BotConfig botConfig;

    protected GroupService(ClassFieldsStorage classFieldsStorage, UserRepository userRepository, GroupRepository groupRepository, MenuStorage menuStorage, GroupsUnderConstruction groupAdditionHelper, ParseUtil parseUtil, TaskService taskService, SubjectRepository subjectRepository, AnnouncementRepository announcementRepository, TimersStorage timersStorage, Converter converter, NotificationRepository notificationRepository, ThreadUtil threadUtil) {
        super(groupRepository, threadUtil, parseUtil, groupAdditionHelper, menuStorage, converter, classFieldsStorage, subjectRepository, userRepository);
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.menuStorage = menuStorage;
        this.parseUtil = parseUtil;
        this.taskService = taskService;
        this.subjectRepository = subjectRepository;
        this.announcementRepository = announcementRepository;
        this.timersStorage = timersStorage;
        this.notificationRepository = notificationRepository;
        botConfig = new BotConfig();
    }
    private void transferOwnership(int groupId, int newOwnerId, int oldOwnerId) {
        groupRepository.updateUserRole(newOwnerId, Role.OWNER, groupId);
        groupRepository.updateUserRole(oldOwnerId, Role.ADMIN, groupId);
    }

    @Override
    public void handleAdditionStart(Update update) {
        User u = userRepository.get(parseUtil.getTag(update));
        u.setMode("Add");
        u.setInstanceAdditionStage(InstanceAdditionStage.GROUP_NAME);
        userRepository.update(u);
        addEntity(update, new Group());
    }

    @Override
    protected void persistEntity(Update update, Group group) {
        User user = userRepository.get(parseUtil.getTag(update));
        group.setUsers(List.of(user));
        group.setUserRoles(Map.of(user, Role.OWNER));
        super.persistEntity(update, group);
    }

    private void kickUser(int groupId, int userId) {
        userRepository.deleteUserFromGroup(userId, groupId);
    }

    private void addAdmin(int groupId, int userId) {
        groupRepository.updateUserRole(userId, Role.ADMIN, groupId);
    }

    public void handleGroupLeave(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        Message message = new Message();
        if (groupRepository.getUserRole(u.getId(), parseUtil.getTargetId(callbackData)).equals(Role.OWNER)) {
            message.setText("Owner cant leave the group");
            resultMessagesList.add(message);
        } else {
            message.setText("You have left the group");
            Session session = HibernateConfig.getSession();
            userRepository.deleteUserFromGroup(u.getId(), parseUtil.getTargetId(callbackData));
            session.close();
            resultMessagesList.add(message);
            resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUPS_MENU, update, u.getId()));
        }
    }

    public void handleOwnershipTransfer(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        transferOwnership(parseUtil.getTargetId(callbackData), parseUtil.getTargetId(callbackData, 2), u.getId());
        message.setText("*Ownership transferred.*\nYou are admin now");
        botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
        resultMessagesList.add(message);
        resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_MANAGE_MENU, update, parseUtil.getTargetId(callbackData)));
        session.close();
    }

    public void handleUserKick(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        Message message = new Message();
        kickUser(parseUtil.getTargetId(callbackData), parseUtil.getTargetId(callbackData,2));
        message.setText("User kicked");
        botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
        resultMessagesList.add(message);
        resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_MANAGE_MENU, update, parseUtil.getTargetId(callbackData)));
    }

    public void handleAdminRemoval(Update update, List<Message> resultMessagesList, String callbackData) {
        Message message = new Message();
        removeAdmin(parseUtil.getTargetId(callbackData), parseUtil.getTargetId(callbackData, 2));
        message.setText("Admin removed");
        resultMessagesList.add(message);
        resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_MANAGE_MENU, update, parseUtil.getTargetId(callbackData)));
    }

    public void handleAdminAddition(Update update, List<Message> resultMessagesList, String callbackData) {
        Message message = new Message();
        addAdmin(parseUtil.getTargetId(callbackData), parseUtil.getTargetId(callbackData, 2));
        message.setText("Admin added");
        resultMessagesList.add(message);
        resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_MANAGE_MENU, update, parseUtil.getTargetId(callbackData)));
    }

    public void handleGroupDelete(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        Message message = new Message();
        if (groupRepository.getUserRole(u.getId(), parseUtil.getTargetId(callbackData)) != null &&
                groupRepository.getUserRole(u.getId(), parseUtil.getTargetId(callbackData)).equals(Role.OWNER)) {
            deleteGroup(parseUtil.getTargetId(callbackData));
            message.setText("Group deleted");
            resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUPS_INDEX_MENU, update));
        }
        else {
            message.setText("Do not the bot");
        }
        botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
        resultMessagesList.add(0, message);
    }

    private void deleteGroup(int id) {
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(id, session);
        group.setImportantInfo(new ArrayList<>());
        groupRepository.update(group, session);
        group.getTasks().forEach(task -> taskService.forceDeleteTask(task.getId()));
        group.getSubjects().forEach(subject -> subjectRepository.delete(subject.getId()));
        group.getAnnouncements().forEach(announcement -> announcementRepository.delete(announcement.getId()));
        group.getImportantInfo().forEach(announcement -> announcementRepository.delete(announcement.getId()));
        group.getUsers().forEach(user -> kickUser(group.getId(), user.getId()));
        group.getRepeatedNotifications().forEach(notification -> {
            timersStorage.getRepeatedNotificationTimers().get(notification.getId()).cancel();
            notificationRepository.delete(notification.getId());
        });
        session.close();
        groupRepository.delete(group.getId());
    }

    private void removeAdmin(int groupId, int userId) {
        groupRepository.updateUserRole(userId, Role.DEFAULT, groupId);
    }
}

