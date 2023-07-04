package bot.schedulebot.repositories;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Entity;
import jakarta.annotation.PostConstruct;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.lang.reflect.ParameterizedType;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public abstract class Repository<T extends Entity> {
    protected int entitiesAmount;
    private final Class<T> genericType;
    protected Repository() {
        genericType = (Class<T>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
    @PostConstruct
    private void init() {
        entitiesAmount = getEntitiesAmount();
    }

    protected int getEntitiesAmount() {
        try {
            return Objects.requireNonNull(this.getAll().stream()
                            .max(Comparator.comparingInt(Entity::getId)).orElse(null))
                    .getId() + 1;
        } catch (NullPointerException e) {
            return 1;
        }
    }

    public List<T> getAll() {
        Session session = HibernateConfig.getSession();
        Query<T> query = session.createQuery("from " + genericType.getTypeName(), genericType);
        List<T> result = query.getResultList();
        session.close();
        return result;
    }

    public List<T> getAll(Session session) {
        Query<T> query = session.createQuery("from " + genericType.getTypeName(), genericType);
        return query.getResultList();
    }

    public T get(int id) {
        Session session = HibernateConfig.getSession();
        Query<T> query = session.createQuery("from " + genericType.getTypeName() + " t where t.id = :id", genericType);
        query.setParameter("id", id);
        T result = query.uniqueResult();
        session.close();
        return result;
    }

    public T get(int id, Session session) {
        Query<T> query = session.createQuery("from " + genericType.getTypeName() + " t where t.id = :id", genericType);
        query.setParameter("id", id);
        return query.uniqueResult();
    }

    public void delete(int id) {
        Session session = HibernateConfig.getSession();
        Transaction transaction = session.beginTransaction();
        Query<?> query = session.createQuery("delete from " + genericType.getTypeName() + " t where t.id = :id", null);
        query.setParameter("id", id);
        query.executeUpdate();
        transaction.commit();
        session.close();
    }

    public void update(T t, Session session) {
        Transaction transaction = session.beginTransaction();
        session.merge(t);
        transaction.commit();
    }

    public void update(T t) {
        Session session = HibernateConfig.getSession();
        Transaction transaction = session.beginTransaction();
        session.merge(t);
        transaction.commit();
        session.close();
    }

    public void add(T t) {
        Session session = HibernateConfig.getSession();
        Transaction transaction = session.beginTransaction();
        t.setId(entitiesAmount++);
        session.persist(t);
        transaction.commit();
        session.close();
    }

    public void add(T t, Session session) {
        Transaction transaction = session.beginTransaction();
        t.setId(entitiesAmount++);
        session.persist(t);
        transaction.commit();
    }
}
