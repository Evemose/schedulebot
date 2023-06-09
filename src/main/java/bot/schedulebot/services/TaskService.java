package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.*;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.objectsunderconstruction.TasksUnderConstruction;
import bot.schedulebot.repositories.*;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.*;
import bot.schedulebot.util.generators.KeyboardGenerator;
import bot.schedulebot.util.generators.TextGenerator;
import org.hibernate.Session;
import org.springframework.dao.DataIntegrityViolationException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalDate;
import java.util.List;

@org.springframework.stereotype.Service
public class TaskService extends Service<Task> {
    private final UserRepository userRepository;
    private final ParseUtil parseUtil;
    private final GroupRepository groupRepository;
    private final UnappointedTaskRepository unappointedTaskRepository;
    private final MenuStorage menuStorage;
    private final TaskRepository taskRepository;
    private final AppointmentRepository appointmentRepository;
    private final Notificator notificator;
    private final TodayTasksInfoService todayTasksInfoService;
    private final TodayTasksInfoRepository todayTasksInfoRepository;
    private final TextGenerator textGenerator;
    private final KeyboardGenerator keyboardGenerator;
    private final BotConfig botConfig;
    private final AppointmentService appointmentService;

    protected TaskService(ClassFieldsStorage classFieldsStorage, UserRepository userRepository, ParseUtil parseUtil, GroupRepository groupRepository, SubjectRepository subjectRepository, UnappointedTaskRepository unappointedTaskRepository, MenuStorage menuStorage, TaskRepository taskRepository, AppointmentRepository appointmentRepository, Converter converter, Notificator notificator, TodayTasksInfoService todayTasksInfoService, TodayTasksInfoRepository todayTasksInfoRepository, TextGenerator textGenerator, KeyboardGenerator keyboardGenerator, TasksUnderConstruction tasksUnderConstruction, ThreadUtil threadUtil, AppointmentService appointmentService) {
        super(taskRepository, threadUtil, parseUtil, tasksUnderConstruction, menuStorage, converter, classFieldsStorage, subjectRepository, userRepository);
        this.userRepository = userRepository;
        this.parseUtil = parseUtil;
        this.groupRepository = groupRepository;
        this.unappointedTaskRepository = unappointedTaskRepository;
        this.menuStorage = menuStorage;
        this.taskRepository = taskRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificator = notificator;
        this.todayTasksInfoService = todayTasksInfoService;
        this.todayTasksInfoRepository = todayTasksInfoRepository;
        this.textGenerator = textGenerator;
        this.keyboardGenerator = keyboardGenerator;
        this.botConfig = new BotConfig();
        this.appointmentService = appointmentService;
    }

    public void handleTaskDeletion(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        Message message = new Message();
        int taskId = parseUtil.getTargetId(callbackData);
        Session session = HibernateConfig.getSession();
        Task task = taskRepository.get(taskId, session);
        int groupId = task.getGroup().getId();
        session.close();
        if (tryDeleteTask(parseUtil.getTargetId(callbackData))) {
            message.setText("Task deleted");
            botConfig.editMessage(u.getChatId(), Task.getTaskMenus().get(u.getTag()), menuStorage.getMenu(MenuMode.SHOW_GROUP_TASKS, update, groupId));
        } else {
            message.setText("Task still has not been completed by some users. Do you want to force delete it (it will delete all associated appointments)?" +
                    "\n\nUsers that still have not completed task:\n\n" +
                    textGenerator.getUsersWithTask(taskId));
            message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getForceDeleteTaskKeyboard(task)));
        }
        botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
        resultMessagesList.add(message);
        todayTasksInfoService.updateTodayTasksInfoInGroup(groupId);
    }

    public boolean tryDeleteTask(int taskId) {
        try {
            taskRepository.delete(taskId);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    public void handleTaskForceDeletion(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        Session session = HibernateConfig.getSession();
        int groupId = taskRepository.get(parseUtil.getTargetId(callbackData), session).getGroup().getId();
        session.close();
        Session session1 = HibernateConfig.getSession();
        forceDeleteTask(parseUtil.getTargetId(callbackData));
        Message message = new Message();
        message.setText("Task deleted");
        botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
        resultMessagesList.add(message);
        botConfig.editMessage(u.getChatId(), Task.getTaskMenus().get(u.getTag()), menuStorage.getMenu(MenuMode.SHOW_GROUP_TASKS, update, groupId));
        todayTasksInfoService.updateTodayTasksInfo(todayTasksInfoRepository.get(u.getTag(), session1), session1);
    }

    public void forceDeleteTask(int taskId) {
        Session session = HibernateConfig.getSession();
        List<User> users = taskRepository.get(taskId, session).getGroup().getUsers();
        users.forEach(user -> todayTasksInfoService.resetTodayTasksInfo(user.getTodayTasksInfo()));
        appointmentRepository.getAppointmentsOfTask(taskId).forEach(appointment -> appointmentRepository.delete(appointment.getId()));
        unappointedTaskRepository.getUnappointedTasksOfTask(taskId).forEach(unappointedTask -> unappointedTaskRepository.delete(unappointedTask.getId()));
        taskRepository.delete(taskId);
        session.close();
    }

    public void handleGroupPersonalTaskAddition(Update update, String callbackData, User u) {
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()), session);
        if (group.getSubjects().isEmpty()) {
            botConfig.sendMessage(parseUtil.getTag(update), menuStorage.getMenu(MenuMode.NO_SUBJECTS_FOR_TASK_MENU, update, group.getId()));
            return;
        }
        u.setGroupMode(true);
        u.setMode("Add");
        u.setInstanceAdditionStage(InstanceAdditionStage.TASK_START);
        userRepository.update(u);
        Task task = new Task();
        task.setGroup(groupRepository.get(parseUtil.getTargetId(callbackData)));
        task.setTargetUser(userRepository.get(parseUtil.getTargetId(callbackData, 2)));
        session.close();
        addEntity(update, task);
    }

    public void handleAdditionStart(Update update) {
        Session session = HibernateConfig.getSession();
        Group group = groupRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()), session);
        if (group.getSubjects().isEmpty()) {
            botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(), menuStorage.getMenu(MenuMode.NO_SUBJECTS_FOR_TASK_MENU, update));
            return;
        }
        Task task = new Task();
        User user = userRepository.get(parseUtil.getTag(update));
        user.setMode("Add");
        user.setInstanceAdditionStage(InstanceAdditionStage.TASK_START);
        userRepository.update(user);
        task.setGroup(groupRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData())));
        session.close();
        addEntity(update, task);
    }

    public void handlePersonalTaskAddition(Update update) {
        Session session = HibernateConfig.getSession();
        User user = userRepository.get(parseUtil.getTag(update), session);
        user.setMode("Add");
        user.setInstanceAdditionStage(InstanceAdditionStage.TASK_START);
        user.setGroupMode(false);
        userRepository.update(user, session);
        if (user.getSubjects().isEmpty()) {
            botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(), menuStorage.getMenu(MenuMode.NO_SUBJECTS_FOR_PERSONAL_APPOINTMENT, update));
            return;
        }
        session.close();
        addEntity(update, new Task());
    }
    @Override
    protected void persistEntity(Update update, Task task) {
        if (task.getGroup() != null) {
            super.persistEntity(update, task);
            persistGroupTask(update, task);
        } else {
            taskRepository.add(task);
            persistPersonalTask(update, task);
        }
    }

    private void persistPersonalTask(Update update, Task task) {
        UnappointedTask unappointedTask = new UnappointedTask(task);
        unappointedTask.setUser(userRepository.get(parseUtil.getTag(update)));
        unappointedTaskRepository.add(unappointedTask);
        Session session = HibernateConfig.getSession();
        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(parseUtil.getTag(update), session);
        todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
        session.close();
        appointmentService.handleAdditionStart(update, unappointedTask);
    }

    @Override
    protected Message inspectValueSuitability(Class<?> fieldType, Object value, Task t) {
        Message message = new Message();
        if (fieldType.equals(LocalDate.class) && ((LocalDate)value).isBefore(LocalDate.now())) {
            message.setText("Date can't be in the past");
            return message;
        }
        return super.inspectValueSuitability(fieldType, value, t);
    }

    private void persistGroupTask(Update update, Task task) {
        if (task.getTargetUser() != null) {
            UnappointedTask unappointedTask = new UnappointedTask(task);
            unappointedTask.setUser(task.getTargetUser());
            unappointedTaskRepository.add(unappointedTask);
            notificator.sendPersonalNotificationAboutNewTask(task.getTargetUser(), unappointedTask);
            Session session = HibernateConfig.getSession();
            todayTasksInfoService.updateTodayTasksInfo(todayTasksInfoRepository.get(task.getTargetUser().getTag(), session), session);
            session.close();
        } else {
            Session session = HibernateConfig.getSession();
            Group group = groupRepository.get(task.getGroup().getId(), session);
            group.getUsers().forEach(user -> {
                UnappointedTask unappointedTask = new UnappointedTask(task);
                unappointedTask.setUser(user);
                unappointedTaskRepository.add(unappointedTask);
                if (!user.getTag().equals(parseUtil.getTag(update))) {
                    notificator.sendPersonalNotificationAboutNewTask(user, unappointedTask);
                }
            });
            todayTasksInfoService.updateTodayTasksInfoInGroup(group.getId());
            session.close();
        }
    }

}

