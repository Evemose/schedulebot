package bot.schedulebot.handlers;

import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.storages.menustorages.MenuStorage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;

@Component
public class CommandHandler {

    private final UserRepository userRepository;
    private final MenuStorage menuStorage;

    public CommandHandler(UserRepository userRepository, MenuStorage menuStorage) {
        this.userRepository = userRepository;
        this.menuStorage = menuStorage;
    }

    List<Message> handleCommand(Update update) {
        String command = update.getMessage().getText();
        List<Message> resultMessagesSet = new ArrayList<>();
        switch (command) {
            case "/start" -> {
                resultMessagesSet.add(tryRegisterUser(update.getMessage().getFrom(), update));
                try {
                    resultMessagesSet.add(menuStorage.getMenu(MenuMode.MAIN_MENU, update, userRepository.get(update.getMessage().getFrom().getUserName()).getId()));
                } catch (NullPointerException e) {
                    // in case of user is not registered
                }
            }
            case "/door" -> resultMessagesSet.add(menuStorage.getMenu(MenuMode.MAIN_MENU, update, userRepository.get(update.getMessage().getFrom().getUserName()).getId()));
            default -> {
                Message message = new Message();
                message.setText("Wrong command");
                resultMessagesSet.add(message);
            }
        }
        return resultMessagesSet;
    }
    public Message tryRegisterUser(User user, Update update) {
        Message message = new Message();
        bot.schedulebot.entities.User existingUser = userRepository.getByChatId(update.getMessage().getChatId().toString());
        if (existingUser == null) {

            bot.schedulebot.entities.User newUser = new bot.schedulebot.entities.User();
            newUser.setName(user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : ""));
            if (user.getUserName() == null) {
                message.setText("You don't have a username! Please, set it in Telegram settings and try again.");
                return message;
            }
            newUser.setTag(user.getUserName());
            newUser.setChatId(update.getMessage().getChatId().toString());
            newUser.setGroupMode(true);
            userRepository.add(newUser);

            message.setText("You've been registered!\n");
        } else {
            existingUser.setChatId(update.getMessage().getChatId().toString());
            userRepository.update(existingUser);
            message.setText("You are already registered!");
        }
        return message;
    }
}
