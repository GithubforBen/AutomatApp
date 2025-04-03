package de.schnorrenbergers.automat.database;

import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Setting;
import de.schnorrenbergers.automat.database.types.Statistic;
import de.schnorrenbergers.automat.database.types.User;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.sql.SQLException;

public class Database {
    private final SessionFactory sessionFactory;

    public Database() throws SQLException, ClassNotFoundException {
        sessionFactory = new Configuration().configure()
                .addAnnotatedClass(Setting.class)
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Kurs.class)
                .addAnnotatedClass(Wohnort.class)
                .addAnnotatedClass(Statistic.class)
                .buildSessionFactory();
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }
}
