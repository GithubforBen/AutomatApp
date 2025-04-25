package de.schnorrenbergers.automat.database.types;

import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import org.json.JSONObject;

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

    public Student(String firstName, String lastName, int[] rfid, Gender gender, Date birthday, Wohnort wohnort, Kurs[] kurse) {
        super(firstName, lastName, rfid, gender, birthday, wohnort);
        this.kurse = List.of(kurse);
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

    @Override
    public String toJSONString() {
        JSONObject jsonObject = new JSONObject(super.toJSONString());
        jsonObject.put("kurse", kurse);
        return jsonObject.toString();
    }
}
