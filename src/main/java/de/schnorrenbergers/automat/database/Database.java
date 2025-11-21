package de.schnorrenbergers.automat.database;

import de.schnorrenbergers.automat.database.types.*;
import de.schnorrenbergers.automat.database.types.types.Attandance;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class Database {
    private final SessionFactory sessionFactory;

    /**
     * Initializes a new instance of the Database class and sets up the Hibernate SessionFactory.
     * This constructor configures Hibernate using the default configuration file and registers
     * various annotated entity classes.
     * <p>
     * The following entity classes are added to the Hibernate configuration:
     * - Setting
     * - User
     * - Kurs
     * - Wohnort
     * - Statistic
     * - Student
     * - Teacher
     * - Login
     * - Konto
     * <p>
     * Additional settings, such as enabling SQL logging, can also be configured here.
     * <p>
     * The resulting SessionFactory is used to manage database connections and transactions
     * throughout the application lifecycle.
     */
    public Database(String user, String filePW, String userPW) {
        String password = filePW + " " + userPW;
        System.out.println(password);
        Configuration configuration = new Configuration().configure()
                .setProperty("show_sql", true)
                .setProperty("connection.username", user)
                .setProperty("connection.password", password)
                .addAnnotatedClass(Setting.class)
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Kurs.class)
                .addAnnotatedClass(Wohnort.class)
                .addAnnotatedClass(Statistic.class)
                .addAnnotatedClass(Student.class)
                .addAnnotatedClass(Teacher.class)
                .addAnnotatedClass(Login.class)
                .addAnnotatedClass(Konto.class)
                .addAnnotatedClass(Sweet.class)
                .addAnnotatedClass(Attandance.class);
        sessionFactory = configuration
                .buildSessionFactory();
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
/*

                .setProperty("connection.username", user)
                .setProperty("connection.password", password)
 */
