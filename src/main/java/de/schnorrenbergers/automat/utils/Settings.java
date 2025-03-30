package de.schnorrenbergers.automat.utils;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Setting;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class Settings {

    @Deprecated
    public String getSetting(String adress) {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        List<Setting> resultList = session.createSelectionQuery("from Setting s where s.key = :adress", Setting.class).setParameter("adress", adress).getResultList();
        session.close();
        return resultList.getFirst().getValue();
    }

    public String getSettingOrDefault(String adress, String defaultValue) {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        List<Setting> resultList = session.createSelectionQuery("from Setting s where s.key = :adress", Setting.class).setParameter("adress", adress).getResultList();
        session.close();
        if (resultList.size() != 1) return defaultValue;
        return resultList.getFirst().getValue();
    }

    public boolean setSetting(String adress, String value) {
        SessionFactory sessionFactory = Main.getInstance().getDatabase().getSessionFactory();
        try {
            sessionFactory.inTransaction(session -> {
                List<Setting> resultList = session.createSelectionQuery("from Setting where key = :adress", Setting.class).setParameter("adress", adress).getResultList();
                int size = resultList.size();
                if (size == 0) {
                    throw new RuntimeException("No setting found for adress " + adress);
                }
                session.remove(resultList.getFirst());
                session.persist(new Setting(adress, value));
            });
        } catch (Exception e) {
            sessionFactory.inTransaction(session -> {
                session.persist(new Setting(adress, value));
            });
        }
        return true;
    }
}
