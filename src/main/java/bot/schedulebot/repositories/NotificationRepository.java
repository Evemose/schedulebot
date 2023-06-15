package bot.schedulebot.repositories;

import bot.schedulebot.entities.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository extends bot.schedulebot.repositories.Repository<Notification> {
    @Autowired
    protected NotificationRepository() {}
}
