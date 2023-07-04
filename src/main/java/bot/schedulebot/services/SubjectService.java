package bot.schedulebot.services;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Subject;
import bot.schedulebot.entities.User;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.objectsunderconstruction.SubjectsUnderConstruction;
import bot.schedulebot.repositories.GroupRepository;
import bot.schedulebot.repositories.SubjectRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.ClassFieldsStorage;
import bot.schedulebot.util.Converter;
import bot.schedulebot.util.ParseUtil;
import bot.schedulebot.util.ThreadUtil;
import org.hibernate.Session;
import org.springframework.dao.DataIntegrityViolationException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.concurrent.Exchanger;

@org.springframework.stereotype.Service
public class SubjectService extends Service<Subject> {
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ParseUtil parseUtil;
    private final MenuStorage menuStorage;
    private final GroupRepository groupRepository;
    private final SubjectsUnderConstruction subjectAdditionHelper;
    private final BotConfig botConfig;

    protected SubjectService(ClassFieldsStorage classFieldsStorage, UserRepository userRepository, SubjectRepository subjectRepository, Converter converter, ParseUtil parseUtil, MenuStorage menuStorage, GroupRepository groupRepository, SubjectsUnderConstruction subjectAdditionHelper, ThreadUtil threadUtil) {
        super(subjectRepository, threadUtil, parseUtil, subjectAdditionHelper, menuStorage, converter, null, classFieldsStorage, subjectRepository);
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.parseUtil = parseUtil;
        this.menuStorage = menuStorage;
        this.groupRepository = groupRepository;
        this.subjectAdditionHelper = subjectAdditionHelper;
        this.botConfig = new BotConfig();
    }

    public void handleSubjectDeletion(Update update, List<Message> resultMessagesList, String callbackData, User u) {
        u.setGroupMode(callbackData.matches("Delete subject in group \\d+"));
        userRepository.update(u);
        Message message = new Message();

        try {
            resultMessagesList.add(message);
            Session session = HibernateConfig.getSession();
            int groupId = -132;
            if (u.isGroupMode()) {
                groupId = subjectRepository.get(parseUtil.getTargetId(callbackData), session).getGroup().getId();
            }
            subjectRepository.delete(parseUtil.getTargetId(callbackData));
            message.setText("Subject deleted");
            botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
            botConfig.editMessage(u.getChatId(),
                    subjectAdditionHelper.getMessageIds().get(u.getTag()),
                    groupId == -132 ? menuStorage.getMenu(MenuMode.SHOW_SUBJECTS_OF_USER, update)
                            : menuStorage.getMenu(MenuMode.GET_GROUP_SUBJECTS, update, groupId));
            session.close();
        } catch (DataIntegrityViolationException e) {
            message.setText("Subject still has tasks related to it");
        }
    }

    @Override
    public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, Subject entity) {
        new Thread(() -> {
        try {
            threadUtil.scheduleThreadKill(Thread.currentThread());
            Exchanger<Update> exchanger = subjectAdditionHelper.getExchangers().get(parseUtil.getTag(update));

            botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                    menuStorage.getMenu(MenuMode.SUBJECT_START, update));
            entity.setName(exchanger.exchange(null).getMessage().getText());

            subjectRepository.add(entity);
            botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                    menuStorage.getMenu(MenuMode.SET_SUBJECT_NAME, update));

            if (entity.getGroup() != null)
            botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                    menuStorage.getMenu(MenuMode.GET_GROUP_SUBJECTS, update, entity.getGroup().getId()));
            else {
                botConfig.sendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                        menuStorage.getMenu(MenuMode.SHOW_SUBJECTS_OF_USER, update));
            }
        } catch (InterruptedException ignored){}
        }, "Subject construction of " + parseUtil.getTag(update)).start();
        return null;
    }

    public void handleAdditionStart(Update update) {
        Subject subject = new Subject();
        subjectAdditionHelper.getExchangers().put(parseUtil.getTag(update), new Exchanger<>());
        String callbackData = update.getCallbackQuery().getData();
        User user = userRepository.get(parseUtil.getTag(update));
        user.setGroupMode(callbackData.matches("Add subject to group \\d+"));
        user.setInstanceAdditionStage(InstanceAdditionStage.SUBJECT_START);
        userRepository.update(user);
        if (callbackData.matches("Add subject to group \\d+")) {
            subject.setGroup(groupRepository.get(parseUtil.getTargetId(callbackData)));
        }
        else {
            subject.setUser(userRepository.get(parseUtil.getTag(update)));
        }
        addEntity(update, subject);
    }
}
