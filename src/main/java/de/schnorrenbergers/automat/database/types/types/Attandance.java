package de.schnorrenbergers.automat.database.types.types;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Attandance {

    @Id
    @GeneratedValue
    private Long id;
    private int day;
    private int month;
    private int year;
    private long login;
    private long logout;
    private Type type;
    public Attandance() {
    }

    public Attandance(int day, int month, int year, long login, Type type) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.login = login;
    }

    public void logout(long logout) {
        this.logout = logout;
    }

    public double attandance() {
        return (logout - login) / 1000.0 / 60.0 / 60.0;
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public long getLogin() {
        return login;
    }

    public long getLogout() {
        return logout;
    }

    public Type getType() {
        return type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public enum Type {
        NORMAL, AWAY, EXCUSED
    }
}
