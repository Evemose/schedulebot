package bot.schedulebot.storages.menustorages;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.*;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.Role;
import bot.schedulebot.repositories.GroupRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.util.ParseUtil;
import bot.schedulebot.util.generators.KeyboardGenerator;
import bot.schedulebot.util.generators.TextGenerator;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GroupMenuStorage {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final KeyboardGenerator keyboardGenerator;
    private final TextGenerator textGenerator;
    private final ParseUtil parseUtil;

    public GroupMenuStorage(UserRepository userRepository, GroupRepository groupRepository, KeyboardGenerator keyboardGenerator, TextGenerator textGenerator, ParseUtil parseUtil) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.keyboardGenerator = keyboardGenerator;
        this.textGenerator = textGenerator;
        this.parseUtil = parseUtil;
    }

    public Message getGroupsMenu(int targetId) {
        Message message = new Message();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> menuInlineKeyboard = new ArrayList<>();
        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("View my groups", "Show users groups"));
        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("Create group", "Create group"));
        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("Join group", "Show group join menu"));
        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show main menu of " + targetId));
        markup.setKeyboard(menuInlineKeyboard);

        message.setText("Choose what you want to do:");

        message.setReplyMarkup(markup);

        return message;
    }

    public Message getGroupMenu(int groupId, Update update) {
        Message message = new Message();
        Role role;

        if (update.hasCallbackQuery())
            role = groupRepository.getUserRole(userRepository.get(update.getCallbackQuery().getFrom().getUserName()).getId(), groupId);
        else
            role = groupRepository.getUserRole(userRepository.get(update.getMessage().getFrom().getUserName()).getId(), groupId);

        message.setText("*" + groupRepository.get(groupId).getName() + "*");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("See group code",
                "Show group code " + groupId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("See important info",
                "Show important info in group " + groupId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("See my appointments from this group",
                "Show appointments in group " + groupId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("See my unappointed tasks from this group",
                "Show unappointed tasks in group " + groupId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("See announcements in this group",
                "Show announcements in group " + groupId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("See people in group",
                "Show people in group " + groupId));
        if (role == Role.ADMIN || role == Role.OWNER) {
            keyboard.add(keyboardGenerator.createSingleButtonRow("Manage repeated notifications",
                    "Show notifications in group " + groupId));
            keyboard.add(keyboardGenerator.createSingleButtonRow("Manage subjects",
                    "Show subjects in group " + groupId));
            keyboard.add(keyboardGenerator.createSingleButtonRow("Manage tasks",
                    "Show tasks in group " + groupId));
            keyboard.add(keyboardGenerator.createSingleButtonRow("Manage announcements",
                    "Show announcements menu in group " + groupId));
            if (role == Role.OWNER) {
                keyboard.add(keyboardGenerator.createSingleButtonRow("Manage admins",
                        "Manage admins " + groupId));
                keyboard.add(keyboardGenerator.createSingleButtonRow("Transfer ownership",
                        "Transfer ownership in " + groupId));
                keyboard.add(keyboardGenerator.createSingleButtonRow("Delete group",
                        "Delete group " + groupId));
            }
        }
        if (role != Role.OWNER) {
            keyboard.add(keyboardGenerator.createSingleButtonRow("Leave group",
                    "Leave group " + groupId));
        }

        keyboard.add(keyboardGenerator.createSingleButtonRow("Back",
                "Show users groups"));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(inlineKeyboardMarkup);
        return message;
    }

    public void handleGroupJoinMenuProvision(Update update, List<Message> resultMessagesList) {
        Message message = getGroupJoinMenu();
        message.setText("Enter group code");
        resultMessagesList.add(message);
        User user = userRepository.get(parseUtil.getTag(update));
        user.setInstanceAdditionStage(InstanceAdditionStage.GROUP_JOIN);
        userRepository.update(user);
    }

    public Message getManageAdminsMenu(int id) {
        Message message = new Message();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Add admin", "Add admin to " + id));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Remove admin", "Remove admin from " + id));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show group " + id));

        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        message.setText("*Choose what you want to do*");
        return message;
    }

    public Message getGroupsIndexMenu(String tag) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        User user = userRepository.get(tag, session);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> menuInlineKeyboard = new ArrayList<>();

        if (user.getGroups().isEmpty()) {
            message.setText("*You are not participant of any group*");
            menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show groups menu"));
            markup.setKeyboard(menuInlineKeyboard);
            message.setReplyMarkup(markup);
            return message;
        }

        message.setText("*Groups list:*\n");

        user.getGroups().stream().forEach(group -> {
            menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow(group.getName(), "Show group " + group.getId()));
            message.setText(message.getText() + group.getName() + "\n");
        });

        menuInlineKeyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show groups menu"));

        markup.setKeyboard(menuInlineKeyboard);

        message.setText(message.getText() + "\nChoose group to view:");
        message.setReplyMarkup(markup);

        session.close();

        return message;
    }

    public Message getUsersInGroupList(int groupId, Update update) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(groupId, session);
        List<User> users = group.getUsers();

        message.setText("*Users in " + group.getName() + "*");

        users.stream().forEach(user -> {
            message.setText(message.getText() + "\n" + user.getName() + " " + groupRepository.getUserRole(user.getId(), groupId));
        });

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

//        keyboard.add(keyboardGenerator.createSingleButtonRow("Add admin", "Add admin to " + groupId));
        if (groupRepository.getUserRole(userRepository.get(parseUtil.getTag(update)).getId(), groupId) != Role.DEFAULT)
            keyboard.add(keyboardGenerator.createSingleButtonRow("Kick user", "Kick user from " + groupId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show group " + groupId));

        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public Message getUsersInGroupToAlterUser(int groupId, Update update, String callbackBase, String backButtonCallback, Role requiredRole) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        groupRepository.get(groupId, session).getUsers().forEach(user -> {
            if (!user.getTag().equals(parseUtil.getTag(update)) &&
                    (requiredRole == null || requiredRole.equals(groupRepository.getUserRole(user.getId(), groupId))))
                keyboard.add(keyboardGenerator.createSingleButtonRow(user.getName(), callbackBase + " " + groupId + " to " + user.getId()));
        });
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", backButtonCallback + " " + groupId));
        markup.setKeyboard(keyboard);

        if (keyboard.size() > 1) {
            message.setText("*Choose user:*\n");
        } else {
            message.setText("*No users in group except you*\n");
        }
        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public Message getAppointmentsInGroupList(int groupId, String tag) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(groupId, session);
        List<Appointment> appointments = group.getAppointments();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        appointments = appointments.stream()
                .filter(appointment -> appointment.getUser().getTag().equals(tag))
                .collect(Collectors.toList());

        List<List<InlineKeyboardButton>> keyboard = keyboardGenerator.getKeyboardFromAppointmentsList(appointments, groupId, true);
        markup.setKeyboard(keyboard);

        String text = textGenerator.getMessageTextFromAppointmentsList(appointments);

        message.setText(text);
        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public Message getUnappointedTasksInGroupList(int groupId, String tag) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(groupId, session);
        List<UnappointedTask> unappointedTasks = group.getUnappointedTasks();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        unappointedTasks = unappointedTasks.stream()
                .filter(unappointedTask -> unappointedTask.getUser().getTag().equals(tag))
                .collect(Collectors.toList());

        List<List<InlineKeyboardButton>> keyboard = keyboardGenerator.getKeyboardFromUnappointedTasksList(unappointedTasks, groupId, true);
        markup.setKeyboard(keyboard);

        String text = textGenerator.getMessageTextFromUnappointedTasksList(unappointedTasks);

        message.setText(text);
        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public Message getSubjectsMenu(int groupId) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(groupId, session);
        List<Subject> subjects = group.getSubjects();

        if (subjects.isEmpty()) {
            message.setText("*No subjects in group*");
        } else {
            message.setText("*Subjects in " + group.getName() + "*\n\n" + textGenerator.getMessageTextFromSubjectsList(subjects) + "*Choose option:*");
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Add subject", "Add subject to group " + groupId));
        keyboard.addAll(keyboardGenerator.getKeyboardFromSubjectsList(subjects, groupId, true));

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public Message getNotificationsInGroupMenu(int groupId) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        List<Notification> notifications = groupRepository.get(groupId, session).getRepeatedNotifications();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Add notification",
                "Add notification to " + groupId));

        String text = "*Notifications in group:*";
        for (Notification notification : notifications) {
            text += "\n" + notification.getTitle();
            keyboard.add(keyboardGenerator.createSingleButtonRow(notification.getTitle(),
                    "Show notification " + notification.getId()));
        }
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back",
                "Show group " + groupId));
        text += "\n\n*Choose option:*";

        message.setText(text);
        if (notifications.isEmpty()) {
            message.setText("*No repeated notifications in this group*");
        }

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        session.close();
        return message;
    }

    public Message getSubjectsMenuToAlterGroup(int groupId, String callBackBase) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(groupId, session);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<Subject> subjects = group.getSubjects();

        if (subjects.isEmpty()) {
            message.setText("*No subjects in group*");
        } else {
            message.setText("*Choose subject:*");
        }

        group.getSubjects().stream().forEach(subject -> {
            keyboard.add(keyboardGenerator.createSingleButtonRow(subject.getName(), callBackBase + " " + subject.getId()));
        });
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show subjects in group " + groupId));

        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public Message getGroupCodeMenu(int targetId) {
        Message message = new Message();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show group " + targetId));
        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);
        message.setText(groupRepository.get(targetId).getCode());

        return message;
    }

    public Message getGroupJoinMenu() {
        Message message = new Message();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show groups menu"));
        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);
        return message;
    }

    public Message getTasksInGroupMenu(int groupId) {
        Message message = new Message();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(groupId, session);
        List<Task> tasks = group.getTasks();

        if (tasks.isEmpty()) {
            message.setText("*No active tasks in group*\n");
        } else {
            message.setText("*Active tasks:*\n");
            tasks.stream().forEach(task -> message.setText(message.getText() + task.getName() + "\n"));
        }

        message.setText(message.getText() + "\n*Choose option: *\n");

        group.getTasks().stream().forEach(task -> {
            keyboard.add(keyboardGenerator.createSingleButtonRow(task.getName(), "Show task " + task.getId()));
        });

        keyboard.add(keyboardGenerator.createSingleButtonRow("Add task", "Add task to group " + groupId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Add personal task", "Add personal task in group " + groupId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show group " + groupId));

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public Message getAnnouncementsMenu(int groupId, boolean isManageMode) {
        Session session = HibernateConfig.getSession();
        Message message = new Message();
        Group group = groupRepository.get(groupId, session);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<Announcement> announcements = groupRepository.get(groupId, session).getAnnouncements();

        message.setText(textGenerator.getMessageTextFromAnnouncementsList(announcements) + (isManageMode || announcements.isEmpty() ? "\n*Choose option:*" : "\n*Choose which you want to view:*"));

        group.getAnnouncements().stream().forEach(announcement -> {
            keyboard.add(keyboardGenerator.createSingleButtonRow(announcement.getTitle(), "Show announcement " + (isManageMode ? "(manage) " : "") + announcement.getId()));
        });
        if (isManageMode) {
            keyboard.add(keyboardGenerator.createSingleButtonRow("Add announcement", "Add announcement to group " + groupId));
        }
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show group " + groupId));

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public Message getImportantInfo(int groupId) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(groupId, session);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        group.getImportantInfo().stream().forEach(announcement -> {
            keyboard.add(keyboardGenerator.createSingleButtonRow(announcement.getTitle(), "Show announcement (impinf) " + announcement.getId()));
        });
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show group " + groupId));

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        message.setText("*Important info of group:*");

        return message;
    }
}
