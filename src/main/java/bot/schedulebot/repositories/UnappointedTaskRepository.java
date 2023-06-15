package bot.schedulebot.repositories;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.UnappointedTask;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static bot.schedulebot.config.HibernateConfig.getSession;

@Repository
public class UnappointedTaskRepository extends bot.schedulebot.repositories.Repository<UnappointedTask> {
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    @Autowired
    protected UnappointedTaskRepository(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    public List<UnappointedTask> getUnappointedTasksOfTask(int taskId) {
        Session session = HibernateConfig.getSession();
        List<UnappointedTask> unappointedTasks = getAll(session).stream().filter(unappointedTask -> unappointedTask.getTask().getId() == taskId).toList();
        session.close();
        return unappointedTasks;
    }

    public void delete(int id, Session session) {
        Transaction transaction = session.beginTransaction();
        Query query = session.createQuery("delete UnappointedTask a where a.id = :id");
        query.setParameter("id", id);
        query.executeUpdate();
        transaction.commit();
    }

    public UnappointedTask getUnappointedTaskByTaskAndUser(int taskId, int userId) {
        Session session = getSession();
        Transaction transaction = session.beginTransaction();
        Query<UnappointedTask> query = session.createQuery("select a from UnappointedTask a where a.user = :user and a.task = :task");
        query.setParameter("user", userRepository.get(userId));
        query.setParameter("task", taskRepository.get(taskId));
        UnappointedTask unappointedTask = query.uniqueResult();
        transaction.commit();
        session.close();
        return unappointedTask;
    }
}
