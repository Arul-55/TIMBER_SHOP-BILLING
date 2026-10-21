package com.timbershop.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final SessionFactory sessionFactory;

    static {
        try {
        	sessionFactory = new Configuration()
        	        .configure("hibernate.cfg.xml")
        	        .addAnnotatedClass(com.timbershop.entity.Timber.class)
        	        .addAnnotatedClass(com.timbershop.entity.Customer.class)
        	        .addAnnotatedClass(com.timbershop.entity.Bill.class)
        	        .addAnnotatedClass(com.timbershop.entity.BillItem.class)
        	        .buildSessionFactory();

        } catch (Throwable e) {
            System.err.println("SessionFactory creation failed.");
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}