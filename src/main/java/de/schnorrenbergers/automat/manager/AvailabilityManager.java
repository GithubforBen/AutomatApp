package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import org.hibernate.Session;

public class AvailabilityManager {
    public AvailabilityManager() {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
    }
}
