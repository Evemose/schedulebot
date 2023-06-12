package bot.schedulebot.repositories;

import bot.schedulebot.entities.TodayTasksInfo;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

@org.springframework.stereotype.Repository
public class TodayTasksInfoRepository extends Repository<TodayTasksInfo> {

    @Override
    protected int getEntitiesAmount() {
        return getAll().size() + 1;
    }

    public TodayTasksInfo get(String tag, Session session) {
        Query<TodayTasksInfo> query = session.createQuery("select t from TodayTasksInfo t where t.user.tag = :tag");
        query.setParameter("tag", tag);
        TodayTasksInfo todayTasksInfo = query.uniqueResult();
        return todayTasksInfo;
    }

    @Override
    public void update(TodayTasksInfo todayTasksInfo, Session session) {
        if (todayTasksInfo.getId() == 0) {
            add(todayTasksInfo, session);
        }
        else {
            todayTasksInfo.getUnappointedTasksWithDeadlineToday().get(0).getTask().getGroup().getUserRoles();
            Transaction transaction = session.beginTransaction();
            session.update(todayTasksInfo);
            session.flush();
            transaction.commit();
        }
    }

}
