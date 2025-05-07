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
        switch (id) {
            case 0 -> {
                return "Mentos";
            }
            case 1 -> {
                return "Dublo";
            }
            case 2 -> {
                return "Kinder";
            }
            case 3 -> {
                return "Maoam";
            }
            case 4 -> {
                return "Smarties";
            }
            case 5 -> {
                return "Haribo";
            }
            case 6 -> {
                return "Brause";
            }
            case 7 -> {
                return "Stats";
            }
            default -> {
                return "";
            }
        }
    }
}
