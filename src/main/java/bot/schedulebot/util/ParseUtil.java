package bot.schedulebot.util;

import bot.schedulebot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ParseUtil {

    private final BotConfig botConfig;

    public ParseUtil() {
        this.botConfig = new BotConfig();
    }

    public int getTargetId(String callbackData) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(callbackData);
        matcher.find();
        return Integer.valueOf(matcher.group(0));
    }

    public int getTargetId(String callbackData, int number) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(callbackData);
        for (int i = 0; i < number; i++)
            matcher.find();
        return Integer.valueOf(matcher.group(0));
    }

    public String getTag(Update update) {
        if (update.hasCallbackQuery()) return update.getCallbackQuery().getFrom().getUserName();
        else return update.getMessage().getFrom().getUserName();
    }

    public File getMessageImage(Update update) {
        PhotoSize photoSize = update.getMessage().getPhoto().stream().max(Comparator.comparing(PhotoSize::getFileSize)).orElse(null);
        String fileId = photoSize.getFileId();
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        try {
            org.telegram.telegrambots.meta.api.objects.File tgFile = botConfig.execute(getFile);
            return botConfig.downloadFile(tgFile);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public String getChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId().toString();
        } else {
            return update.getMessage().getChatId().toString();
        }
    }

    public bot.schedulebot.entities.File getMessageFile(Update update) {
        String fileId = update.getMessage().getDocument().getFileId();
        GetFile getFile = new GetFile(fileId);
        bot.schedulebot.entities.File resultFile = new bot.schedulebot.entities.File();
        try {
            org.telegram.telegrambots.meta.api.objects.File tgFile = botConfig.execute(getFile);
            File file = botConfig.downloadFile(tgFile);
            resultFile.setFile(Files.readAllBytes(file.toPath()));
            resultFile.setFileType(tgFile.getFilePath().substring(tgFile.getFilePath().lastIndexOf(".")));
            file.delete();
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resultFile;
    }
}
