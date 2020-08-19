package com.vinberts.vinscraper.database;

import com.google.common.collect.Lists;
import com.vinberts.vinscraper.database.models.Definition;
import com.vinberts.vinscraper.database.models.WordQueue;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 *
 */
@Slf4j
public class DatabaseHelper {

    public static boolean insertNewDefinition(Definition definition) {
        Transaction transaction = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.save(definition);
            transaction.commit();
            session.close();
            return true;
        } catch (RuntimeException e) {
            if (Objects.nonNull(transaction)) {
                transaction.rollback();
            }
            log.error("Exception occurred trying to commit new Definition", e);
        }
        return false;
    }

    public static boolean insertNewWordQueue(WordQueue queue) {
        Transaction transaction = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.save(queue);
            transaction.commit();
            session.close();
            return true;
        } catch (RuntimeException e) {
            if (Objects.nonNull(transaction)) {
                transaction.rollback();
            }
            log.error("Exception occurred trying to commit new Queue", e);
        }
        return false;
    }

    public static Optional<Definition> getDefinitionById(String id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        IdentifierLoadAccess<Definition> identifierLoadAccess = session.byId(Definition.class);
        Optional<Definition> gifOptional = identifierLoadAccess.loadOptional(id);
        session.close();
        return gifOptional;
    }

    public static List<WordQueue> getUnprocessedWordQueue(int limit) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        EntityManager manager = session.getEntityManagerFactory().createEntityManager();

        String hql = "SELECT q FROM WordQueue q WHERE q.processed = false";
        Query query = manager.createQuery(hql);
        query.setMaxResults(limit);
        List results = query.getResultList();
        session.close();
        if (results.isEmpty()) {
            return Lists.newArrayList();
        } else {
            return results;
        }
    }

    public static Optional<Definition> getDefinitionByWord(String word) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        EntityManager manager = session.getEntityManagerFactory().createEntityManager();

        String hql = "SELECT d FROM Definition d WHERE d.word = ?1";
        Query query = manager.createQuery(hql).setParameter(1, word);
        List results = query.getResultList();
        session.close();
        if (results.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of((Definition)results.get(0));
        }
    }

    public static boolean updateWordQueue(WordQueue wordQueue) {
        EntityManager manager = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            manager = session.getEntityManagerFactory().createEntityManager();
            manager.getTransaction().begin();
            String hql = "UPDATE WordQueue set processed = true " +
                    "WHERE uuid = :id";
            Query query = manager.createQuery(hql);
            query.setParameter("id", wordQueue.getUuid());
            query.executeUpdate();
            manager.getTransaction().commit();
            session.close();
            return true;
        } catch (RuntimeException e) {
            if (manager != null) {
                manager.getTransaction().rollback();
            }
            log.error("Exception occurred trying to update word queue", e);
            return false;
        }
    }
}
