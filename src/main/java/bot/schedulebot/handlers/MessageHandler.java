package bot.schedulebot.handlers;

import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.services.MainService;
import bot.schedulebot.services.Service;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.ParseUtil;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

@Controller
public class MessageHandler {
    private final CommandHandler commandHandler;
    private final MenuStorage menuStorage;
    private final UserRepository userRepository;
    private final Service mainService;
    private final CallbackQueryHandler callbackQueryHandler;

    public MessageHandler(CommandHandler commandHandler, MenuStorage menuStorage, UserRepository userRepository, MainService mainAdditionHandler, ParseUtil parseUtil, CallbackQueryHandler callbackQueryHandler) {
        this.commandHandler = commandHandler;
        this.menuStorage = menuStorage;
        this.userRepository = userRepository;
        this.mainService = mainAdditionHandler;
        this.callbackQueryHandler = callbackQueryHandler;
    }

    public List<Message> handleMessage(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                update.getMessage().setText(update.getMessage().getText()
                        .replace("*", " x ")
                        .replace("[", "\\["));
            }
            if (update.getMessage().hasText() && update.getMessage().getText().startsWith("/")) {
                return commandHandler.handleCommand(update);
            } else {
                if (!update.getMessage().getFrom().getUserName().equals("schedule_toDobot"))
                    return mainService.handleAddition(
                            userRepository.get(update.getMessage().getFrom().getUserName()).getInstanceAdditionStage(),
                            update, null);
            }
        } else if (update.hasCallbackQuery()) {
            return callbackQueryHandler.handleCallbackQuery(update);
        }
        if (update.getMessage().getPinnedMessage() != null) {
            return new ArrayList<>();
        } else {
            List<Message> messages = new ArrayList<>();
            messages.add(menuStorage.getMenu(MenuMode.UNHANDLED_MESSAGE, update));
            return messages;
        }
    }




}

