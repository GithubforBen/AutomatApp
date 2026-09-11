package de.schnorrenbergers.automat.database.types;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.types.Attandance;
import jakarta.persistence.*;
import org.hibernate.Session;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Entity
public class Konto {
    @Id
    @GeneratedValue
    private Long id;

    private Long userId;
    private double balance;
    private boolean isInfinite;
    @OneToMany(cascade = CascadeType.ALL)
    private List<Attandance> attendances;

    public Konto(Long userId, double balance, boolean isInfinite) {
        this.userId = userId;
        this.balance = balance;
        this.isInfinite = isInfinite;
        this.attendances = new LinkedList<>();
    }

    public Konto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean withdraw(double amount) {
        if (amount > balance) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public boolean deposit(double amount) {
        if (amount < 0) {
            return false;
        }
        balance += amount;
        return true;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isInfinite() {
        return isInfinite;
    }

    public void setInfinite(boolean infinite) {
        isInfinite = infinite;
    }

    public double getBalanceRounded() {
        return ((int) (balance * 10)) / 10.0;
    }

    /**
     * Kommen an der Station: ab jetzt zählt der Tag als anwesend - auch wenn er
     * vorher als entschuldigt oder abwesend eingetragen war.
     */
    public void checkIn(long time) {
        Attandance today = attendanceOn(time);
        if (today == null) {
            LocalDate date = toDate(time);
            getAttendancesOrCreate().add(new Attandance(date.getDayOfMonth(), date.getMonthValue(), date.getYear(), time, Attandance.Type.NORMAL));
            return;
        }
        today.setType(Attandance.Type.NORMAL);
    }

    /**
     * Gehen an der Station. Normalerweise gibt es vom Kommen schon einen Eintrag;
     * fehlt er (z.B. bei Altdaten), wird der Tag trotzdem als anwesend erfasst.
     */
    public void checkOut(long time) {
        Attandance today = attendanceOn(time);
        if (today == null) {
            checkIn(time);
            today = attendanceOn(time);
        }
        today.setType(Attandance.Type.NORMAL);
        today.logout(time);
    }

    private Attandance attendanceOn(long time) {
        // Früher standen hier Date.getDay()/getMonth()/getYear() - das sind
        // Wochentag, Monat ab 0 und Jahr ab 1900, also völlig andere Werte als
        // die, mit denen die Website Anwesenheiten speichert und sucht.
        LocalDate date = toDate(time);
        for (Attandance attendance : getAttendancesOrCreate()) {
            if (attendance.isOn(date.getDayOfMonth(), date.getMonthValue(), date.getYear())) {
                return attendance;
            }
        }
        return null;
    }

    private List<Attandance> getAttendancesOrCreate() {
        if (attendances == null) {
            attendances = new ArrayList<>();
            System.out.println("Attendance list is empty it was fixed but it might be a type of problem.");
        }
        return attendances;
    }

    private static LocalDate toDate(long time) {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public User getUser() {
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        User user = session.get(User.class, userId);
        session.close();
        return user;
    }

    @Override
    public String toString() {
        return "Konto{" +
                "id=" + id +
                ", userId=" + userId +
                ", balance=" + balance +
                ", isInfinite=" + isInfinite +
                ", attendances=" + attendances +
                '}';
    }

    public List<Attandance> getAttendances() {
        return attendances;
    }
}
