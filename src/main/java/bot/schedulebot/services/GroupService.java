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
import org.springframework.dao.DataIntegrityViolationException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class GroupService extends Service<Group> {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MenuStorage menuStorage;
    private final GroupsUnderConstruction groupAdditionHelper;
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
        this.groupAdditionHelper = groupAdditionHelper;
        this.parseUtil = parseUtil;
        this.taskService = taskService;
        this.subjectRepository = subjectRepository;
        this.announcementRepository = announcementRepository;
        this.timersStorage = timersStorage;
        this.notificationRepository = notificationRepository;
        botConfig = new BotConfig();
    }

    private List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, Group entity) {
        List<Message> messages = new ArrayList<>();
        Session session = HibernateConfig.getSession();
        User user = userRepository.get(parseUtil.getTag(update), session);
        switch (instanceAdditionStage) {
            case GROUP_START -> {
                handleGroupAdditionStart(user, session);
                messages.add(menuStorage.getMenu(MenuMode.GROUP_START, update));
            }
            case GROUP_NAME -> {
                handleGroupNameSet(update, user, session);
                messages.add(menuStorage.getMenu(MenuMode.SET_GROUP_NAME, update));
                messages.add(menuStorage.getMenu(MenuMode.GROUP_MANAGE_MENU, update, user.getGroups().get(user.getGroups().size() - 1).getId()));
            }
            default -> throw new RuntimeException("Group subject addition stage");
        }
        session.close();
        return messages;
    }

    public void handleGroupJoin(Update update) {
        Session session = HibernateConfig.getSession();
        Message message;
        if (tryAddUser(userRepository.get(parseUtil.getTag(update)).getId(), update.getMessage().getText())) {
            Group group = groupRepository.getAll(session).stream().filter(group1 -> group1.getCode().equals(update.getMessage().getText())).collect(Collectors.toList()).get(0);
            message = menuStorage.getMenu(MenuMode.GROUP_MANAGE_MENU, update, group.getId());
        } else {
            message = new Message();
            if (groupRepository.get(update.getMessage().getText(), session) == null) {
                message.setText("Wrong code. Try again");
            } else {
                message.setText("You are trying to join group you are already in");
            }
            message.setReplyMarkup(menuStorage.getMenu(MenuMode.JOIN_GROUP, update).getReplyMarkup());
        }
        botConfig.sendMessage(update.getMessage().getChatId().toString(), message);
        session.close();
        User user = userRepository.get(parseUtil.getTag(update));
        user.setInstanceAdditionStage(InstanceAdditionStage.NONE);
        userRepository.update(user);
    }

    private void handleGroupAdditionStart(User user, Session session) {
        Group group = new Group();

        user.setInstanceAdditionStage(InstanceAdditionStage.GROUP_NAME);
        group.setUserRoles(new HashMap<>());
        group.getUserRoles().put(user, Role.OWNER);
        group.setUsers(new ArrayList<>());
        group.getUsers().add(user);
        groupAdditionHelper.getObjectsUnderConstructions().put(user.getTag(), group);
        userRepository.update(user, session);

    }

    private void handleGroupNameSet(Update update, User user, Session session) {
        Group group = groupAdditionHelper.getObjectsUnderConstructions().get(user.getTag());
        group.setName(update.getMessage().getText());

        user.setInstanceAdditionStage(InstanceAdditionStage.NONE);
        groupRepository.add(group);
        userRepository.update(user, session);
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

    private boolean tryAddUser(int userId, String code) {
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(code, session);
        if (group != null) {
            try {
                userRepository.addUserToGroup(userId, group.getId());
            } catch (DataIntegrityViolationException e) {
                return false;
            }
            session.close();
            return true;
        } else {
            session.close();
            return false;
        }
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
        group.getTasks().forEach(task -> {
            taskService.forceDeleteTask(task.getId());
        });
        group.getSubjects().forEach(subject -> {
            subjectRepository.delete(subject.getId());
        });
        group.getAnnouncements().forEach(announcement -> {
            announcementRepository.delete(announcement.getId());
        });
        group.getImportantInfo().forEach(announcement -> {
            announcementRepository.delete(announcement.getId());
        });
        group.getUsers().forEach(user -> {
            kickUser(group.getId(), user.getId());
        });
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

