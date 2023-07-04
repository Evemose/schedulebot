package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.entities.Entity;
import bot.schedulebot.entities.File;
import bot.schedulebot.entities.Subject;
import bot.schedulebot.entities.User;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.exceptions.NoFileInUpdateException;
import bot.schedulebot.objectsunderconstruction.ObjectsUnderConstruction;
import bot.schedulebot.repositories.FileRepository;
import bot.schedulebot.repositories.Repository;
import bot.schedulebot.repositories.SubjectRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.ClassFieldsStorage;
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
import java.time.format.DateTimeParseException;
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
    private final FileRepository fileRepository;
    private final Class<?> entityClass;
    private final ClassFieldsStorage classFieldsStorage;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, T entity) {return null;}

    public void handleAdditionStart(Update update) {

    }

    public void editEntity(Update update, String fieldName) {
        objectsUnderConstruction.getEditExchangers().put(parseUtil.getTag(update), new Exchanger<>());
        new Thread(() -> {
            try {
                threadUtil.scheduleThreadKill(Thread.currentThread());
                T t = repository.get(parseUtil.getTargetId(update.getCallbackQuery().getData()));
                Field field;
                try {
                    field = t.getClass().getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }

                Exchanger<Update> exchanger = objectsUnderConstruction.getEditExchangers().get(parseUtil.getTag(update));
                Message message = new Message();
                message.setText("Send new " + Arrays.toString(fieldName.split("(?=[A-Z])")).replace("[", "").replace("]", "").replace(",", " ").toLowerCase() + ":");
                botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(), message);

                Update newValueUpdate = exchanger.exchange(null);
                Object newValue = parseUpdate(field, newValueUpdate);
                while (newValue == null) {
                    newValueUpdate = exchanger.exchange(null);
                    newValue = parseUpdate(field, newValueUpdate);
                }
                updateEntity(update, t, field, newValue);
            } catch (InterruptedException ignored) {
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        },
                ((ParameterizedType)(this.getClass().getGenericSuperclass())).getActualTypeArguments()[0].getTypeName()
                        + " " + fieldName + " edit thread of " + update.getCallbackQuery().getFrom().getUserName()).start();
    }
    protected void addEntity(Update update, T t) {
        Exchanger<Update> exchanger = new Exchanger<>();
        objectsUnderConstruction.getExchangers().put(parseUtil.getTag(update), exchanger);
        new Thread(() -> {
            try {
                threadUtil.scheduleThreadKill(Thread.currentThread());
                botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                        menuStorage.getMenu(MenuMode.valueOf(t.getClass().getSimpleName().toUpperCase()+ "_START"), update));
                setEveryFieldValue(update, exchanger, t);
                this.persistEntity(update, t);
            } catch (InterruptedException ignored) {
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }, entityClass.getTypeName() + " add thread of " + update.getCallbackQuery().getFrom().getUserName()).start();
    }

    private void setEveryFieldValue(Update update, Exchanger<Update> exchanger, T t) throws InterruptedException, IllegalAccessException {
        Update newValueUpdate;
        Message message = new Message();
        System.out.println(Arrays.toString(classFieldsStorage.getEntitiesToAddPropertiesNotCollections().get(entityClass)));
        for(Field field : classFieldsStorage.getEntitiesToAddPropertiesNotCollections().get(entityClass)) {
            if (field.getName().equals("image") || field.getType().equals(File.class)) {
                if (!isUserWantToSetField(update, exchanger, field)) continue;
                message.setText("Send " + field.getName() + ":");
                botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                        message);
            }
            newValueUpdate = exchanger.exchange(null);
            Object newValue = parseUpdate(field, newValueUpdate);
            while (newValue == null) {
                newValueUpdate = exchanger.exchange(null);
                newValue = parseUpdate(field, newValueUpdate);;
            }
            field.set(t, newValue);
            botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                    menuStorage.getMenu(MenuMode.valueOf("SET_"
                                    + t.getClass().getSimpleName().toUpperCase()
                                    + "_" + field.getName().toUpperCase()),
                            update));
        }
    }
    private boolean isUserWantToSetField(Update update, Exchanger<Update> exchanger, Field field) throws InterruptedException {
        Update newValueUpdate;
        newValueUpdate = exchanger.exchange(null);
        while (!newValueUpdate.hasCallbackQuery()) {
            Message message = new Message();
            message.setText("Please answer the question above or cancel addition by clicking any other button");
            botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(), message);
            newValueUpdate = exchanger.exchange(null);
        }
        if (newValueUpdate.getCallbackQuery().getData().contains(" no")) {
            botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                    menuStorage.getMenu(MenuMode.valueOf("SET_" + entityClass.getSimpleName().toUpperCase() + "_" + field.getName().toUpperCase()), update));
            return false;
        }
        return true;
    }
    protected Object parseUpdate(Field field, Update newValueUpdate) throws InterruptedException {
        Object newValue;
            try {
                if (field.getType().equals(File.class)) {
                    if (newValueUpdate.getMessage().hasDocument()) {
                        newValue = parseUtil.getMessageFile(newValueUpdate);
                    } else {
                        throw new NoFileInUpdateException("file");
                    }
                } else if (field.getType().equals(String.class)) {
                    if (field.getName().toLowerCase().contains("image")) {
                        if (newValueUpdate.getMessage().hasPhoto()) {
                            newValue = converter.convertFileToJsonString(parseUtil.getMessageImage(newValueUpdate));
                        } else {
                            throw new NoFileInUpdateException("image");
                        }
                    } else {
                        newValue = newValueUpdate.getMessage().getText();
                    }
                } else if (field.getType().equals(LocalDate.class)) {
                    newValue = LocalDate.parse(newValueUpdate.getMessage().getText(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                } else if (field.getType().equals(LocalTime.class)) {
                    newValue = LocalTime.parse(newValueUpdate.getMessage().getText());
                } else if (field.getType().equals(int.class)) {
                    newValue = Integer.parseInt(newValueUpdate.getMessage().getText());
                    if ((Integer)newValue < 1) {
                        throw new NumberFormatException();
                    }
                } else if (field.getType().equals(Subject.class)) {
                    newValue = subjectRepository.get(parseUtil.getTargetId(newValueUpdate.getCallbackQuery().getData()));
                } else {
                    throw new RuntimeException("Unknown field type");
                }
            } catch (NumberFormatException | DateTimeParseException e) {
                Message message = new Message();
                message.setText("Wrong format. Try again");
                botConfig.sendMessage(newValueUpdate.getMessage().getChatId().toString(), message);
                return null;
            } catch (NoFileInUpdateException e) {
                Message message = new Message();
                message.setText(e.getMessage());
                botConfig.sendMessage(newValueUpdate.getMessage().getChatId().toString(), message);
                return null;
            }
        return newValue;
    }
    protected void persistEntity(Update update, T t) {
        repository.add(t);
        objectsUnderConstruction.getExchangers().remove(parseUtil.getTag(update));
        User user = userRepository.get(parseUtil.getTag(update));
        user.setMode("None");
        user.setInstanceAdditionStage(InstanceAdditionStage.NONE);
        userRepository.update(user);
        botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                menuStorage.getMenu(MenuMode.valueOf(entityClass.getSimpleName().toUpperCase() + "_MANAGE_MENU"),
                        update, t.getId()));
    }
    private void updateEntity(Update update, T t, Field field, Object newValue) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(t, newValue);

        repository.update(t);
        objectsUnderConstruction.getEditExchangers().remove(parseUtil.getTag(update));

        Message message = new Message();
        message.setText(t.getClass().getSimpleName() + " edited");
        botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(), message);
        botConfig.editMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                update.getCallbackQuery().getMessage().getMessageId(),
                menuStorage.getMenu(MenuMode.valueOf(t.getClass().getSimpleName().toUpperCase() + "_EDIT_MENU"),
                        update, parseUtil.getTargetId(update.getCallbackQuery().getData())));
    }

    protected Service(Repository<T> repository, ThreadUtil threadUtil, ParseUtil parseUtil, ObjectsUnderConstruction<T> objectsUnderConstruction, MenuStorage menuStorage, Converter converter, FileRepository fileRepository, ClassFieldsStorage classFieldsStorage, SubjectRepository subjectRepository, UserRepository userRepository) {
        this.repository = repository;
        this.threadUtil = threadUtil;
        this.parseUtil = parseUtil;
        this.objectsUnderConstruction = objectsUnderConstruction;
        this.menuStorage = menuStorage;
        this.converter = converter;
        this.fileRepository = fileRepository;
        this.classFieldsStorage = classFieldsStorage;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        botConfig = new BotConfig();
        entityClass = (Class<?>) ((ParameterizedType)(this.getClass().getGenericSuperclass())).getActualTypeArguments()[0];
    }
}
