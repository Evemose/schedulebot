package bot.schedulebot.storages.menustorages;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Subject;
import bot.schedulebot.entities.User;
import bot.schedulebot.objectsunderconstruction.TasksUnderConstruction;
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

@Component
public class SubjectMenuStorage {
    private final UserRepository userRepository;
    private final KeyboardGenerator keyboardGenerator;
    private final TextGenerator textGenerator;
    private final TasksUnderConstruction tasksUnderConstruction;
    private final ParseUtil parseUtil;

    public SubjectMenuStorage(UserRepository userRepository, KeyboardGenerator keyboardGenerator, TextGenerator textGenerator, TasksUnderConstruction tasksUnderConstruction, ParseUtil parseUtil) {
        this.userRepository = userRepository;
        this.keyboardGenerator = keyboardGenerator;
        this.textGenerator = textGenerator;
        this.tasksUnderConstruction = tasksUnderConstruction;
        this.parseUtil = parseUtil;
    }

    public Message getUserSubjectsListToAlterUser(String tag, String callBackBase) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        User user = userRepository.get(tag, session);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        user.getSubjects().stream().forEach(subject -> {
            keyboard.add(keyboardGenerator.createSingleButtonRow(subject.getName(), callBackBase + " " + subject.getId()));
        });
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show subjects of " + user.getId()));

        markup.setKeyboard(keyboard);

        message.setText("*Choose subject:*");
        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public Message getSubjectsMenu(String tag) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        User user = userRepository.get(tag, session);
        List<Subject> subjects = user.getSubjects();

        message.setText("*Subjects of " + user.getName() + "*\n\n" + textGenerator.getMessageTextFromSubjectsList(subjects));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Add subject", "Add subject to user " + user.getId()));
        keyboard.addAll(keyboardGenerator.getKeyboardFromSubjectsList(subjects, user.getId(), false));

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public Message getNoSubjectsForTaskMenu(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Message message = new Message();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Add subject", "Add subject to group " + parseUtil.getTargetId(callbackData)));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show group " + parseUtil.getTargetId(callbackData)));
        markup.setKeyboard(keyboard);

        message.setText("No subjects in group. Cant create task");
        message.setReplyMarkup(markup);
        return message;
    }

    public Message getNoSubjectForPersonalAppointmentMenu(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Message message = new Message();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Add subject", "Add subject to user " + parseUtil.getTargetId(callbackData)));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show group " + parseUtil.getTargetId(callbackData)));
        markup.setKeyboard(keyboard);

        message.setText("*No subjects of user. Cant add appointment*");
        message.setReplyMarkup(markup);
        return message;
    }

    public Message getSubjectMenu(int subjectId) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Subject subject = session.get(Subject.class, subjectId);

        message.setText("*Subject " + subject.getName() + "*\n\n" + textGenerator.getMessageTextFromSubject(subject));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Edit", "Edit subject " + subject.getId()));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Delete", "Delete subject " + subject.getId()));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back",
                subject.getUser() == null ? "Show subjects in group " + subject.getGroup().getId() :
                        "Show subjects of " + subject.getUser().getId()));

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        session.close();
        return message;
    }
}
