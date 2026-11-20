package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.User;
import de.schnorrenbergers.automat.utils.CustomRequest;
import org.hibernate.Session;

import java.util.List;
import java.util.logging.Logger;

/**
 * Alarm des Automaten: Der Summer sitzt im Scanner und wird über dessen
 * {@code /alarm_on} bzw. {@code /alarm_off} geschaltet.
 * <p>
 * Ausgelöst wird er, wenn eine Lichtschranke der Ausgabe anspricht, ohne dass
 * gerade etwas ausgegeben wird (jemand greift in den Automaten). Aus geht er
 * über die PIN des Admin-Bereichs oder wenn eine Lehrkraft bzw. ein Admin die
 * eigene Karte scannt.
 */
public final class AlarmManager {

    private static final Logger LOG = Logger.getLogger(AlarmManager.class.getName());

    private AlarmManager() {
    }

    /**
     * Schaltet den Alarm ein. Wird auch erneut gesendet, wenn er schon läuft -
     * falls der Scanner zwischendurch neu gestartet ist.
     *
     * @param reason wofür der Alarm ausgelöst wurde (nur fürs Protokoll)
     */
    public static void trigger(String reason) {
        LOG.warning("Alarm ausgelöst: " + reason);
        Main.getInstance().setAlarm(true);
        sendAsync(true);
    }

    /**
     * Schaltet den Alarm aus, falls er läuft. Die Anfrage an den Scanner läuft im
     * Hintergrund, damit die Oberfläche nicht wartet.
     *
     * @param reason wodurch der Alarm beendet wurde (nur fürs Protokoll)
     * @return ob der Alarm lief
     */
    public static boolean disarm(String reason) {
        if (!Main.getInstance().isAlarm()) return false;
        LOG.info("Alarm aus: " + reason);
        Main.getInstance().setAlarm(false);
        sendAsync(false);
        return true;
    }

    /**
     * Beendet den Alarm, wenn die gescannte Karte einer Lehrkraft oder einem Admin
     * gehört (Admins sind Lehrkräfte mit Admin-Stufe).
     *
     * @return ob der Alarm dadurch beendet wurde
     */
    public static boolean disarmByCard(int[] rfid) {
        if (rfid == null || !Main.getInstance().isAlarm()) return false;
        User user = findUser(rfid);
        if (!(user instanceof Teacher)) return false;
        return disarm("Karte von " + user.getFullName());
    }

    /**
     * Schaltet den Summer am Scanner und wartet auf dessen Antwort.
     *
     * @return ob der Scanner mit "success" bestätigt hat
     */
    public static boolean send(boolean on) {
        try {
            String answer = new CustomRequest(on ? "alarm_on" : "alarm_off", CustomRequest.REVIVER.SCANNER).execute();
            return "success".equals(answer);
        } catch (Exception e) {
            LOG.warning("Scanner für den Alarm nicht erreichbar: " + e.getMessage());
            return false;
        }
    }

    private static void sendAsync(boolean on) {
        new Thread(() -> send(on), on ? "alarm-on" : "alarm-off").start();
    }

    private static User findUser(int[] rfid) {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        try {
            List<User> users = session.createSelectionQuery("from User u where u.rfid = :rfid", User.class)
                    .setParameter("rfid", rfid).getResultList();
            return users.isEmpty() ? null : users.getFirst();
        } finally {
            session.close();
        }
    }
}
