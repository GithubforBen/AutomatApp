package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Konto;
import de.schnorrenbergers.automat.database.types.User;
import de.schnorrenbergers.automat.database.types.types.Attandance;
import org.hibernate.Session;

import java.util.List;

public class KontenManager {

    /**
     * The id of the user this manager is associated with.
     */
    private Long id;

    public KontenManager(Long id) {
        this.id = id;
    }

    /**
     * Constructs a new {@code KontenManager} instance by retrieving the user ID
     * associated with the given RFID values. The user ID is determined by querying
     * the database for users whose RFID values match any of the provided RFID inputs.
     *
     * @param rfid an array of RFID integers used to identify the user. Each RFID value in the array
     *             is checked against the database to match with existing user records.
     */
    public KontenManager(int[] rfid) {
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            List<User> resultList = session.createSelectionQuery("from User u where u.rfid in :rfid", User.class)
                    .setParameter("rfid", rfid).getResultList();
            this.id = resultList.getFirst().getId();
        });
    }

    /**
     * Retrieves the {@code Konto} associated with the user ID of this {@code KontenManager}.
     * If no existing {@code Konto} is found for the user, a new {@code Konto} is instantiated
     * with a balance of 0 and the {@code isInfinite} flag set to {@code false}.
     *
     * @return the {@code Konto} associated with the user ID, or a new {@code Konto} if none exists.
     */
    public Konto getKonto() {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        List<Konto> resultList = session.createSelectionQuery("from Konto k where k.userId = :id", Konto.class)
                .setParameter("id", id).getResultList();
        if (resultList.isEmpty()) {
            session.close();
            return new Konto(id, 0, false);
        }
        session.close();
        return resultList.getFirst();
    }

    /**
     * Updates the specified {@code Konto} in the database.
     * This method merges the state of the provided {@code Konto} object with
     * the existing record in the database, ensuring the changes are persisted.
     *
     * @param konto the {@code Konto} instance to be updated in the database.
     *              It must not be null and should contain valid data to be persisted.
     */
    public void updateKonto(Konto konto) {
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            session.merge(konto);
        });
    }

    /**
     * Deposits the specified amount into the account (Konto) associated with the user ID
     * managed by this KontenManager. The account is retrieved using {@code getKonto()},
     * and the updated account data is saved to the database using {@code updateKonto()}.
     *
     * @param amount the amount to be deposited into the associated account. The amount must be non-negative.
     *               If the amount is less than 0, the deposit operation will fail.
     * @return true if the deposit was successful; false if the deposit amount was invalid.
     */
    public boolean deposit(double amount) {
        Konto konto = getKonto();
        boolean b = konto.deposit(amount);
        updateKonto(konto);
        return b;
    }

    /**
     * Withdraws a specified amount from the user's associated account (Konto).
     * The account is retrieved using {@code getKonto()}, and the withdrawal
     * operation is performed using the {@code withdraw} method of the {@code Konto} class.
     * After the operation, the updated account data is saved to the database
     * using {@code updateKonto()}.
     *
     * @param amount the amount to withdraw from the account. The amount must be less than
     *               or equal to the current account balance. If the amount exceeds the balance,
     *               the withdrawal will fail.
     * @return true if the withdrawal was successful; false otherwise.
     */
    public boolean withdraw(double amount) {
        Konto konto = getKonto();
        boolean b = konto.withdraw(amount);
        updateKonto(konto);
        return b;
    }

    /**
     * Checks whether attendance records contain an entry for the specified date.
     * <p>
     * This method iterates through the attendance timestamps retrieved from getKonto().
     * It converts each timestamp to a {@code Date} object and compares it to the provided day,
     * month, and year. If a match is found, the method returns {@code true}.
     *
     * @param day   the day of the month to check for attendance (1-31).
     * @param month the zero-based month of the year to check for attendance (0-11).
     * @param year  the year to check for attendance (e.g., 2023).
     * @return {@code true} if an attendance record exists for the specified date;
     * {@code false} otherwise.
     */
    public boolean checkAttendance(int day, int month, int year) {
        for (Attandance attendance : getKonto().getAttendances()) {
            if (attendance.getDay() == day && attendance.getMonth() == month && attendance.getYear() == year) {
                return !attendance.getType().equals(Attandance.Type.AWAY);
            }
        }
        return false;
    }

    /**
     * Adds a users attandance to the Users record.
     */
    public void attend(long l) {
        getKonto().attend(l);
    }
}
