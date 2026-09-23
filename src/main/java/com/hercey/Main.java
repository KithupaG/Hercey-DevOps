package com.hercey;

import com.hercey.listener.ContextPathListener;
import com.hercey.middleware.AuthAccessFilter;
import com.hercey.util.HibernateUtil;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.File;

public class Main {

    private static final String CONTEXT_PATH = "/hercey";

    public static void main(String[] args) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

        try {
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(8080);
            tomcat.getConnector();

            Context context = tomcat.addWebapp(CONTEXT_PATH, new File("src/main/webapp").getAbsolutePath());

            context.addApplicationListener(ContextPathListener.class.getName());
            context.addApplicationListener(AuthAccessFilter.class.getName());

            tomcat.start();
            System.out.println("App URL: http://localhost:8080" + CONTEXT_PATH);
            tomcat.getServer().await();
            try (Session s = HibernateUtil.getSessionFactory().openSession()){}

        } catch (LifecycleException e) {
            throw new RuntimeException("Tomcat Embedded Server loading failed: " + e.getMessage());
        }
    }
}
