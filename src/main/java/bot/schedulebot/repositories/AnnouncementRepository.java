package bot.schedulebot.repositories;

import bot.schedulebot.entities.Announcement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AnnouncementRepository extends bot.schedulebot.repositories.Repository<Announcement> {
    @Autowired
    protected AnnouncementRepository() {}
}
