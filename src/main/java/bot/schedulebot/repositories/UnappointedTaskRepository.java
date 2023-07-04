package bot.schedulebot.repositories;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.UnappointedTask;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UnappointedTaskRepository extends bot.schedulebot.repositories.Repository<UnappointedTask> {

    protected UnappointedTaskRepository() {}

    public List<UnappointedTask> getUnappointedTasksOfTask(int taskId) {
        Session session = HibernateConfig.getSession();
        List<UnappointedTask> unappointedTasks = getAll(session).stream().filter(unappointedTask -> unappointedTask.getTask().getId() == taskId).toList();
        session.close();
        return unappointedTasks;
    }

    public void delete(int id, Session session) {
        Transaction transaction = session.beginTransaction();
        Query<?> query = session.createQuery("delete UnappointedTask a where a.id = :id", null);
        query.setParameter("id", id);
        query.executeUpdate();
        transaction.commit();
    }
}
