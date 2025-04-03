package de.schnorrenbergers.automat.database.types;

import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import jakarta.persistence.*;
import org.json.JSONObject;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "firstName")
    private String firstName;

    @Column(name = "lastName")
    private String lastName;

    @Column(name = "rifd")
    private int[] rfid;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "age")
    private Date birthday;

    @OneToOne
    private Wohnort wohnort;

    @ManyToMany
    private List<Kurs> kurse;

    public User(String firstName, String lastName, int[] rfid, Gender gender, Date birthday, Wohnort wohnort, Kurs[] kurse) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.rfid = rfid;
        this.gender = gender;
        this.birthday = birthday;
        this.wohnort = wohnort;
        this.kurse = new ArrayList<>();
        this.kurse.addAll(Arrays.stream(kurse).toList());
    }

    public User(String firstName, String lastName, int[] rfid, Gender gender, Date birthday, Wohnort wohnort, List<Kurs> kurse) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.rfid = rfid;
        this.gender = gender;
        this.birthday = birthday;
        this.wohnort = wohnort;
        this.kurse = kurse;
    }

    public User() {}

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int[] getRfid() {
        return rfid;
    }

    public void setRfid(int[] rfid) {
        this.rfid = rfid;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date age) {
        this.birthday = age;
    }

    public Wohnort getWohnort() {
        return wohnort;
    }

    public void setWohnort(Wohnort wohnort) {
        this.wohnort = wohnort;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", rfid=" + Arrays.toString(rfid) +
                ", gender=" + gender +
                ", age=" + birthday +
                ", wohnort=" + wohnort +
                ", kurse=" + Arrays.toString(kurse.toArray()) +
                '}';
    }

    public String toJSONString() {
        JSONObject jsonObject = new JSONObject(this);
        return jsonObject.toString();
                /*
                "{ \"id\":" + id + "," +
                "\"firstName\":" + firstName + "," +

                 */

    }
}
