package com.vinberts.vinscraper.database;

import com.vinberts.vinscraper.database.models.Definition;
import com.vinberts.vinscraper.database.models.WordQueue;
import com.vinberts.vinscraper.utils.SystemPropertyLoader;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import java.util.Properties;

import static com.vinberts.vinscraper.utils.SystemPropertyLoader.DATABASE_PASS;
import static com.vinberts.vinscraper.utils.SystemPropertyLoader.DATABASE_URL;
import static com.vinberts.vinscraper.utils.SystemPropertyLoader.DATABASE_USERNAME;

/**
 *
 */
public class HibernateUtil {
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Properties systemProps = SystemPropertyLoader.getSystemProps();
                Configuration configuration = new Configuration();
                // Hibernate settings equivalent to hibernate.cfg.xml's properties
                Properties settings = new Properties();
                settings.put(Environment.DRIVER, "org.postgresql.Driver");
                settings.put(Environment.URL, systemProps.get(DATABASE_URL));
                settings.put(Environment.USER, systemProps.get(DATABASE_USERNAME));
                settings.put(Environment.PASS, systemProps.get(DATABASE_PASS));
                settings.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQL82Dialect");
                settings.put(Environment.SHOW_SQL, "false");
                settings.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
                settings.put(Environment.HBM2DDL_AUTO, "update");
                configuration.setProperties(settings);
                configuration.addAnnotatedClass(Definition.class);
                configuration.addAnnotatedClass(WordQueue.class);
                ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties()).build();
                sessionFactory = configuration.buildSessionFactory(serviceRegistry);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sessionFactory;
    }
}
