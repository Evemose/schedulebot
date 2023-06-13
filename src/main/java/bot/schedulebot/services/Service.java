package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.entities.Entity;
import bot.schedulebot.entities.File;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.objectsunderconstruction.ObjectsUnderConstruction;
import bot.schedulebot.repositories.Repository;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.Converter;
import bot.schedulebot.util.ParseUtil;
import bot.schedulebot.util.ThreadUtil;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Exchanger;

public abstract class Service<T extends Entity> {
    private final Repository<T> repository;
    protected final ThreadUtil threadUtil;
    protected final ParseUtil parseUtil;
    private final BotConfig botConfig;
    private final ObjectsUnderConstruction<T> objectsUnderConstruction;
    private final MenuStorage menuStorage;
    private final Converter converter;
    abstract public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, T entity);
    public void editEntity(Update update, String fieldName) {
        objectsUnderConstruction.getEditExchangers().put(parseUtil.getTag(update), new Exchanger<>());
        new Thread(() -> {
            try {
                T t = repository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()));
                Field field;
                try {
                    field = t.getClass().getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
                Exchanger<Update> exchanger = objectsUnderConstruction.getEditExchangers().get(parseUtil.getTag(update));
                Object newValue;
                Message message = new Message();
                message.setText("Send new " + Arrays.toString(fieldName.split("(?=[A-Z])")).replace("[", "").replace("]", "").replace(",", " ").toLowerCase() + ":");
                botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(), message);
                Update newValueUpdate = exchanger.exchange(null);
                if (field.getType().equals(File.class)) {
                    newValue = parseUtil.getMessageFile(newValueUpdate);
                } else if (field.getType().equals(String.class)) {
                    if (fieldName.toLowerCase().contains("image")) {
                        newValue = converter.convertFileToJsonString(parseUtil.getMessageImage(newValueUpdate));
                    } else {
                        newValue = newValueUpdate.getMessage().getText();
                    }
                } else if (field.getType().equals(LocalDate.class)) {
                    newValue = LocalDate.parse(newValueUpdate.getMessage().getText(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                } else if (field.getType().equals(LocalTime.class)) {
                    newValue = LocalTime.parse(newValueUpdate.getMessage().getText());
                } else if (field.getType().equals(int.class)) {
                    newValue = Integer.parseInt(newValueUpdate.getMessage().getText());
                } else {
                    throw new RuntimeException("Unknown field type");
                }
                field.setAccessible(true);
                field.set(t, (field.getType()).cast(newValue));
                repository.update(t);
                objectsUnderConstruction.getEditExchangers().remove(parseUtil.getTag(update));
                message.setText(t.getClass().getSimpleName() + " edited");
                botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(), message);
                botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                        menuStorage.getMenu(MenuMode.valueOf(t.getClass().getSimpleName().toUpperCase() + "_EDIT_MENU"),
                                update, parseUtil.getTargetId(update.getCallbackQuery().getData())));
            } catch (InterruptedException ignored) {
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        },
                ((ParameterizedType)(this.getClass().getGenericSuperclass())).getActualTypeArguments()[0].getTypeName()
                        + " " + fieldName + " edit thread of " + update.getCallbackQuery().getFrom().getUserName()).start();
    }
    protected Service(Repository<T> repository, ThreadUtil threadUtil, ParseUtil parseUtil, ObjectsUnderConstruction<T> objectsUnderConstruction, MenuStorage menuStorage, Converter converter) {
        this.repository = repository;
        this.threadUtil = threadUtil;
        this.parseUtil = parseUtil;
        this.objectsUnderConstruction = objectsUnderConstruction;
        this.menuStorage = menuStorage;
        this.converter = converter;
        botConfig = new BotConfig();
    }
}
