package bot.schedulebot.boot;

import bot.schedulebot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotBoot {
    private final BotConfig botConfig;

    public BotBoot(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    public void startBot() {
        TelegramBotsApi telegramBotsApi;
        try {
            telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        try {
            telegramBotsApi.registerBot(botConfig);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
