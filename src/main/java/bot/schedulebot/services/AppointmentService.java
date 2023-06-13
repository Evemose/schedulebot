package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.*;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.enums.TaskType;
import bot.schedulebot.objectsunderconstruction.AppointmentsUnderConstruction;
import bot.schedulebot.objectsunderconstruction.ObjectsUnderConstruction;
import bot.schedulebot.objectsunderconstruction.TasksUnderConstruction;
import bot.schedulebot.repositories.*;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.ParseUtil;
import bot.schedulebot.util.generators.KeyboardGenerator;
import org.hibernate.Session;
import org.springframework.dao.DataIntegrityViolationException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
public class AppointmentService extends Service<Appointment> {
    private final ObjectsUnderConstruction appointmentObjectsUnderConstruction;
    private final AppointmentRepository appointmentRepository;
    private final UnappointedTaskRepository unappointedTaskRepository;
    private final ParseUtil parseUtil;
    private final UserRepository userRepository;
    private final MenuStorage menuStorage;
    private final KeyboardGenerator keyboardGenerator;
    private final TodayTasksInfoRepository todayTasksInfoRepository;
    private final BotConfig botConfig;
    private final TodayTasksInfoService todayTasksInfoService;
    private final TaskRepository taskRepository;
    private final TasksUnderConstruction tasksUnderConstruction;
    private final AppointmentsUnderConstruction appointmentsUnderConstruction;

    public AppointmentService(AppointmentsUnderConstruction appointmentAdditionHelper, AppointmentRepository appointmentRepository, UnappointedTaskRepository unappointedTaskRepository, ParseUtil parseUtil, UserRepository userRepository, MenuStorage menuStorage, KeyboardGenerator keyboardGenerator, TodayTasksInfoRepository todayTasksInfoRepository, TodayTasksInfoService todayTasksInfoService, TaskRepository taskRepository, TasksUnderConstruction tasksUnderConstruction, AppointmentsUnderConstruction appointmentsUnderConstruction) {
        super(appointmentRepository, parseUtil, appointmentsUnderConstruction);
        this.appointmentObjectsUnderConstruction = appointmentAdditionHelper;
        this.appointmentRepository = appointmentRepository;
        this.unappointedTaskRepository = unappointedTaskRepository;
        this.parseUtil = parseUtil;
        this.userRepository = userRepository;
        this.menuStorage = menuStorage;
        this.keyboardGenerator = keyboardGenerator;
        this.todayTasksInfoRepository = todayTasksInfoRepository;
        botConfig = new BotConfig();
        this.todayTasksInfoService = todayTasksInfoService;
        this.taskRepository = taskRepository;
        this.tasksUnderConstruction = tasksUnderConstruction;
        this.appointmentsUnderConstruction = appointmentsUnderConstruction;
    }

    @Override
    public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, Appointment entity) {
        List<Message> messages = new ArrayList<>();

        switch (instanceAdditionStage) {
            case APPOINTMENT_START -> {
                handleAppointmentAdditionStart(update);
                messages.add(menuStorage.getMenu(MenuMode.APPOINT_TASK, update));
            }
            case APPOINTMENT_DATE -> {
                Session session = HibernateConfig.getSession();
                User user = userRepository.get(parseUtil.getTag(update), session);
                Appointment appointment = (Appointment) appointmentObjectsUnderConstruction.getObjectsUnderConstructions().get(user.getTag());
                try {
                    handleAppointmentDateSet(update, user, appointment, session, messages);
                } catch (DateTimeException e) {
                    Message message = new Message();
                    message.setText("Wrong date. Try again");
                    messages.add(message);
                }
                if (!messages.isEmpty()) {
                    session.close();
                    break;
                }
                messages.add(menuStorage.getMenu(MenuMode.SET_APPOINTMENT_DATE, update));
                messages.add(menuStorage.getMenu(MenuMode.SHOW_APPOINTMENT, update, appointment.getId()));
                session.close();
                appointmentObjectsUnderConstruction.getObjectsUnderConstructions().remove(parseUtil.getTag(update));
            }
            default -> throw new RuntimeException();
        }

        return messages;
    }

    private void handleAppointmentAdditionStart(Update update) {
        User user = userRepository.get(parseUtil.getTag(update));

        if (!appointmentObjectsUnderConstruction.getObjectsUnderConstructions().containsKey(parseUtil.getTag(update))) {
            Appointment appointment = new Appointment();
            Task task = unappointedTaskRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData())).getTask();
            appointment.setTask(task);
            appointment.setUser(user);
            appointment.setGroup(task.getGroup());
            appointmentObjectsUnderConstruction.getObjectsUnderConstructions().put(update.getCallbackQuery().getFrom().getUserName(), appointment);
        }
        user.setInstanceAdditionStage(InstanceAdditionStage.APPOINTMENT_DATE);

        userRepository.update(user);

    }

    private void handleAppointmentDateSet(Update update, User user, Appointment appointment, Session session, List<Message> messages) {
        if (appointment.getAppointedDate() == null || appointment.getId() == -1) {
            appointment.setId(0);
            appointment.setAppointedDate(LocalDate.parse(update.getMessage().getText(), DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            if (appointment.getAppointedDate().isAfter(appointment.getTask().getDeadline())) {
                messages.add(createYesNoKeyboard());
                return;
            }
        }
        if (appointment.getAppointedDate().isBefore(LocalDate.now())) {
            appointment.setId(-1);
            throw new DateTimeException("Invalid date");
        }

        appointmentObjectsUnderConstruction.getObjectsUnderConstructions().remove(user.getTag());
        UnappointedTask unappointedTask = unappointedTaskRepository.getUnappointedTaskByTaskAndUser(appointment.getTask().getId(), user.getId());
        appointmentRepository.add(appointment);

        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(user.getTag(), session);
        try {
            if (unappointedTask != null) // if appointment is personal unappointed task isn`t being created
                unappointedTaskRepository.delete(unappointedTask.getId());
        } catch (
                DataIntegrityViolationException e) { // in case unappointed task is related to some TodayTasksInfo entity
            todayTasksInfoService.resetTodayTasksInfo(todayTasksInfo);
            unappointedTaskRepository.delete(unappointedTask.getId());
        }
        todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
        todayTasksInfoService.updateTodayTasksInfoMessage(todayTasksInfo);
        user.setInstanceAdditionStage(InstanceAdditionStage.NONE);

        userRepository.update(user, session);
        //groupRepository.addGroupAppointment(appointment.getGroup().getId(), appointmentRepository.getAppointmentByTaskAndUser(appointment.getTask().getId(), user.getId()).getId());
    }

    private Message createYesNoKeyboard() {
        Message message = new Message();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<String> text = new ArrayList<>();
        List<String> callbackData = new ArrayList<>();
        text.add("Yes");
        text.add("No");
        callbackData.add("Task appointment yes");
        callbackData.add("Task appointment no");
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(keyboardGenerator.createManyButtonsRow(text, callbackData));
        markup.setKeyboard(keyboard);
        message.setText("Date is after deadline. Are you sure?");
        message.setReplyMarkup(markup);
        return message;
    }

    private void markAppointmentAsDone(int id) {
        Session session = HibernateConfig.getSession();
        Appointment appointment = appointmentRepository.get(id, session);
        try {
            boolean isPersonalAppointment = appointment.getGroup() == null;
            int taskId = appointment.getTask().getId();
            session.close();
            appointmentRepository.delete(id);
            if (isPersonalAppointment) {
                taskRepository.delete(taskId);
            }
        } catch (DataIntegrityViolationException e) { // in case appointment related to tasks info
            TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(appointment.getUser().getTag(), session);
            todayTasksInfoService.resetTodayTasksInfo(todayTasksInfo);
            appointmentRepository.delete(id);
            todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
            todayTasksInfoService.updateTodayTasksInfoMessage(todayTasksInfo);
            session.close();
        }
    }

    public void handleAppointmentAdditionInitiation(Update update, List<Message> resultMessagesList, User u) {
        Session session = HibernateConfig.getSession();
        User user = userRepository.get(parseUtil.getTag(update), session);
        user.setGroupMode(false);
        userRepository.update(user, session);
        tasksUnderConstruction.getTaskTypes().put(update.getCallbackQuery().getFrom().getUserName(), TaskType.PERSONAL);
        if (user.getSubjects().isEmpty()) {
            session.close();
            resultMessagesList.add(menuStorage.getMenu(MenuMode.NO_SUBJECTS_FOR_PERSONAL_APPOINTMENT, update));
        } else {
            resultMessagesList.addAll(handleAddition(InstanceAdditionStage.TASK_START, update, null));
            Appointment appointment = new Appointment();
            appointment.setUser(u);
            appointment.setTask(tasksUnderConstruction.getObjectsUnderConstructions().get(u.getTag()));
            appointmentsUnderConstruction.getObjectsUnderConstructions().put(u.getTag(), appointment);
        }
    }

    public void handleAppointmentDateChange(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        u.setGroupMode(callbackData.matches("Change appointed date \\(group\\) \\d+"));
        userRepository.update(u);
        resetAppointment(parseUtil.getTargetId(callbackData));
        resultMessagesList.addAll(handleAddition(InstanceAdditionStage.APPOINTMENT_START, update, null));
    }

    public void handleMarkAppointmentAsDone(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        Session session = HibernateConfig.getSession();
        Appointment appointment = appointmentRepository.get(parseUtil.getTargetId(callbackData), session);
        int groupId = -123;
        if (u.isGroupMode()) groupId = appointment.getGroup().getId();
        session.close();
        markAppointmentAsDone(parseUtil.getTargetId(callbackData));
        resultMessagesList.add(menuStorage.getMenu(MenuMode.MARK_APPOINTMENT_AS_DONE, update, parseUtil.getTargetId(callbackData)));
        if (u.isGroupMode()) {
            resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_APPOINTMENTS_MENU, update, groupId));
        } else {
            resultMessagesList.add(menuStorage.getMenu(MenuMode.USER_APPOINTMENTS_LIST, update, u.getId()));
        }
    }

    private void resetAppointment(int id) {
        Session session = HibernateConfig.getSession();
        Appointment appointment = appointmentRepository.get(id, session);
        UnappointedTask unappointedTask = new UnappointedTask();
        unappointedTask.setUser(appointment.getUser());
        unappointedTask.setGroup(appointment.getGroup());
        unappointedTask.setTask(appointment.getTask());
        appointment.setId(-1);
        appointmentObjectsUnderConstruction.getObjectsUnderConstructions().put(appointment.getUser().getTag(), appointment);
        unappointedTaskRepository.add(unappointedTask);
        session.close();

        try {
            appointmentRepository.delete(id);
        } catch (DataIntegrityViolationException e) {
            Session session1 = HibernateConfig.getSession();
            TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(appointment.getUser().getTag(), session1);
            todayTasksInfoService.resetTodayTasksInfo(todayTasksInfo);
            appointmentRepository.delete(id);
            todayTasksInfoService.updateTodayTasksInfoMessage(todayTasksInfo);
            session1.close();
        }
    }

    public void appointTaskForDeadline(List<Message> resultMessagesList, Update update) {
        Session session = HibernateConfig.getSession();
        String text, callbackData = update.getCallbackQuery().getData();
        User u = userRepository.get(parseUtil.getTag(update), session);
        Message message = update.getCallbackQuery().getMessage();
        if (message.hasText()) {
            text = message.getText();
        } else if (message.hasPhoto() || message.hasDocument()) {
            text = message.getCaption();
        } else {
            text = null;
        }

        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(u.getTag(), session);
        Appointment appointment;

        if (!text.contains("Appointed date")) {
            UnappointedTask unappointedTask = unappointedTaskRepository.get(parseUtil.getTargetId(callbackData), session);

            appointment = new Appointment();

            if (unappointedTask != null) {
                appointment.setTask(unappointedTask.getTask());
                appointment.setGroup(unappointedTask.getGroup());
                appointment.setUser(unappointedTask.getUser());
                appointment.setAppointedDate(unappointedTask.getTask().getDeadline());
                try {
                    unappointedTaskRepository.delete(parseUtil.getTargetId(callbackData));
                } catch (DataIntegrityViolationException e) { // related to tasks info of use
                    todayTasksInfoService.resetTodayTasksInfo(todayTasksInfo);
                    unappointedTaskRepository.delete(parseUtil.getTargetId(callbackData), session);
                }
                appointmentRepository.add(appointment);
                resultMessagesList.add(menuStorage.getMenu(MenuMode.SHOW_APPOINTMENT, update, appointment.getId()));
            }
        } else { //in case task being appointed from appointment menu and not unappointed task menu
            appointment = appointmentRepository.get(parseUtil.getTargetId(callbackData), session);
            appointment.setAppointedDate(appointment.getTask().getDeadline());
            appointmentRepository.update(appointment);
            try {
                botConfig.editMessageText(u.getChatId(),
                        update.getCallbackQuery().getMessage().getMessageId(),
                        menuStorage.getMenu(MenuMode.SHOW_APPOINTMENT_NO_ATTACHEMETS, update, appointment.getId()));
            } catch (RuntimeException e) {
                // in case current appointed date equals previous
            }
            resultMessagesList.add(menuStorage.getMenu(MenuMode.SET_APPOINTMENT_DATE, update));
        }

        if (todayTasksInfo != null) {
            todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
            todayTasksInfoService.updateTodayTasksInfoMessage(todayTasksInfo);
        }

        session.close();
    }

    public void appointTaskForTomorrow(Update update) {
        User u = userRepository.get(parseUtil.getTag(update));
        Appointment appointment = appointmentRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()));
        appointment.setAppointedDate(LocalDate.now().plusDays(1));
        appointmentRepository.update(appointment);

        Session session = HibernateConfig.getSession();
        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(u.getTag(), session);
        if (todayTasksInfo != null) {
            todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
            todayTasksInfoService.updateTodayTasksInfoMessage(todayTasksInfo);
        }
        session.close();

        try {
            botConfig.editMessageText(u.getChatId(),
                    update.getCallbackQuery().getMessage().getMessageId(),
                    menuStorage.getMenu(MenuMode.SHOW_APPOINTMENT_NO_ATTACHEMETS, update, appointment.getId()));
        } catch (RuntimeException e) {
            // in case current appointed date equals previous
        }
    }

}
