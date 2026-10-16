package com.hercey.util;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.HashSet;
import java.util.Properties;

public class HibernateUtil {

    private static final SessionFactory sessionFactory;

    static {

        try {

            Configuration configuration = new Configuration().configure();
            Properties properties = configuration.getProperties();
            for (String name : new HashSet<>(properties.stringPropertyNames())) {
                properties.setProperty(name, Env.resolve(properties.getProperty(name)));
            }

            sessionFactory = configuration.buildSessionFactory();

        } catch (HibernateException e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError("SessionFactory creation failed: " + e.getMessage());
        }

    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }


}
