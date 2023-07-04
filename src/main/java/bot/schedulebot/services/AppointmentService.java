package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.*;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.objectsunderconstruction.AppointmentsUnderConstruction;
import bot.schedulebot.repositories.*;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.ClassFieldsStorage;
import bot.schedulebot.util.Converter;
import bot.schedulebot.util.ParseUtil;
import bot.schedulebot.util.ThreadUtil;
import bot.schedulebot.util.generators.KeyboardGenerator;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.springframework.dao.DataIntegrityViolationException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Exchanger;

@org.springframework.stereotype.Service
public class AppointmentService extends Service<Appointment> {
    private final AppointmentsUnderConstruction appointmentObjectsUnderConstruction;
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

    protected AppointmentService(ClassFieldsStorage classFieldsStorage, AppointmentsUnderConstruction appointmentAdditionHelper, Converter converter, AppointmentRepository appointmentRepository, UnappointedTaskRepository unappointedTaskRepository, ParseUtil parseUtil, UserRepository userRepository, MenuStorage menuStorage, KeyboardGenerator keyboardGenerator, TodayTasksInfoRepository todayTasksInfoRepository, ThreadUtil threadUtil, TodayTasksInfoService todayTasksInfoService, TaskRepository taskRepository, AppointmentsUnderConstruction appointmentsUnderConstruction) {
        super(appointmentRepository, threadUtil, parseUtil, appointmentsUnderConstruction, menuStorage, converter, classFieldsStorage, null, userRepository);
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
    }

    void handleAdditionStart(Update update, UnappointedTask unappointedTask) {
        User user = userRepository.get(parseUtil.getTag(update));
        user.setMode("Add");
        user.setInstanceAdditionStage(InstanceAdditionStage.APPOINTMENT_START);
        userRepository.update(user);
        addEntity(update, new Appointment(unappointedTask));
    }

    @Override
    public void handleAdditionStart(Update update) {
        Session session = HibernateConfig.getSession();
        User user = userRepository.get(parseUtil.getTag(update));
        user.setMode("Add");
        user.setGroupMode(update.getCallbackQuery().getData().contains(" group "));
        user.setInstanceAdditionStage(InstanceAdditionStage.APPOINTMENT_START);
        userRepository.update(user);
        addEntity(update,
                new Appointment(unappointedTaskRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()),
                        session)));
        session.close();
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
            session.close();
        }
    }

    public void handleAppointmentDateChange(Update update, String callbackData, User u) {
        u.setGroupMode(callbackData.contains("group"));
        u.setInstanceAdditionStage(InstanceAdditionStage.APPOINTMENT_DATE);
        u.setMode("Edit");
        userRepository.update(u);
        appointmentObjectsUnderConstruction.getEditExchangers().put(u.getTag(), new Exchanger<>());
        editEntity(update, "date");
    }

    @SneakyThrows
    @Override
    protected Message inspectValueSuitability(Class<?> fieldType, Object value, Appointment t) {
        if (fieldType.equals(LocalDate.class)) {
            Field field = Task.class.getDeclaredField("deadline");
            field.setAccessible(true);
            LocalDate deadline = (LocalDate) field.get(t.getTask());
            if (((LocalDate)value).isAfter(deadline)) {
                Message message = new Message();
                message.setText("Date is after deadline. Are you sure you want to appoint it?");
                message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getYesNoKeyboard()));
                return message;
            } else if (((LocalDate)value).isBefore(LocalDate.now())) {
                Message message = new Message();
                message.setText("Date cannot be before today");
                return message;
            }
        }
        return super.inspectValueSuitability(fieldType, value, t);
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

        if (!Objects.requireNonNull(text).contains("Appointed on")) {
            appointUnappointedTaskForDeadline(resultMessagesList, update, session, callbackData, todayTasksInfo);
        } else { //in case task being appointed from appointment menu and not unappointed task menu
            appointAppointmentForDeadline(resultMessagesList, update, session, callbackData, u);
        }
        todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
        session.close();
    }

    private void appointAppointmentForDeadline(List<Message> resultMessagesList, Update update, Session session, String callbackData, User u) {
        Appointment appointment;
        appointment = appointmentRepository.get(parseUtil.getTargetId(callbackData), session);
        appointment.setDate(appointment.getTask().getDeadline());
        appointmentRepository.update(appointment);
        try {
            botConfig.editMessage(u.getChatId(),
                    update.getCallbackQuery().getMessage().getMessageId(),
                    menuStorage.getMenu(MenuMode.SHOW_APPOINTMENT_NO_ATTACHEMETS, update, appointment.getId()));
        } catch (RuntimeException ignored) {
            // in case current appointed date equals previous
        }
        resultMessagesList.add(menuStorage.getMenu(MenuMode.SET_APPOINTMENT_DATE, update));
    }

    private void appointUnappointedTaskForDeadline(List<Message> resultMessagesList, Update update, Session session, String callbackData, TodayTasksInfo todayTasksInfo) {
        Appointment appointment;
        UnappointedTask unappointedTask = unappointedTaskRepository.get(parseUtil.getTargetId(callbackData), session);

        if (unappointedTask != null) {
            appointment = new Appointment(unappointedTask);
            appointment.setDate(unappointedTask.getTask().getDeadline());
            try {
                unappointedTaskRepository.delete(parseUtil.getTargetId(callbackData));
            } catch (DataIntegrityViolationException e) { // related to tasks info of use
                todayTasksInfoService.resetTodayTasksInfo(todayTasksInfo);
                unappointedTaskRepository.delete(parseUtil.getTargetId(callbackData), session);
            }
            appointmentRepository.add(appointment);
            resultMessagesList.add(menuStorage.getMenu(MenuMode.APPOINTMENT_MANAGE_MENU, update, appointment.getId()));
        }
    }

    public void appointTaskForTomorrow(Update update) {
        User u = userRepository.get(parseUtil.getTag(update));
        Appointment appointment = appointmentRepository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()));
        u.setGroupMode(appointment.getGroup() != null);
        userRepository.update(u);
        appointment.setDate(LocalDate.now().plusDays(1));
        appointmentRepository.update(appointment);

        Session session = HibernateConfig.getSession();
        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(u.getTag(), session);
        todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
        session.close();
        try {
            botConfig.editMessage(u.getChatId(),
                    update.getCallbackQuery().getMessage().getMessageId(),
                    menuStorage.getMenu(MenuMode.SHOW_APPOINTMENT_NO_ATTACHEMETS, update, appointment.getId()));
        } catch (RuntimeException ignored) {
            // in case current appointed date equals previous
        }
    }

    @Override
    public void persistEntity(Update update, Appointment appointment) {
        super.persistEntity(update, appointment);
        Session session = HibernateConfig.getSession();
        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(parseUtil.getTag(update), session);
        todayTasksInfoService.updateTodayTasksInfo(todayTasksInfo, session);
        session.close();
    }

}
