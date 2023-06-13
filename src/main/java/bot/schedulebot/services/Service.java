package bot.schedulebot.services;

import bot.schedulebot.entities.Entity;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.util.ThreadUtil;
import org.springframework.scheduling.annotation.Async;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public abstract class Service<T> {
    abstract public List<Message> handleAddition(InstanceAdditionStage instanceAdditionStage, Update update, T entity);


}
