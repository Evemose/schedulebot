package bot.schedulebot.repositories;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Announcement;
import bot.schedulebot.entities.Task;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AnnouncementRepository extends bot.schedulebot.repositories.Repository<Announcement> {
    private final FileRepository fileRepository;

    @Autowired
    protected AnnouncementRepository(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public void add(Announcement announcement) {
        Session session = HibernateConfig.getSession();
        add(announcement, session);
        session.close();
    }

    @Override
    public void add(Announcement announcement, Session session) {
        if (announcement.getFile() != null) {
            fileRepository.add(announcement.getFile(), session);
        }
        super.add(announcement, session);
    }

    @Override
    public void update(Announcement announcement) {
        Session session = HibernateConfig.getSession();
        update(announcement, session);
        session.close();
    }

    @Override
    public void update(Announcement announcement, Session session) {
        Session otherSession = HibernateConfig.getSession();
        Announcement oldAnnouncement = get(announcement.getId(), otherSession);
        if (oldAnnouncement.getFile() != null) {
            fileRepository.delete(announcement.getFile().getId());
        }
        otherSession.close();
        if (announcement.getFile() != null) {
            fileRepository.add(announcement.getFile(), session);
        }
        super.update(announcement, session);
    }
}
