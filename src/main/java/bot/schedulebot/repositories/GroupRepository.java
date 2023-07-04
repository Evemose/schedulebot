package bot.schedulebot.repositories;

import bot.schedulebot.entities.Group;
import bot.schedulebot.enums.Role;
import bot.schedulebot.util.generators.StringGenerator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static bot.schedulebot.config.HibernateConfig.getSession;

@Repository
public class GroupRepository extends bot.schedulebot.repositories.Repository<Group> {
    private final StringGenerator stringGenerator;

    @Autowired
    protected GroupRepository(StringGenerator stringGenerator) {
        this.stringGenerator = stringGenerator;
    }

    public Group get(String code, Session session) {
        Query<Group> query = session.createQuery("select g from Group g where code = :code", Group.class);
        query.setParameter("code", code);
        return query.uniqueResult();
    }

    @Override
    public void add(Group group) {
        Session session = getSession();
        Transaction transaction = session.beginTransaction();
        group.setId(entitiesAmount++);
        do {
            group.setCode(stringGenerator.generateRandomString());
        } while (get(group.getCode(), session) != null);
        session.merge(group);
        transaction.commit();
        session.close();
    }

    public void updateUserRole(int userId, Role role, int groupId) {
        Session session = getSession();
        Transaction transaction = session.beginTransaction();
        NativeQuery<?> query = session.createNativeQuery("update groups_user_roles set userroles = :role where users_id = :userId and group_id = :groupId", Object.class);
        query.setParameter("role", role.toString());
        query.setParameter("userId", userId);
        query.setParameter("groupId", groupId);
        query.executeUpdate();
        transaction.commit();
        session.close();
    }

    public Role getUserRole(int userId, int groupId) {
        Session session = getSession();
        NativeQuery<String> query = session.createNativeQuery("select userroles from groups_user_roles where users_id = :userId and group_id = :groupId", String.class);
        query.setParameter("userId", userId);
        query.setParameter("groupId", groupId);
        String role = query.uniqueResult();
        return role == null ? null : Role.valueOf(role);
    }
}
