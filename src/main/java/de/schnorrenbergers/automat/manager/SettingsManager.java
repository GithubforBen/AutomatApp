package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Setting;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class SettingsManager {

    /**
     * Retrieves the value of a setting for the provided address. This method
     * is deprecated and may be removed in future versions.
     *
     * @param address The address (key) associated with the setting to retrieve.
     * @return The value of the setting corresponding to the given address,
     *         or null if the setting does not exist.
     * @deprecated Use {@link #getSettingOrDefault(String, String)} instead for
     *             better handling of non-existing settings.
     */
    @Deprecated
    public String getSetting(String address) {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        List<Setting> resultList = session.createSelectionQuery("from Setting s where s.key = :adress", Setting.class).setParameter("address", address).getResultList();
        session.close();
        return resultList.getFirst().getValue();
    }

    /**
     * Returns a setting.
     *
     * @param adress       The address associated with the setting
     * @param defaultValue This value will be returned if the setting wasn't found
     * @return The setting
     */
    public String getSettingOrDefault(String adress, String defaultValue) {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        List<Setting> resultList = session.createSelectionQuery("from Setting s where s.key = :address", Setting.class).setParameter("address", adress).getResultList();
        session.close();
        if (resultList.size() != 1) return defaultValue;
        return resultList.getFirst().getValue();
    }

    /**
     * Sets a setting
     *
     * @param address The address where the setting should be saved.
     * @param value   The Value of the setting.
     * @return true if a setting was replaced; false if the setting is new
     */
    public boolean setSetting(String address, String value) {
        SessionFactory sessionFactory = Main.getInstance().getDatabase().getSessionFactory();
        try {
            sessionFactory.inTransaction(session -> {
                List<Setting> resultList = session.createSelectionQuery("from Setting where key = :address", Setting.class).setParameter("address", address).getResultList();
                int size = resultList.size();
                if (size == 0) {
                    throw new RuntimeException("No setting found for address " + address);
                }
                resultList.forEach(session::remove);
                session.flush();
                session.persist(new Setting(address, value));
            });
        } catch (Exception e) {
            sessionFactory.inTransaction(session -> {
                session.persist(new Setting(address, value));
            });
            return false;
        }
        return true;
    }
}
