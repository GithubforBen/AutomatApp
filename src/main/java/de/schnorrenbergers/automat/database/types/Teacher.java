package de.schnorrenbergers.automat.database.types;

import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Level;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import de.schnorrenbergers.automat.manager.CipherManager;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.json.JSONObject;

import java.sql.Date;
import java.util.Objects;

@Entity
public class Teacher extends User {

    @Column(name = "mail")
    private String email;

    @Column(name = "password") // needs to be hashed
    private String password;

    @Enumerated(EnumType.STRING)
    private Level level;

    public Teacher(String firstName, String lastName, int[] rfid, Gender gender, Date age, Wohnort wohnort, String email, String password, Level level) throws Exception {
        super(firstName, lastName, rfid, gender, age, wohnort);
        this.email = email;
        this.password = new CipherManager().encrypt(password);
        this.level = level;
    }

    public Teacher() {

    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) throws Exception {
        this.password = new CipherManager().encrypt(password);
    }


    public static Teacher fromJSON(JSONObject jsonObject) throws Exception {
        Teacher teacher = new Teacher(
                jsonObject.getString("firstName"),
                jsonObject.getString("lastName"),
                jsonObject.getJSONArray("rfid").toList().stream().mapToInt((x) -> {
                    return Integer.parseInt(String.valueOf(x));
                }).toArray(),
                Gender.valueOf(jsonObject.getString("gender")),
                new Date(jsonObject.getLong("birthday")),
                Wohnort.fromJson(jsonObject.getJSONObject("address")),
                jsonObject.getString("email"),
                jsonObject.getString("password"),
                Level.valueOf(jsonObject.getString("level"))
        );
        return teacher;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = super.toJSON();
        jsonObject.put("mail", email);
        jsonObject.put("password", password);
        jsonObject.put("level", level);
        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Teacher teacher)) return false;
        return Objects.equals(email, teacher.email) && Objects.equals(password, teacher.password) && level == teacher.level && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, password, level);
    }
}