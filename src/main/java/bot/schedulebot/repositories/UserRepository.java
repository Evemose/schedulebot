package bot.schedulebot.repositories;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import static bot.schedulebot.config.HibernateConfig.getSession;

@Repository
public class UserRepository extends bot.schedulebot.repositories.Repository<User> {
    protected UserRepository() {}
    public User get(String tag) {
        Session session = getSession();
        Query<User> query = session.createQuery("select u from User u where tag = :tag");
        query.setParameter("tag", tag);
        User user = query.uniqueResult();
        session.close();
        return user;
    }

    public User get(String tag, Session session) {
        Query<User> query = session.createQuery("select u from User u where tag = :tag");
        query.setParameter("tag", tag);
        User user = query.uniqueResult();
        return user;
    }

    public void deleteUserFromGroup(int userId, int groupId) {
        Session session = HibernateConfig.getSession();
        Transaction transaction = session.beginTransaction();
        NativeQuery query = session.createNativeQuery("delete from groups_users where groups_id = :groupId and users_id = :userId");
        query.setParameter("groupId", groupId);
        query.setParameter("userId", userId);
        query.executeUpdate();
        NativeQuery query3 = session.createNativeQuery("delete from groups_user_roles where group_id = :groupId and users_id = :userId");
        query3.setParameter("groupId", groupId);
        query3.setParameter("userId", userId);
        query3.executeUpdate();
        Query query1 = session.createQuery("delete from Appointment a where a.user.id = :userId and a.group.id = :groupId");
        query1.setParameter("groupId", groupId);
        query1.setParameter("userId", userId);
        Query query2 = session.createQuery("delete from UnappointedTask a where a.user.id = :userId and a.group.id = :groupId");
        query2.setParameter("groupId", groupId);
        query2.setParameter("userId", userId);
        query2.executeUpdate();
        transaction.commit();
        session.close();

    }

    public void addUserToGroup(int userId, int groupId) {
        Session session = HibernateConfig.getSession();
        Transaction transaction = session.beginTransaction();
        NativeQuery query = session.createNativeQuery("insert into groups_users values ( :groupId , :userId )");
        query.setParameter("groupId", groupId);
        query.setParameter("userId", userId);
        query.executeUpdate();
        NativeQuery query1 = session.createNativeQuery("insert into groups_user_roles values ( :groupId , 'DEFAULT', :userId )");
        query1.setParameter("groupId", groupId);
        query1.setParameter("userId", userId);
        query1.executeUpdate();
        transaction.commit();
        session.close();
    }

    public void delete(String tag) {
        Session session = getSession();
        Transaction transaction = session.beginTransaction();
        Query query = session.createQuery("delete User u where tag = :tag");
        query.setParameter("tag", tag);
        query.executeUpdate();
        transaction.commit();
        session.close();
    }

    public User getByChatId(String string) {
        Session session = HibernateConfig.getSession();
        Query<User> query = session.createQuery("select u from User u where chatId = :chatId");
        query.setParameter("chatId", string);
        User user = query.uniqueResult();
        session.close();
        return user;
    }
}
