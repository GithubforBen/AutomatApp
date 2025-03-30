package de.schnorrenbergers.automat.database;

import de.schnorrenbergers.automat.database.types.Setting;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private final SessionFactory sessionFactory;

    public Database() throws SQLException, ClassNotFoundException {
        sessionFactory = new Configuration().configure()
                .addAnnotatedClass(Setting.class)
                .buildSessionFactory();
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }
}
