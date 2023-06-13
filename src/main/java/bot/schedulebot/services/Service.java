package bot.schedulebot.services;

import bot.schedulebot.entities.Entity;
import bot.schedulebot.entities.File;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.objectsunderconstruction.ObjectsUnderConstruction;
import bot.schedulebot.repositories.Repository;
import bot.schedulebot.util.ParseUtil;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Exchanger;

public abstract class Service<T extends Entity> {
    private final Repository<T> repository;
    protected final ParseUtil parseUtil;
    private final ObjectsUnderConstruction<T> objectsUnderConstruction;
    abstract public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, T entity);
    public void editEntity(Update update, String fieldName) {
        new Thread(() -> {
            try {
                T t = repository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()));
                Field field;
                try {
                    field = t.getClass().getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
                Exchanger<Update> exchanger = new Exchanger<>();
                Object newValue;
                Update newValueUpdate = exchanger.exchange(null);
                if (field.getType().equals(File.class)) {
                    newValue = parseUtil.getMessageFile(newValueUpdate);
                } else if (field.getType().equals(String.class)) {
                    if (fieldName.toLowerCase().contains("image")) {
                        newValue = parseUtil.getMessageImage(newValueUpdate);
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
            } catch (InterruptedException ignored) {
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        },
                ((ParameterizedType)repository.getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName()
                        + " " + fieldName + " edit thread of " + update.getCallbackQuery().getFrom().getUserName()).start();
    }
    protected Service(Repository<T> repository, ParseUtil parseUtil, ObjectsUnderConstruction<T> objectsUnderConstruction) {
        this.repository = repository;
        this.parseUtil = parseUtil;
        this.objectsUnderConstruction = objectsUnderConstruction;
    }
}
