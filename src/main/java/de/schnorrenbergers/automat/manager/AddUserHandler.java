package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Wohnort;

import java.sql.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class AddUserHandler {
    public static Queue<UserAdd> addQueue = new PriorityQueue();

    public static void add(UserAdd add) {
        addQueue.add(add);
    }

    public static class UserAdd {
        private Wohnort wohnort;
        private String vorname;
        private String nachname;
        private Gender gender;
        private Date date;
        private List<Kurs> kurse;

        public UserAdd(Wohnort wohnort, String vorname, String nachname, Gender gender, Date date, List<Kurs> kurse) {
            this.wohnort = wohnort;
            this.vorname = vorname;
            this.nachname = nachname;
            this.gender = gender;
            this.date = date;
            this.kurse = kurse;
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
