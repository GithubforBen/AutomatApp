package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Sweet;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.hibernate.Session;

import java.util.List;
import java.util.logging.Logger;

public class AvailabilityManager {
    public AvailabilityManager() {
    }

    /**
     * Adds a specified amount of a sweet to the database. If the sweet type does not exist,
     * a new record is created provided the sweet type is valid. If multiple records exist
     * for the given sweet type, all records are removed, and no addition occurs.
     *
     * <p>This method ensures that only one record exists for a specified sweet type and updates
     * the total amount of sweets appropriately if the record already exists.
     *
     * @param sweet  the type of sweet to be added. Must be an integer within the valid range (e.g., 0 to 7).
     * @param amount the quantity of the sweet to add. This value is added to the sweet's existing total.
     */
    public void addSweet(int sweet, int amount) {
        System.out.println(3);
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            List<Sweet> sweets = session.createSelectionQuery("from Sweet s where s.type = :sweet", Sweet.class).setParameter("sweet", sweet).getResultList();
            if (sweets.isEmpty()) {
                if (sweet > -1 && sweet < 8) {
                    session.persist(new Sweet(sweet, amount));
                    return;
                }
                Logger.getGlobal().warning("Invalid sweet type: " + sweet);
                return;
            }
            if (sweets.size() != 1) {
                alert(sweet);
                Main.getInstance().getDatabase().getSessionFactory().inTransaction(transaction -> {
                    sweets.forEach(session::remove);
                });
                return;
            }
            Sweet first = sweets.getFirst();
            if (first.getAmount() + amount <= 0) {
                first.setAmount(0);
            } else {
                first.setAmount(first.getAmount() + amount);
            }
            session.merge(first);
        });
    }

    /**
     * Checks the availability of a specific type of sweet. If no record exists for the specified sweet,
     * a new record is created with a default amount of 0, provided the sweet type is valid (within the valid range).
     * If multiple records exist for the same sweet type, they are removed, and the availability check will return false.
     *
     * @param sweet the type of sweet to check the availability for. It must be an integer representing the sweet type.
     * @return {@code true} if the sweet is available (amount greater than 0);
     * {@code false} if the sweet is unavailable, invalid, or if multiple records are found and removed.
     */
    public boolean checkAvailability(int sweet) {
        if (sweet == 7) return true;
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        List<Sweet> select = session.createSelectionQuery("from Sweet s where s.type = :sweet", Sweet.class).setParameter("sweet", sweet).getResultList();
        if (select.isEmpty()) {
            if (sweet > -1 && sweet < 8) {
                session.persist(new Sweet(sweet, 0));
                session.close();
                return false;
            }
            Logger.getGlobal().warning("Invalid sweet type: " + sweet);
            return false;
        }
        if (select.size() != 1) {
            alert(sweet);
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(transaction -> {
                select.forEach(transaction::remove);
            });
            return false;
        }
        return select.getFirst().getAmount() > 0;
    }

    /**
     * Generates and displays a warning alert when multiple availability records are found for a specific sweet.
     * <p>
     * This method is used to notify the user about the detection of duplicate records for a specific sweet type.
     * The alert provides information about the sweet and warns that these duplicate records will be deleted.
     * The user is advised to recount the specified sweet to ensure database consistency.
     *
     * @param sweet the type of sweet for which duplicate records are detected. Must be an integer representing the sweet type.
     */
    private void alert(int sweet) {
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Multiple Availability Records found!");
            ConfigurationManager configurationManager = new ConfigurationManager();
            alert.setContentText("Multiple Availability Records found for Sweet " +
                    configurationManager.getString("sweets._" + sweet + ".name") + "(" + sweet + ").\n" +
                    "These repetitions will be deleted. Please recount the specified sweet.");
            alert.getButtonTypes().clear();
            alert.getButtonTypes().add(ButtonType.OK);
            alert.initOwner(Main.getInstance().getStage().getScene().getWindow());
            alert.show();
        });
    }
}
