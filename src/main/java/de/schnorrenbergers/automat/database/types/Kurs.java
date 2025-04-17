package de.schnorrenbergers.automat.database.types;

import de.schnorrenbergers.automat.database.types.types.Day;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class Kurs {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToMany
    private List<Teacher> tutor;

    @Enumerated(EnumType.STRING)
    private Day day;

    public Kurs(String name, List<Teacher> tutor, Day day) {
        this.name = name;
        this.tutor = tutor;
        this.day = day;
    }

    public Kurs() {}

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Kurs{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", tutor=" + tutor +
                ", day=" + day +
                '}';
    }
}
