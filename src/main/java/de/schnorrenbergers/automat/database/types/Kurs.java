package de.schnorrenbergers.automat.database.types;

import de.schnorrenbergers.automat.database.types.types.Day;
import jakarta.persistence.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Represents a course entity in the database.
 * <p>
 * This class is annotated as an entity using JPA annotations
 * and is used to store and manage information about courses.
 * A course has a unique identifier, a name, a list of tutors,
 * and a specific day on which it is scheduled.
 * <p>
 * Fields:
 * - id: Unique identifier for the course (auto-generated).<p>
 * - name: Name of the course.<p>
 * - tutor: List of Teacher objects representing the tutors for this course.<p>
 * - Day: Enum value representing the day of the week the course is held.<p>
 * <p>
 * Constructors:
 * - A default no-arg constructor.<p>
 * - A parameterized constructor to initialize the course with a name, a list of tutors, and a day.<p>
 * <p>
 * Methods:
 * - Getters and setters for all fields.<p>
 * - Overrides the toString method to provide a string representation of the course.<p>
 */
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

    private Long creationDate;

    public Kurs(String name, List<Teacher> tutor, Day day) {
        this.name = name;
        this.tutor = tutor;
        this.day = day;
        this.creationDate = System.currentTimeMillis();
    }

    public Kurs() {}

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Teacher> getTutor() {
        return tutor;
    }

    public void setTutor(List<Teacher> tutor) {
        this.tutor = tutor;
    }

    public Day getDay() {
        return day;
    }

    public void setDay(Day day) {
        this.day = day;
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

    public static Kurs fromJSON(JSONObject jsonObject) {
        Kurs kurs = new Kurs();
        kurs.setId(jsonObject.getLong("id"));
        kurs.setName(jsonObject.getString("name"));
        kurs.setDay(Day.valueOf(jsonObject.getString("day")));
        List<Teacher> tutor1 = jsonObject.getJSONArray("tutor").toList().stream().map((x) -> {
            try {
                return Teacher.fromJSON((JSONObject) x);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();
        kurs.setTutor(tutor1);
        return kurs;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("name", name);
        jsonObject.put("day", day);
        jsonObject.put("tutor", new JSONArray(tutor.stream().map(Teacher::toJSON).toArray()));
        return jsonObject;
    }
}
