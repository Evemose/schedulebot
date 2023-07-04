package bot.schedulebot.storages.menustorages;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.TodayTasksInfo;
import bot.schedulebot.entities.UnappointedTask;
import bot.schedulebot.entities.User;
import bot.schedulebot.repositories.TodayTasksInfoRepository;
import bot.schedulebot.repositories.UnappointedTaskRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.util.Converter;
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
public class UnappointedTaskMenuStorage {
    private final UnappointedTaskRepository unappointedTaskRepository;
    private final KeyboardGenerator keyboardGenerator;
    private final TextGenerator textGenerator;
    private final UserRepository userRepository;
    private final Converter converter;
    private final BotConfig botConfig;
    private final FileGenerator fileGenerator;
    private final TodayTasksInfoRepository todayTasksInfoRepository;

    public UnappointedTaskMenuStorage(UnappointedTaskRepository unappointedTaskRepository, KeyboardGenerator keyboardGenerator, TextGenerator textGenerator, UserRepository userRepository, Converter converter, FileGenerator fileGenerator, TodayTasksInfoRepository todayTasksInfoRepository) {
        this.unappointedTaskRepository = unappointedTaskRepository;
        this.keyboardGenerator = keyboardGenerator;
        this.textGenerator = textGenerator;
        this.userRepository = userRepository;
        this.converter = converter;
        this.fileGenerator = fileGenerator;
        this.botConfig = new BotConfig();
        this.todayTasksInfoRepository = todayTasksInfoRepository;
    }

    public Message getUnappointedTaskMenu(int unappointedTaskId, Update update, boolean hasBackButton) {
        Session session = HibernateConfig.getSession();
        UnappointedTask unappointedTask = unappointedTaskRepository.get(unappointedTaskId, session);
        Message message = new Message();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardGenerator.getUnappointedTaskKeyboard(unappointedTask, hasBackButton));

        message.setReplyMarkup(markup);

        if (unappointedTask.getTask().getFile() != null) {
            File file = fileGenerator.getFileFromByteArray(unappointedTask.getTask().getFile().getFile(),
                    unappointedTask.getTask().getName() + unappointedTask.getTask().getFile().getFileType());
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

        if (unappointedTask.getTask().getImage() != null) {
            botConfig.sendPhoto(update.getCallbackQuery().getMessage().getChatId().toString(),
                    new InputFile(converter.convertJsonStringToFile(unappointedTask.getTask().getImage())),
                    unappointedTask.toString(),
                    markup);
            message = null;
        } else {
            message.setText(unappointedTask.toString());
            message.setReplyMarkup(markup);
        }

        session.close();
        return message;
    }

    public Message getUnappointedTasksMenu(String tag) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        User user = userRepository.get(tag, session);
        List<UnappointedTask> unappointedTasks = user.getUnappointedTasks();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> keyboard = keyboardGenerator.getKeyboardFromUnappointedTasksList(unappointedTasks, userRepository.get(tag).getId(), false);
        markup.setKeyboard(keyboard);

        String text = textGenerator.getMessageTextFromUnappointedTasksList(unappointedTasks);
        if (text.isEmpty()) text = "You dont have any unappointed tasks!";

        message.setText(text);
        message.setReplyMarkup(markup);

        session.close();
        return message;
    }

    public void getUnappointedTasksWithDeadlineTodayMenu(int tasksInfoId) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(tasksInfoId, session);

        message.setText(textGenerator.getUnappointedTasksWithDeadlineTodayText(todayTasksInfo));
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getUnappointedTasksWithDeadlineTodayKeyboard(todayTasksInfo)));

        botConfig.editMessage(todayTasksInfo.getUser().getChatId(), todayTasksInfo.getMessageId(), message);
        session.close();
    }

    public void getOutdatedUnappointedTasksMenu(int tasksInfoId) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(tasksInfoId, session);

        message.setText(textGenerator.getOutdatedUnappointedTasksText(todayTasksInfo));
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboardGenerator.getOutdatedUnappointedTasksKeyboard(todayTasksInfo)));

        botConfig.editMessage(todayTasksInfo.getUser().getChatId(), todayTasksInfo.getMessageId(), message);
        session.close();
    }
}
