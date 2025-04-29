package de.schnorrenbergers.automat.database;

import de.schnorrenbergers.automat.database.types.*;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class Database {
    private final SessionFactory sessionFactory;

    public Database() {
        sessionFactory = new Configuration().configure()
                .addAnnotatedClass(Setting.class)
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Kurs.class)
                .addAnnotatedClass(Wohnort.class)
                .addAnnotatedClass(Statistic.class)
                .addAnnotatedClass(Student.class)
                .addAnnotatedClass(Teacher.class)
                .addAnnotatedClass(Login.class)
                .addAnnotatedClass(Konto.class)
                .setProperty("show_sql", true)
                .buildSessionFactory();
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
