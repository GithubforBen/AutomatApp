package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Statistic;
import de.schnorrenbergers.automat.database.types.types.StatisticType;
import org.json.JSONObject;

public class StatisticManager {

    /**
    * Persists the statistic.
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
     * @param sweet sweet to be incremented
     */
    public void persistDispense(int sweet) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.append("type", sweet);
        Statistic statistic = new Statistic(jsonObject.toString(), StatisticType.SWEET_DISPENSE);
        persist(statistic);
    }

    //TODO: set User data correctly
    public void persistLogin(long userId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.append("userID", userId);
        Statistic statistic = new Statistic(jsonObject.toString(), StatisticType.STUDENT_ATTEND);
        persist(statistic);
    }

    /**
     * @param id
     * @return sweet associated with the given id.
     */
    public String getFromId(int id) {
        return Main.getInstance().getConfigurationManager().getString("sweets._" + id + ".name");
    }
}
