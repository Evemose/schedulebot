package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.*;
import bot.schedulebot.enums.EditOrNew;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.enums.TaskType;
import bot.schedulebot.objectsunderconstruction.AppointmentsUnderConstruction;
import bot.schedulebot.objectsunderconstruction.TasksUnderConstruction;
import bot.schedulebot.repositories.*;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.Converter;
import bot.schedulebot.util.Notificator;
import bot.schedulebot.util.ParseUtil;
import bot.schedulebot.util.generators.KeyboardGenerator;
import bot.schedulebot.util.generators.TextGenerator;
import org.hibernate.Session;
import org.springframework.dao.DataIntegrityViolationException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.io.File;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
public class TaskService extends Service<Task> {
    private final UserRepository userRepository;
    private final ParseUtil parseUtil;
    private final GroupRepository groupRepository;
    private final TasksUnderConstruction taskAdditionHelper;
    private final SubjectRepository subjectRepository;
    private final UnappointedTaskRepository unappointedTaskRepository;
    private final MenuStorage menuStorage;
    private final TaskRepository taskRepository;
    private final AppointmentRepository appointmentRepository;
    private final Converter converter;
    private final FileRepository fileRepository;
    private final AppointmentsUnderConstruction appointmentsUnderConstruction;
    private final Notificator notificator;
    private final TodayTasksInfoService todayTasksInfoService;
    private final TodayTasksInfoRepository todayTasksInfoRepository;
    private final TextGenerator textGenerator;
    private final KeyboardGenerator keyboardGenerator;
    private final BotConfig botConfig;
    private final TasksUnderConstruction tasksUnderConstruction;

    public TaskService(UserRepository userRepository, ParseUtil parseUtil, GroupRepository groupRepository, TasksUnderConstruction taskAdditionHelper, SubjectRepository subjectRepository, UnappointedTaskRepository unappointedTaskRepository, MenuStorage menuStorage, TaskRepository taskRepository, AppointmentRepository appointmentRepository, Converter converter, FileRepository fileRepository, AppointmentsUnderConstruction appointmentsUnderConstruction, Notificator notificator, TodayTasksInfoService todayTasksInfoService, TodayTasksInfoRepository todayTasksInfoRepository, TextGenerator textGenerator, KeyboardGenerator keyboardGenerator, TasksUnderConstruction tasksUnderConstruction) {
        super(taskRepository, parseUtil, tasksUnderConstruction);
        this.userRepository = userRepository;
        this.parseUtil = parseUtil;
        this.groupRepository = groupRepository;
        this.taskAdditionHelper = taskAdditionHelper;
        this.subjectRepository = subjectRepository;
        this.unappointedTaskRepository = unappointedTaskRepository;
        this.menuStorage = menuStorage;
        this.taskRepository = taskRepository;
        this.appointmentRepository = appointmentRepository;
        this.converter = converter;
        this.fileRepository = fileRepository;
        this.appointmentsUnderConstruction = appointmentsUnderConstruction;
        this.notificator = notificator;
        this.todayTasksInfoService = todayTasksInfoService;
        this.todayTasksInfoRepository = todayTasksInfoRepository;
        this.textGenerator = textGenerator;
        this.keyboardGenerator = keyboardGenerator;
        this.botConfig = new BotConfig();
        this.tasksUnderConstruction = tasksUnderConstruction;
    }

    public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, Task entity) {
        List<Message> messages = new ArrayList<>();
        User user = userRepository.get(parseUtil.getTag(update));

        switch (instanceAdditionStage) {
            case TASK_START -> {
                handleTaskStart(update, user);
                messages.add(menuStorage.getMenu(MenuMode.ADD_TASK_TO_GROUP, update));
            }
            case TASK_FILE -> {
                handleTaskDocumentSet(update, user);
                messages.add(menuStorage.getMenu(MenuMode.SET_TASK_DOCUMENT, update));
            }
            case TASK_IMAGE_NO_FILE -> {
                user.setInstanceAdditionStage(InstanceAdditionStage.TASK_IMAGE);
                Session session = HibernateConfig.getSession();
                userRepository.update(user, session);
                session.close();
                messages.add(menuStorage.getMenu(MenuMode.SET_TASK_IMAGE_SKIP_FILE, update));
            }
            case TASK_IMAGE -> {
                handleTaskImageSet(update, user);
                messages.add(menuStorage.getMenu(MenuMode.SET_TASK_IMAGE, update));
            }
            case TASK_NAME_NO_IMAGE -> {
                user.setInstanceAdditionStage(InstanceAdditionStage.TASK_NAME);
                Session session = HibernateConfig.getSession();
                userRepository.update(user, session);
                session.close();
                messages.add(menuStorage.getMenu(MenuMode.SET_TASK_NAME_SKIP_IMAGE, update));
            }
            case TASK_NAME -> {
                handleTaskNameSet(update, user);
                messages.add(menuStorage.getMenu(MenuMode.SET_TASK_NAME, update));
            }
            case TASK_DESCRIPTION -> {
                handleTaskDescriptionSet(update, user);
                messages.add(menuStorage.getMenu(MenuMode.SET_TASK_SUBJECT_LIST, update, user.isGroupMode() ? taskAdditionHelper.getObjectsUnderConstructions().get(user.getTag()).getGroup().getId() : -1));
            }
            case TASK_SUBJECT -> {
                try {
                    handleTaskSubjectSet(update, user);
                    messages.add(menuStorage.getMenu(MenuMode.SET_TASK_SUBJECT, update));
                } catch (NullPointerException e) {
                    //in case someone accidentally pressed button not while adding task
                }
            }
            case TASK_DEADLINE -> {
                Task task = taskAdditionHelper.getObjectsUnderConstructions().get(user.getTag());
                try {
                    handleTaskDeadlineSet(update, user);
                } catch (DateTimeException e) {
                    Message message = new Message();
                    message.setText("Invalid date. Try again");
                    messages.add(message);
                    return messages;
                }
                if (appointmentsUnderConstruction.getObjectsUnderConstructions().get(user.getTag()) != null) {
                    user.setInstanceAdditionStage(InstanceAdditionStage.APPOINTMENT_DATE);
                    userRepository.update(user);
                    messages.add(menuStorage.getMenu(MenuMode.APPOINT_TASK, update));
                } else {
                    Session session = HibernateConfig.getSession();
                    todayTasksInfoRepository.getAll(session).stream().forEach(todayTasksInfo -> {
                        todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
                        todayTasksInfoService.updateTodayTasksInfoMessage(todayTasksInfo);
                    });
                    session.close();
                    messages.add(menuStorage.getMenu(MenuMode.SET_TASK_DEADLINE, update));
                    messages.add(menuStorage.getMenu(MenuMode.SHOW_GROUP_TASKS, update, task.getGroup().getId()));
                }
                taskAdditionHelper.getEditOrNewTask().remove(parseUtil.getTag(update));
                taskAdditionHelper.getTaskTargets().remove(parseUtil.getTag(update));
                taskAdditionHelper.getTaskTypes().remove(parseUtil.getTag(update));
                taskAdditionHelper.getObjectsUnderConstructions().remove(parseUtil.getTag(update));
            }
        }
        if (taskAdditionHelper.getEditOrNewTask().get(parseUtil.getTag(update)) != null &&
                taskAdditionHelper.getEditOrNewTask().get(parseUtil.getTag(update)).equals(EditOrNew.EDIT)) {
            user.setInstanceAdditionStage(InstanceAdditionStage.NONE);
            userRepository.update(user);
            Task task = taskAdditionHelper.getObjectsUnderConstructions().get(parseUtil.getTag(update));
            taskRepository.update(task);
            messages.clear();
            Message message = new Message();
            message.setText("Task changed");
            messages.add(message);
            taskAdditionHelper.getObjectsUnderConstructions().remove(parseUtil.getTag(update));
            taskAdditionHelper.getMessageIds().remove(parseUtil.getTag(update));
            messages.add(menuStorage.getMenu(MenuMode.TASK_EDIT_MENU, update, task.getId()));
        }
        return messages;
    }

    private void handleTaskDocumentSet(Update update, User user) {
        bot.schedulebot.entities.File taskFile = parseUtil.getMessageFile(update);
        taskAdditionHelper.getObjectsUnderConstructions().get(user.getTag()).setFile(taskFile);
        user.setInstanceAdditionStage(InstanceAdditionStage.TASK_IMAGE);
        userRepository.update(user);
    }

    private void handleTaskImageSet(Update update, User user) {
        File image = parseUtil.getMessageImage(update);
        taskAdditionHelper.getObjectsUnderConstructions().get(user.getTag()).setImage(converter.convertFileToJsonString(image));
        user.setInstanceAdditionStage(InstanceAdditionStage.TASK_NAME);
        userRepository.update(user);
        image.delete();
    }

    private void handleTaskStart(Update update, User user) {
        Task task = new Task();

        appointmentsUnderConstruction.getObjectsUnderConstructions().remove(user.getTag());

        task.setGroup(groupRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData(), (taskAdditionHelper.getTaskTypes().get(parseUtil.getTag(update)).equals(TaskType.PERSONAL) && userRepository.get(parseUtil.getTag(update)).isGroupMode()) ? 2 : 1)));
        taskAdditionHelper.getObjectsUnderConstructions().put(user.getTag(), task);

        user.setInstanceAdditionStage(InstanceAdditionStage.TASK_IMAGE);

        userRepository.update(user);
    }

    private void handleTaskNameSet(Update update, User user) {
        taskAdditionHelper.getObjectsUnderConstructions().get(user.getTag()).setName(update.getMessage().getText());

        user.setInstanceAdditionStage(InstanceAdditionStage.TASK_DESCRIPTION);

        userRepository.update(user);
    }

    private void handleTaskDescriptionSet(Update update, User user) {
        taskAdditionHelper.getObjectsUnderConstructions().get(user.getTag()).setDescription(update.getMessage().getText());

        user.setInstanceAdditionStage(InstanceAdditionStage.TASK_SUBJECT);

        userRepository.update(user);
    }

    private void handleTaskSubjectSet(Update update, User user) {
        taskAdditionHelper.getObjectsUnderConstructions().get(user.getTag()).setSubject(subjectRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData())));
        user.setInstanceAdditionStage(InstanceAdditionStage.TASK_DEADLINE);
        userRepository.update(user);
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
            resultMessagesList.add(menuStorage.getMenu(MenuMode.SHOW_GROUP_TASKS, update, groupId));
        } else {
            message.setText("Task still has not been completed by some users. Do you want to force delete it (it will delete all associated appointments)?" +
                    "\n\nUsers that still have not completed task:\n\n" +
                    textGenerator.getStringOfUsersWithTask(taskId));
            message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getForceDeleteTaskKeyboard(task)));
        }
        botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
        resultMessagesList.add(0, message);
    }

    private void handleTaskDeadlineSet(Update update, User user) {
        Task task = taskAdditionHelper.getObjectsUnderConstructions().get(user.getTag());

        task.setDeadline(LocalDate.parse(update.getMessage().getText(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        if (task.getDeadline().isBefore(LocalDate.now())) throw new DateTimeException("Date before current time");

        user.setInstanceAdditionStage(InstanceAdditionStage.NONE);
        taskAdditionHelper.getObjectsUnderConstructions().remove(user.getTag());

        userRepository.update(user);
        Session session = HibernateConfig.getSession();
        if (task.getFile() != null) fileRepository.add(task.getFile());
        taskRepository.add(task, session);

        if (user.isGroupMode()) {
            Group group = groupRepository.get(task.getGroup().getId(), session);

            if (taskAdditionHelper.getTaskTypes().get(user.getTag()).equals(TaskType.GROUP)) {
                group.getUsers().stream().forEach(u -> {
                    UnappointedTask unappointedTask = new UnappointedTask();
                    unappointedTask.setTask(task);
                    unappointedTask.setGroup(group);
                    unappointedTask.setUser(u);
                    unappointedTaskRepository.add(unappointedTask);
                    group.getUnappointedTasks().add(unappointedTask);
                    TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(u.getTag(), session);
                    todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
                    todayTasksInfoService.updateTodayTasksInfoMessage(todayTasksInfo);
                    groupRepository.update(group, session);
                });
                notificator.sendNotificationsToGroupUsersAboutNewAppointment(group, update, task);
            } else if (appointmentsUnderConstruction.getObjectsUnderConstructions().get(user.getTag()) == null) {
                User u = userRepository.get(taskAdditionHelper.getTaskTargets().get(user.getTag()), session);
                UnappointedTask unappointedTask = new UnappointedTask();
                unappointedTask.setTask(task);
                unappointedTask.setGroup(group);
                unappointedTask.setUser(u);
                unappointedTaskRepository.add(unappointedTask);
                notificator.sendPersonalNotificationAboutNewTask(user, unappointedTask);
            }
        }
        session.close();
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
        forceDeleteTask(parseUtil.getTargetId(callbackData));
        Message message = new Message();
        message.setText("Task deleted");
        botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
        resultMessagesList.add(message);
        resultMessagesList.add(menuStorage.getMenu(MenuMode.SHOW_GROUP_TASKS, update, groupId));
    }

    public void handleTaskPropertyChange(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        Task task = taskRepository.get(parseUtil.getTargetId(callbackData));
        String propertyToChange = callbackData.substring("Change task ".length());
        tasksUnderConstruction.getObjectsUnderConstructions().put(parseUtil.getTag(update), task);
        tasksUnderConstruction.getMessageIds().put(u.getTag(), update.getCallbackQuery().getMessage().getMessageId());

        tasksUnderConstruction.getEditOrNewTask().put(parseUtil.getTag(update), EditOrNew.EDIT);
        if (propertyToChange.matches("name \\d+")) {
            resultMessagesList.add(menuStorage.getMenu(MenuMode.SET_TASK_IMAGE, update));
            propertyToChange = "NAME";
        } else if (propertyToChange.matches("description \\d+")) {
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
        } else if (propertyToChange.matches("deadline \\d+")) {
            resultMessagesList.add(menuStorage.getMenu(MenuMode.SET_TASK_SUBJECT, update));
        } else throw new RuntimeException(propertyToChange);

        u.setInstanceAdditionStage(InstanceAdditionStage.valueOf("TASK_" + propertyToChange));

        Session session = HibernateConfig.getSession();
        userRepository.update(u, session);
        session.close();
    }

    public void handleTaskSetDocumentYes(List<Message> resultMessagesList, User u) {
        u.setInstanceAdditionStage(InstanceAdditionStage.TASK_FILE);
        Session session = HibernateConfig.getSession();
        userRepository.update(u, session);
        session.close();
        Message message = new Message();
        message.setText("Send document you want to attach");
        resultMessagesList.add(message);
    }

    public void handleTaskSetImageYes(List<Message> resultMessagesList, User u) {
        u.setInstanceAdditionStage(InstanceAdditionStage.TASK_IMAGE);
        Session session = HibernateConfig.getSession();
        userRepository.update(u, session);
        session.close();
        Message message = new Message();
        message.setText("Send image you want to attach");
        resultMessagesList.add(message);
    }
    public void handleTaskAdditionToGroup(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        u.setGroupMode(true);
        userRepository.update(u);
        Session session = HibernateConfig.getSession();
        tasksUnderConstruction.getTaskTypes().put(update.getCallbackQuery().getFrom().getUserName(), TaskType.GROUP);
        tasksUnderConstruction.getEditOrNewTask().put(parseUtil.getTag(update), EditOrNew.NEW);
        if (groupRepository.get(parseUtil.getTargetId(callbackData), session).getSubjects().size() < 1) {
            resultMessagesList.add(menuStorage.getMenu(MenuMode.NO_SUBJECTS_FOR_TASK_MENU, update));
        } else {
            resultMessagesList.addAll(handleAddition(InstanceAdditionStage.TASK_START, update, null));
        }
        session.close();
    }

    public void forceDeleteTask(int taskId) {
        Session session = HibernateConfig.getSession();
        List<User> users = taskRepository.get(taskId, session).getGroup().getUsers();

        users.forEach(user -> todayTasksInfoService.resetTodayTasksInfo(user.getTodayTasksInfo()));

        appointmentRepository.getAppointmentsOfTask(taskId).stream().forEach(appointment -> appointmentRepository.delete(appointment.getId()));

        unappointedTaskRepository.getUnappointedTasksOfTask(taskId).stream().forEach(unappointedTask -> unappointedTaskRepository.delete(unappointedTask.getId()));

        taskRepository.delete(taskId);

        users.forEach(user -> {
            todayTasksInfoService.updateTodayTasksInfo(user.getTodayTasksInfo(), session);
            todayTasksInfoService.updateTodayTasksInfoMessage(user.getTodayTasksInfo());
        });

        session.close();
    }

    public void handlePersonalTaskAddition(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        u.setGroupMode(true);
        userRepository.update(u);
        Session session = HibernateConfig.getSession();
        tasksUnderConstruction.getTaskTypes().put(parseUtil.getTag(update), TaskType.PERSONAL);
        tasksUnderConstruction.getTaskTargets().put(parseUtil.getTag(update), userRepository.get(parseUtil.getTargetId(callbackData)).getTag());
        tasksUnderConstruction.getEditOrNewTask().put(parseUtil.getTag(update), EditOrNew.NEW);
        if (groupRepository.get(parseUtil.getTargetId(callbackData, 2), session).getSubjects().size() < 1) {
            resultMessagesList.add(menuStorage.getMenu(MenuMode.NO_SUBJECTS_FOR_TASK_MENU, update));
        } else {
            resultMessagesList.addAll(handleAddition(InstanceAdditionStage.TASK_START, update, null));
        }
        session.close();
    }
}

