package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Student;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.User;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import org.hibernate.Session;

import java.sql.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Warteschlange für neue Nutzer. Schüler*innen und Lehrkräfte werden nie direkt
 * angelegt: Die Website merkt sie hier vor, angelegt werden sie erst, wenn am
 * Automaten unter "Nutzer hinzufügen" ihre Chipkarte gescannt wird (siehe
 * {@link #create(UserAdd, int[], Session)}). So hat jede Person eine Karte.
 * <p>
 * Die Warteschlange lebt nur im Speicher - nach einem Neustart des Automaten ist
 * sie leer.
 */
public class AddUserHandler {
    public static Queue<UserAdd> addQueue = new ConcurrentLinkedQueue<>();

    public static void add(UserAdd add) {
        addQueue.add(add);
    }

    /**
     * Ist die E-Mail-Adresse schon vergeben - von einer Lehrkraft in der Datenbank
     * oder einer, die noch in der Warteschlange auf ihre Karte wartet?
     */
    public static boolean isEmailTaken(String email) {
        String wanted = email.trim().toLowerCase();
        for (UserAdd queued : addQueue) {
            if (queued.isTeacher() && queued.teacher.getEmail().trim().toLowerCase().equals(wanted)) return true;
        }
        try (Session session = Main.getInstance().getDatabase().getSessionFactory().openSession()) {
            return session.createSelectionQuery(
                            "select count(*) from Teacher t where lower(t.email) = :email", Long.class)
                    .setParameter("email", wanted).getSingleResult() > 0;
        }
    }

    /**
     * Legt einen vorgemerkten Nutzer mit der gescannten Karte an.
     *
     * @return den angelegten Nutzer
     */
    public static User create(UserAdd add, int[] rfid, Session session) {
        session.persist(add.getWohnort());
        User user;
        if (add.isTeacher()) {
            add.teacher.setRfid(rfid);
            user = add.teacher;
        } else {
            user = new Student(add.getVorname(), add.getNachname(), rfid, add.getGender(), add.getDate(),
                    add.getWohnort(), add.getKurse());
        }
        session.persist(user);
        session.flush();
        return user;
    }

    public static class UserAdd {
        private Wohnort wohnort;
        private String vorname;
        private String nachname;
        private Gender gender;
        private Date date;
        private List<Kurs> kurse;
        /**
         * Bei Lehrkräften die fertige, noch nicht gespeicherte Lehrkraft - das
         * Passwort ist darin schon gehasht, im Speicher liegt es also nie im Klartext.
         */
        private Teacher teacher;

        public UserAdd(Wohnort wohnort, String vorname, String nachname, Gender gender, Date date, List<Kurs> kurse) {
            this.wohnort = wohnort;
            this.vorname = vorname;
            this.nachname = nachname;
            this.gender = gender;
            this.date = date;
            this.kurse = kurse;
        }

        /**
         * Eine Lehrkraft vormerken. Ihre Karte wird beim Anlegen gesetzt.
         */
        public static UserAdd teacher(Teacher teacher) {
            UserAdd add = new UserAdd(teacher.getWohnort(), teacher.getFirstName(), teacher.getLastName(),
                    teacher.getGender(), teacher.getBirthday(), List.of());
            add.teacher = teacher;
            return add;
        }

        public boolean isTeacher() {
            return teacher != null;
        }

        public Wohnort getWohnort() {
            return wohnort;
        }

        public void setWohnort(Wohnort wohnort) {
            this.wohnort = wohnort;
        }

        public String getVorname() {
            return vorname;
        }

        public void setVorname(String vorname) {
            this.vorname = vorname;
        }

        public String getNachname() {
            return nachname;
        }

        public void setNachname(String nachname) {
            this.nachname = nachname;
        }

        public Gender getGender() {
            return gender;
        }

        public void setGender(Gender gender) {
            this.gender = gender;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public List<Kurs> getKurse() {
            return kurse;
        }

        public void setKurse(List<Kurs> kurse) {
            this.kurse = kurse;
        }
    }
}
