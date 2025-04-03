package de.schnorrenbergers.automat.database.types;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Level;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import jakarta.persistence.*;

import java.sql.Date;
import java.util.List;

@Entity
@Table(name = "teatcher")
public class Teacher extends User {

    @Column(name = "mail")
    private String email;

    @Column(name = "password") // needs to be hashed
    private String password;

    @Enumerated(EnumType.STRING)
    private Level level;

    public Teacher(String firstName, String lastName, int[] rfid, Gender gender, Date age, Wohnort wohnort, Kurs[] kurse, String email, String password, Level level) {
        super(firstName, lastName, rfid, gender, age, wohnort, kurse);
        this.email = email;
        this.password = password;
    }

    public Teacher(String firstName, String lastName, int[] rfid, Gender gender, Date age, Wohnort wohnort, List<Kurs> kurse, String email, String password, Level level) {
        super(firstName, lastName, rfid, gender, age, wohnort, kurse);
        this.email = email;
        this.password = password;
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

    public void setPassword(String password) {
        this.password = password;
    }

}
