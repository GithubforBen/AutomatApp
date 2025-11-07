package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Login;
import de.schnorrenbergers.automat.database.types.Student;
import de.schnorrenbergers.automat.database.types.User;
import org.hibernate.Session;

import java.util.List;

public class LoginManager {

    /**
     * @return true if the user just came in
     */
    public boolean login(Long userId) {
        System.out.println(userId);
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        List<Login> resultList = session.createSelectionQuery("from Login l where l.userId = :id", Login.class)
                .setParameter("id", userId).getResultList();
        resultList.forEach(System.out::println);
        new StatisticManager().persistLogin(userId);
        if (resultList.isEmpty()) {
            session.close();
            Main.getInstance().getDatabase().getSessionFactory().inTransaction((transaction) -> {
                transaction.persist(new Login(userId, System.currentTimeMillis()));
                List<Login> selectLogin = transaction.createSelectionQuery("from Login l", Login.class).getResultList();
            });
            return true;
        }
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((transaction) -> {
            resultList.forEach(transaction::remove);
        });
        long attendance = System.currentTimeMillis() - resultList.getFirst().getLoginTime();
        ConfigurationManager configurationManager = Main.getInstance().getConfigurationManager();
        if (attendance < 1000L * 60 * 60 * configurationManager.getInt("invalidation-time")) {
            new KontenManager(userId).deposit((double) attendance / 60 / 60 / 1000L);
            new KontenManager(userId).attend(System.currentTimeMillis());
            session.close();
            return false;
        }
        session.close();
        return false;
    }

    /**
     * Tries to find a user with the given rfid. If found, it will be logged in. using {@link #login(Long)}
     *
     * @param rfid
     * @return true if the user just came in, false if the user wasn't found or the user was already logged in.
     */
    public boolean login(int[] rfid) {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        for (User user : session.createSelectionQuery("from User u where u.rfid = :rfid", User.class)
                .setParameter("rfid", rfid).getResultList()) {
            session.close();
            return login(user.getId());
        }
        session.close();
        return false;
    }

    /**
     * Retrieves attendance data representing the number of currently logged-in users and the number
     * of users not currently logged in.
     *
     * @return an integer array where the first element is the total count of logged-in users
     * and the second element is the total count of users not logged in.
     */
    public int[] getAttendance() {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        List<Login> login = session.createSelectionQuery("from Login l", Login.class).getResultList();
        List<Student> students = session.createSelectionQuery("from Student s", Student.class).getResultList();
        session.close();
        return new int[]{login.size(), students.size() - login.size()};
    }
}
