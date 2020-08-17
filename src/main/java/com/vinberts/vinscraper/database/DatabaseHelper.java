package com.vinberts.vinscraper.database;

import com.vinberts.vinscraper.database.models.Definition;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.Session;
import org.hibernate.Transaction;

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

    public static Optional<Definition> getDefinitionById(String id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        IdentifierLoadAccess<Definition> identifierLoadAccess = session.byId(Definition.class);
        Optional<Definition> gifOptional = identifierLoadAccess.loadOptional(id);
        session.close();
        return gifOptional;
    }
}
