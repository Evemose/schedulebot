package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Appointment;
import bot.schedulebot.entities.TodayTasksInfo;
import bot.schedulebot.entities.UnappointedTask;
import bot.schedulebot.entities.User;
import bot.schedulebot.repositories.TodayTasksInfoRepository;
import bot.schedulebot.storages.menustorages.AppointmentMenuStorage;
import bot.schedulebot.storages.menustorages.UnappointedTaskMenuStorage;
import bot.schedulebot.util.generators.KeyboardGenerator;
import bot.schedulebot.util.generators.TextGenerator;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TodayTasksInfoService {
    private final TodayTasksInfoRepository todayTasksInfoRepository;
    private final BotConfig botConfig;
    private final AppointmentMenuStorage appointmentMenuStorage;
    private final UnappointedTaskMenuStorage unappointedTaskMenuStorage;
    private final TextGenerator textGenerator;
    private final KeyboardGenerator keyboardGenerator;

    public TodayTasksInfoService(TodayTasksInfoRepository todayTasksInfoRepository, AppointmentMenuStorage appointmentMenuStorage, UnappointedTaskMenuStorage unappointedTaskMenuStorage, TextGenerator textGenerator, KeyboardGenerator keyboardGenerator) {
        this.todayTasksInfoRepository = todayTasksInfoRepository;
        botConfig = new BotConfig();
        this.appointmentMenuStorage = appointmentMenuStorage;
        this.unappointedTaskMenuStorage = unappointedTaskMenuStorage;
        this.textGenerator = textGenerator;
        this.keyboardGenerator = keyboardGenerator;
    }

    public void resetTodayTasksInfo(TodayTasksInfo todayTasksInfo) {
        todayTasksInfo.setUnappointedTasksWithDeadlineToday(new ArrayList<>());
        todayTasksInfo.setAppointmentsForToday(new ArrayList<>());
        todayTasksInfo.setAppointmentsWithDeadlineToday(new ArrayList<>());
        todayTasksInfo.setOutdatedAppointments(new ArrayList<>());
        todayTasksInfo.setOutdatedUnappointedTasks(new ArrayList<>());
        todayTasksInfoRepository.update(todayTasksInfo);
    }

    public void updateTodayTasksInfo(TodayTasksInfo todayTasksInfo, Session session) {
        if (todayTasksInfo != null) {
            User user = todayTasksInfo.getUser();
            List<Appointment> appointments = user.getAppointments();
            List<UnappointedTask> unappointedTasks = user.getUnappointedTasks();

            todayTasksInfo.setAppointmentsForToday(appointments.stream()
                    .filter(appointment -> appointment.getAppointedDate().isEqual(LocalDate.now()))
                    .toList());
            todayTasksInfo.setAppointmentsWithDeadlineToday(appointments.stream()
                    .filter(appointment -> appointment.getTask().getDeadline().isEqual(LocalDate.now()) && !todayTasksInfo.getAppointmentsForToday().contains(appointment))
                    .toList());
            todayTasksInfo.setUnappointedTasksWithDeadlineToday(unappointedTasks.stream()
                    .filter(unappointedTask -> unappointedTask.getTask().getDeadline().isEqual(LocalDate.now()))
                    .toList());
            todayTasksInfo.setOutdatedUnappointedTasks(unappointedTasks.stream()
                    .filter(unappointedTask -> unappointedTask.getTask().getDeadline().isBefore(LocalDate.now()))
                    .toList());
            todayTasksInfo.setOutdatedAppointments(appointments.stream()
                    .filter(appointment -> appointment.getAppointedDate().isBefore(LocalDate.now()))
                    .toList());

            todayTasksInfoRepository.update(todayTasksInfo, session);
        }
    }

    public void deleteAllTasksInfoMessages() {
        Session session = HibernateConfig.getSession();
        todayTasksInfoRepository.getAll(session).stream().forEach(todayTasksInfo -> {
            if (todayTasksInfo.getUser() != null)
                botConfig.deleteMessage(todayTasksInfo.getUser().getChatId(), todayTasksInfo.getMessageId());
        });
        session.close();
    }

    public void updateTodayTasksInfoMessage(TodayTasksInfo todayTasksInfo) {
        try {
            switch (todayTasksInfo.getMode()) {
                case MAIN -> {
                    Message message = new Message();
                    message.setText(textGenerator.getMessageTextFromTodayTasksInfo(todayTasksInfo));
                    message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getEverydayTaskNotificationKeyboard(todayTasksInfo)));
                    botConfig.editMessageText(todayTasksInfo.getUser().getChatId(), todayTasksInfo.getMessageId(), message);
                }
                case APPOINTMENTS_FOR_TODAY -> {
                    appointmentMenuStorage.getAppointmentsForTodayMenu(todayTasksInfo.getId());
                }
                case APPOINTMENTS_WITH_DEADLINE_TODAY -> {
                    appointmentMenuStorage.getAppointmentsWithDeadlineTodayMenu(todayTasksInfo.getId());
                }
                case UNAPPOINTED_TASKS_WITH_DEADLINE_TODAY -> {
                    unappointedTaskMenuStorage.getUnappointedTasksWithDeadlineTodayMenu(todayTasksInfo.getId());
                }
                case OUTDATED_APPOINTMENTS -> {
                    appointmentMenuStorage.getOutdatedAppointmentsMenu(todayTasksInfo.getId());
                }
                case OUTDATED_UNAPPOINTED_TASKS -> {
                    unappointedTaskMenuStorage.getOutdatedUnappointedTasksMenu(todayTasksInfo.getId());
                }
                default -> {
                    throw new RuntimeException("Wrong today tasks mode");
                }
            }
            botConfig.pinMessage(todayTasksInfo.getUser().getChatId(), todayTasksInfo.getMessageId());
        } catch (RuntimeException e) { // message hasn`t been edited

        }
    }
}
