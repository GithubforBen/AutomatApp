package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Statistic;
import de.schnorrenbergers.automat.database.types.User;
import de.schnorrenbergers.automat.database.types.types.StatisticType;
import org.json.JSONObject;

import java.util.List;

public class StatisticManager {

    /**
     * Persists the statistic.
     *
     * @param statistic the statistic to persist
     */
    public void persist(Statistic statistic) {
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            session.persist(statistic);
            session.flush();
        });
    }

    /**
     * Increments the sweet's dispense amount by one
     *
     * @param sweet sweet to be incremented
     */
    public void persistDispense(int sweet) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.append("type", sweet);
        Statistic statistic = new Statistic(jsonObject.toString(), StatisticType.SWEET_DISPENSE);
        persist(statistic);
    }

    public void persistLogin(long userId) {
        JSONObject jsonObject = new JSONObject();
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            List<User> id = session.createSelectionQuery("from User u where u.id = :id", User.class).setParameter("id", userId).getResultList();
            jsonObject.append("gender", id.getFirst().getGender());
            jsonObject.append("age", id.getFirst().getBirthday().getYear());
            jsonObject.append("zip", id.getFirst().getWohnort().getZip());
        });
        Statistic statistic = new Statistic(jsonObject.toString(), StatisticType.STUDENT_ATTEND);
        Statistic statistic_static = new Statistic(String.valueOf(userId), StatisticType.STUDENT_ATTEND_STATIC);
        persist(statistic);
        persist(statistic_static);
    }

    /**
     * @param id
     * @return sweet associated with the given id.
     */
    public String getFromId(int id) {
        return Main.getInstance().getConfigurationManager().getString("sweets._" + id + ".name");
    }
}
