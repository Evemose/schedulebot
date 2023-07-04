package bot.schedulebot.repositories;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Task;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository extends bot.schedulebot.repositories.Repository<Task> {
    private final FileRepository fileRepository;

    @Autowired
    protected TaskRepository(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public void add(Task task) {
        Session session = HibernateConfig.getSession();
        add(task, session);
        session.close();
    }

    @Override
    public void add(Task task, Session session) {
        if (task.getFile() != null) {
            fileRepository.add(task.getFile(), session);
        }
        super.add(task, session);
    }

    public void update(Task task) {
        Session session = HibernateConfig.getSession();
        update(task, session);
        session.close();
    }

    public void update(Task task, Session session) {
        Session otherSession = HibernateConfig.getSession();
        Task oldTask = get(task.getId(), otherSession);
        if (oldTask.getFile() != null) {
            fileRepository.delete(task.getFile().getId());
        }
        otherSession.close();
        if (task.getFile() != null) {
            fileRepository.add(task.getFile(), session);
        }
        super.update(task, session);
    }
}
