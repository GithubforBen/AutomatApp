package de.schnorrenbergers.automat.database.types;

import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import de.schnorrenbergers.automat.manager.KontenManager;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
                "rfid=" + Arrays.toString(getRfid()) +
                "kurse=" + kurse +
                '}';
    }

    public static Student fromJSON(JSONObject jsonObject) {
        Student student = new Student();
        student.setId(jsonObject.getLong("id"));
        student.setFirstName(jsonObject.getString("firstName"));
        student.setLastName(jsonObject.getString("lastName"));
        student.setRfid(jsonObject.getJSONArray("rfid").toList().stream().mapToInt((x) -> {
            if (x instanceof Integer) return (int) x;
            throw new IllegalArgumentException("Invalid rfid: " + x);
        }).toArray());
        student.setGender(Gender.valueOf(jsonObject.getString("gender")));
        student.setBirthday(new Date(jsonObject.getLong("birthday")));
        student.setWohnort(Wohnort.fromJson(jsonObject.getJSONObject("wohnort")));
        List<Kurs> kurse1 = jsonObject.getJSONArray("kurse").toList().stream().map((x) -> {
            try {
                return Kurs.fromJSON((JSONObject) x);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();
        student.setKurse(kurse1);
        return student;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = super.toJSON();
        KontenManager manager = new KontenManager(getId());
        jsonObject.append("hours", manager.getKonto().getBalanceRounded());
        jsonObject.put("kurse", new JSONArray(kurse.stream().map(Kurs::toJSON).toArray()));
        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Student student)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(kurse, student.kurse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), kurse);
    }
}
