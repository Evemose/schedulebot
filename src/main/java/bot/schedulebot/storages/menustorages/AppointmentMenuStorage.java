package bot.schedulebot.storages.menustorages;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Appointment;
import bot.schedulebot.entities.TodayTasksInfo;
import bot.schedulebot.entities.User;
import bot.schedulebot.repositories.AppointmentRepository;
import bot.schedulebot.repositories.TodayTasksInfoRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.util.Converter;
import bot.schedulebot.util.ParseUtil;
import bot.schedulebot.util.generators.FileGenerator;
import bot.schedulebot.util.generators.KeyboardGenerator;
import bot.schedulebot.util.generators.TextGenerator;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.File;
import java.util.List;

@Component
public class AppointmentMenuStorage {
    private final AppointmentRepository appointmentRepository;
    private final KeyboardGenerator keyboardGenerator;
    private final TextGenerator textGenerator;
    private final BotConfig botConfig;
    private final Converter converter;
    private final FileGenerator fileGenerator;
    private final ParseUtil parseUtil;
    private final UserRepository userRepository;
    private final TodayTasksInfoRepository todayTasksInfoRepository;

    public AppointmentMenuStorage(AppointmentRepository appointmentRepository, KeyboardGenerator keyboardGenerator, TextGenerator textGenerator, Converter converter, FileGenerator fileGenerator, ParseUtil parseUtil, UserRepository userRepository, TodayTasksInfoRepository todayTasksInfoRepository) {
        this.appointmentRepository = appointmentRepository;
        this.keyboardGenerator = keyboardGenerator;
        this.textGenerator = textGenerator;
        this.converter = converter;
        this.fileGenerator = fileGenerator;
        this.botConfig = new BotConfig();
        this.parseUtil = parseUtil;
        this.userRepository = userRepository;
        this.todayTasksInfoRepository = todayTasksInfoRepository;
    }

    public Message getAppointmentsMenu(int userId) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        User user = userRepository.get(userId, session);
        List<Appointment> appointments = user.getAppointments();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> keyboard = keyboardGenerator.getKeyboardFromAppointmentsList(appointments, userId, false);
        List<InlineKeyboardButton> backButton = keyboard.remove(keyboard.size() - 1);
        keyboard.add(keyboardGenerator.createSingleButtonRow("Add appointment", "Add appointment to " + userId));
        keyboard.add(backButton);
        String text = textGenerator.getMessageTextFromAppointmentsList(appointments);

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        message.setText(text);

        session.close();
        return message;
    }

    public Message getAppointmentMenu(int appointmentId, Update update, boolean hasBackButton, boolean isGroupMode) {
        Session session = HibernateConfig.getSession();
        Appointment appointment = appointmentRepository.get(appointmentId, session);
        User u = userRepository.get(parseUtil.getTag(update));
        u.setGroupMode(appointment.getGroup() != null);
        userRepository.update(u);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboardGenerator.getAppointmentKeyboard(appointment, hasBackButton, isGroupMode));

        if (appointment.getTask().getFile() != null) {
            File file = fileGenerator.getFileFromByteArray(appointment.getTask().getFile().getFile(),
                    appointment.getTask().getName() + appointment.getTask().getFile().getFileType());
            if (hasBackButton) {
                botConfig.sendDocument(update.getCallbackQuery().getMessage().getChatId().toString(),
                        new InputFile(file),
                        "*Attachment*");
            } else {
                botConfig.sendDocument(update.getCallbackQuery().getMessage().getChatId().toString(),
                        new InputFile(file),
                        "*Attachment*",
                        new InlineKeyboardMarkup(keyboardGenerator.getDeleteButtonKeyboard()));
            }
            file.delete();
        }

        if (appointment.getTask().getImage() != null) {
            File file = converter.convertJsonStringToFile(appointment.getTask().getImage());
            botConfig.sendPhoto(parseUtil.getChatId(update),
                    new InputFile(file),
                    textGenerator.getAppointmentMenuText(appointment),
                    markup);
            file.delete();
        } else {
            session.close();
            return getAppointmentMenuWithoutAttachments(appointmentId, hasBackButton, isGroupMode);
        }
        session.close();
        return null;
    }

    public Message getAppointmentMenuWithoutAttachments(int appointmentId, boolean hasBackButton, boolean isGroupMode) {
        Session session = HibernateConfig.getSession();
        Message message = new Message();
        Appointment appointment = appointmentRepository.get(appointmentId, session);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboardGenerator.getAppointmentKeyboard(appointment, hasBackButton, isGroupMode));
        message.setReplyMarkup(markup);
        message.setText(textGenerator.getAppointmentMenuText(appointment));
        session.close();
        return message;
    }

    public void getAppointmentsForTodayMenu(int tasksInfoId) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(tasksInfoId, session);

        message.setText(textGenerator.getAppointmentsForTodayText(todayTasksInfo));
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getAppointmentsForTodayKeyboard(todayTasksInfo)));

        botConfig.editMessage(todayTasksInfo.getUser().getChatId(), todayTasksInfo.getMessageId(), message);
        session.close();
    }

    public void getAppointmentsWithDeadlineTodayMenu(int tasksInfoId) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(tasksInfoId, session);

        message.setText(textGenerator.getAppointmentsWithDeadlineTodayText(todayTasksInfo));
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getAppointmentsWithDeadlineTodayKeyboard(todayTasksInfo)));

        botConfig.editMessage(todayTasksInfo.getUser().getChatId(), todayTasksInfo.getMessageId(), message);
        session.close();
    }

    public Message getMarkAsDoneMenu() {
        Message message = new Message();
        message.setText("*Well done!*");
        return message;
    }

    public void getOutdatedAppointmentsMenu(int tasksInfoId) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(tasksInfoId, session);

        message.setText(textGenerator.getOutdatedAppointmentsText(todayTasksInfo));
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getOutdatedAppointmentsKeyboard(todayTasksInfo)));

        botConfig.editMessage(todayTasksInfo.getUser().getChatId(), todayTasksInfo.getMessageId(), message);
        session.close();
    }

}
