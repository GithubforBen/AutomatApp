package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Konto;
import de.schnorrenbergers.automat.database.types.Login;
import de.schnorrenbergers.automat.database.types.User;
import org.hibernate.Session;

import java.util.List;

public class LoginManager {

    /**
     * @return true if the user just came in
     */
    public boolean login(Long userId) {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        List<Login> resultList = session.createSelectionQuery("from Login l where l.userId == :id", Login.class)
                .setParameter("id", userId).getResultList();
        if (resultList.isEmpty()) {
            session.persist(new Login(userId, System.currentTimeMillis()));
            session.flush();
            session.close();
            return true;
        }
        long attendance = System.currentTimeMillis() - resultList.getFirst().getLoginTime();
        ConfigurationManager configurationManager = Main.getInstance().getConfigurationManager();
        if (attendance < 1000L * 60 * 60 * configurationManager.getInt("invalidation-time")) {
            List<Konto> konten = session.createSelectionQuery("from Konto k where k.userId == :id", Konto.class)
                    .setParameter("id", userId).getResultList();
            if (konten.isEmpty()) {
                session.persist(new Konto(userId, ((double) attendance / 60 / 60 / 1000), false));
            } else {
                konten.forEach(session::remove);
                new KontenManager(userId).deposit((double) attendance / 60 / 60 / 1000);
            }
            return true;

        }
        new StatisticManager().persistLogin(userId);
        resultList.forEach(session::remove);
        session.flush();
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
}
