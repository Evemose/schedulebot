package bot.schedulebot.storages.menustorages;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Task;
import bot.schedulebot.repositories.TaskRepository;
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
import java.util.ArrayList;
import java.util.List;

@Component
public class TaskMenuStorage {

    private final ParseUtil parseUtil;
    private final TaskRepository taskRepository;
    private final KeyboardGenerator keyboardGenerator;
    private final FileGenerator fileGenerator;
    private final BotConfig botConfig;
    private final Converter converter;
    private final TextGenerator textGenerator;

    public TaskMenuStorage(ParseUtil parseUtil, TaskRepository taskRepository, KeyboardGenerator keyboardGenerator, FileGenerator fileGenerator, Converter converter, TextGenerator textGenerator) {
        this.parseUtil = parseUtil;
        this.taskRepository = taskRepository;
        this.keyboardGenerator = keyboardGenerator;
        this.fileGenerator = fileGenerator;
        this.botConfig = new BotConfig();
        this.converter = converter;
        this.textGenerator = textGenerator;
    }

    public Message getTaskEditMenu(int taskId, Update update) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Task task = taskRepository.get(taskId, session);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboardGenerator.getTaskEditKeyboard(taskId));

        message = sendTaskMessage(message, task, update, markup);
        if (message != null) {
            message.setReplyMarkup(markup);
        }

        session.close();
        return message;
    }

    public Message getTaskMenu(int taskId, Update update) {
        Message message = new Message();
        Session session = HibernateConfig.getSession();
        Task task = taskRepository.get(taskId, session);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(keyboardGenerator.createSingleButtonRow("Edit", "Edit task " + taskId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Delete", "Delete task " + taskId));
        keyboard.add(keyboardGenerator.createSingleButtonRow("Back", "Show tasks in group " + task.getGroup().getId()));
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        message = sendTaskMessage(message, task, update, markup);

        session.close();
        return message;
    }


    private Message sendTaskMessage(Message message, Task task, Update update, InlineKeyboardMarkup markup) {
        if (task.getFile() != null) {
            File file = fileGenerator.getFileFromByteArray(task.getFile().getFile(),
                    task.getName().replace(":", "").replace("\\", "").replace("/", "") + task.getFile().getFileType());
            botConfig.sendDocument(parseUtil.getChatId(update),
                    new InputFile(file),
                    "*Attachment*");
            file.delete();
        }

        if (task.getImage() != null) {
            File file = converter.convertJsonStringToFile(task.getImage());
            botConfig.sendPhoto(parseUtil.getChatId(update),
                    new InputFile(file),
                    task.toString(),
                    markup);
            file.delete();
            return null;
        } else {
            message.setText(task.toString());
        }
        return message;
    }

}
