package bot.schedulebot.repositories;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Entity;
import jakarta.annotation.PostConstruct;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.lang.reflect.ParameterizedType;
import java.util.List;

public abstract class Repository<T extends Entity> {
    protected int entitiesAmount;

    @PostConstruct
    private void init() {
        entitiesAmount = getEntitiesAmount();
    }

    protected int getEntitiesAmount() {
        return this.getAll().size();
    }

    public List<T> getAll() {
        Session session = HibernateConfig.getSession();
        Query<T> query = session.createQuery("select t from " + ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName() + " t");
        List<T> result = query.getResultList();
        session.close();
        return result;
    }

    public List<T> getAll(Session session) {
        Query<T> query = session.createQuery("select t from " + ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName() + " t");
        List<T> result = query.getResultList();
        return result;
    }

    public T get(int id) {
        Session session = HibernateConfig.getSession();
        Query<T> query = session.createQuery("select t from " + ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName() + " t where t.id = :id");
        query.setParameter("id", id);
        T result = query.uniqueResult();
        session.close();
        return result;
    }

    public T get(int id, Session session) {
        Query<T> query = session.createQuery("select t from " + ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName() + " t where t.id = :id");
        query.setParameter("id", id);
        T result = query.uniqueResult();
        return result;
    }

    public void delete(int id) {
        Session session = HibernateConfig.getSession();
        Transaction transaction = session.beginTransaction();
        Query<T> query = session.createQuery("delete from " + ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName() + " t where t.id = :id");
        query.setParameter("id", id);
        query.executeUpdate();
        transaction.commit();
        session.close();
    }

    public void update(T t, Session session) {
        Transaction transaction = session.beginTransaction();
        session.update(t);
        session.flush();
        transaction.commit();
    }

    public void update(T t) {
        Session session = HibernateConfig.getSession();
        Transaction transaction = session.beginTransaction();
        session.update(t);
        session.flush();
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