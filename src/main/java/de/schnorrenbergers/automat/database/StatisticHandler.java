package de.schnorrenbergers.automat.database;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Statistic;
import de.schnorrenbergers.automat.database.types.types.LoginStat;
import de.schnorrenbergers.automat.database.types.types.StatisticType;
import org.json.JSONObject;

import java.util.HashMap;

public class StatisticHandler {

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
        Statistic statistic = new Statistic(String.valueOf(sweet), StatisticType.SWEET_DISPENSE);
        persist(statistic);
    }

    public void persistLogin(LoginStat loginStat) {
        JSONObject jsonObject = new JSONObject();
    }

    public HashMap<String, Integer> getStatisticDispenses() {
        HashMap<String, Integer> statisticDispenses = new HashMap<>();
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            session.createSelectionQuery("from Statistic s where s.type == 'SWEET_DISPENSE'", Statistic.class).list().forEach(statistic -> {
                String fromId = getFromId(Integer.parseInt(statistic.getData()));
                Integer i = statisticDispenses.getOrDefault(fromId, 0);
                if (statisticDispenses.replace(fromId, i + 1) == null) {
                    statisticDispenses.put(fromId, i + 1);
                }
            });
        });
        return statisticDispenses;
    }

    private String getFromId(int id) {
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
