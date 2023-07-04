package bot.schedulebot.config;

import bot.schedulebot.handlers.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class BotConfig extends TelegramLongPollingBot {

    @Value(value = "${bot.name}")
    private String botName;
    @Value(value = "${bot.token}")
    private String botToken;
    private final MessageHandler messageHandler;

    @Autowired
    public BotConfig(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            List<Message> messagesList = messageHandler.handleMessage(update);
            if (update.hasMessage()) {
                sendMessagesList(update.getMessage().getChatId().toString(), messagesList);
            } else {
                sendMessagesList(update.getCallbackQuery().getMessage().getChatId().toString(), messagesList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Integer> sendMessagesList(String chatId, List<Message> messagesList) {
        List<Integer> messageIds = new ArrayList<>();
        messagesList.forEach(message -> messageIds.add(sendMessage(chatId, message)));
        return messageIds;
    }

    public int sendMessage(String chatId, Message message) {
        if (message != null) {
            try {
                if (message.hasReplyMarkup()) {
                    return execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(message.getText().replace("_", "\\_"))
                            .replyMarkup(message.getReplyMarkup())
                            .parseMode("Markdown")
                            .build()).getMessageId();
                } else {
                    return execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(message.getText())
                            .parseMode("Markdown")
                            .build()).getMessageId();
                }
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else {
            return 0;
        }
    }

    public void sendPhoto(String chatId, InputFile inputFile, String caption, InlineKeyboardMarkup markup) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(inputFile);
        sendPhoto.setChatId(chatId);
        sendPhoto.setCaption(caption);
        sendPhoto.setReplyMarkup(markup);
        sendPhoto.setParseMode("Markdown");
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendDocument(String chatId, InputFile file, String caption) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(file);
        sendDocument.setCaption(caption);
        sendDocument.setParseMode("Markdown");
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendDocument(String chatId, InputFile file, String caption, InlineKeyboardMarkup markup) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(file);
        sendDocument.setCaption(caption);
        sendDocument.setReplyMarkup(markup);
        sendDocument.setParseMode("Markdown");
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    public void pinMessage(String chatId, int messageId) {
        PinChatMessage pinChatMessage = new PinChatMessage();
        pinChatMessage.setChatId(chatId);
        pinChatMessage.setMessageId(messageId);
        try {
            execute(pinChatMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void editMessage(String chatId, int messageId, Message message) {
        if (message != null) {
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId);
            editMessageText.setParseMode("Markdown");
            editMessageText.setMessageId(messageId);
            editMessageText.setText(message.getText());
            editMessageText.setReplyMarkup(message.getReplyMarkup());

            EditMessageCaption editMessageCaption = new EditMessageCaption();
            editMessageCaption.setChatId(chatId);
            editMessageCaption.setParseMode("Markdown");
            editMessageCaption.setMessageId(messageId);
            editMessageCaption.setCaption(message.getText());
            editMessageCaption.setReplyMarkup(message.getReplyMarkup());

            try {
                execute(editMessageText);
            } catch (TelegramApiException e) {
                if (!e.getMessage().contains("exactly the same as a current")) {
                    try {
                        execute(editMessageCaption);
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    public void deleteMessage(String chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public BotConfig() {
        this.messageHandler = null;
    }
}

