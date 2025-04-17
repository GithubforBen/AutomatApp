package de.schnorrenbergers.automat.database.types;

import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.sql.Date;
import java.util.List;

@Entity
public class Student extends User{

    @ManyToMany
    private List<Kurs> kurse;

    public Student(String firstName, String lastName, int[] rfid, Gender gender, Date birthday, Wohnort wohnort, List<Kurs> kurse) {
        super(firstName, lastName, rfid, gender, birthday, wohnort);
        this.kurse = kurse;
    }

    public Student() {
    }

    public List<Kurs> getKurse() {
        return kurse;
    }

    public void setKurse(List<Kurs> kurse) {
        this.kurse = kurse;
    }

    @Override
    public String toString() {
        return "Student{" +
                "kurse=" + kurse +
                '}';
    }
}
