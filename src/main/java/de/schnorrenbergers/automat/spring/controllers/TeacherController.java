package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Level;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import org.hibernate.Session;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @PostMapping(value = "/add", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addTeacher(@RequestBody(required = false) String body) {
        JSONObject jsonObject = parseJson(body);
        if (jsonObject == null) {
            return jsonError();
        }
        try {
            JSONObject address = jsonObject.getJSONObject("address");
            Wohnort wohnort = new Wohnort(
                    address.getInt("nr"),
                    address.getString("street"),
                    address.getString("city"),
                    address.getInt("zip"),
                    address.getString("country"));
            Teacher teacher = new Teacher(
                    jsonObject.getString("firstName"),
                    jsonObject.getString("lastName"),
                    jsonObject.getJSONArray("rfid").toList().stream().mapToInt((x) -> Integer.parseInt(String.valueOf(x))).toArray(),
                    Gender.valueOf(jsonObject.getString("gender")),
                    new Date(jsonObject.getLong("birthday")),
                    wohnort,
                    jsonObject.getString("email"),
                    jsonObject.getString("password"),
                    Level.valueOf(jsonObject.getString("level"))
            );
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                session.persist(wohnort);
                session.persist(teacher);
                session.flush();
            });
            return okText("Successfully added teacher");
        } catch (Exception e) {
            e.printStackTrace();
            return jsonError();
        }
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> allTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            if (session.createQuery("from Teacher t", Teacher.class).getResultList().isEmpty()) {
                Wohnort wohnort = new Wohnort(7, "test", "test", 678, "Germany");
                try {
                    Teacher teacher = new Teacher("Jon", "Doe", new int[]{100, 100, 100, 100}, Gender.OTHER, new Date(1999, 2, 1), wohnort, "test@gmail.com", "test", Level.ADMIN);
                    session.persist(wohnort);
                    session.persist(teacher);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            teachers.addAll(session.createSelectionQuery("from Teacher t", Teacher.class).getResultList());
        });
        StringBuilder response = new StringBuilder();
        response.append("{ \"teachers\": [");
        teachers.forEach(teacher -> {
            response.append(teacher.toJSON().toString());
            response.append(",");
        });
        if (!teachers.isEmpty()) response.replace(response.length() - 1, response.length(), "");
        response.append("] }");
        return okJson(response.toString());
    }

    @DeleteMapping(value = "/delete", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteTeacher(@RequestBody(required = false) String body) {
        JSONObject json = parseJson(body);
        if (json == null) {
            return jsonError();
        }
        long id = json.getLong("id");
        if (id == 0) {
            return badRequest();
        }
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        try {
            Teacher teacher = session.get(Teacher.class, id);
            if (teacher == null) {
                return badRequest();
            }
        } finally {
            session.close();
        }
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((session1 -> session1.remove(session1.get(Teacher.class, id))));
        return okText("Successfully deleted teacher");
    }

    @PostMapping(value = "/modify", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> modifyTeacher(@RequestBody(required = false) String body) {
        JSONObject jsonObject = parseJson(body);
        if (jsonObject == null) {
            return jsonError();
        }
        try {
            Teacher teacher = Teacher.fromJSON(jsonObject);
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                Teacher existing = session.get(Teacher.class, teacher.getId());
                if (existing == null) {
                    session.persist(teacher.getWohnort());
                    session.persist(teacher);
                    session.flush();
                    return;
                }
                if (teacher.equals(existing)) {
                    return;
                }
                session.merge(teacher.getWohnort());
                session.merge(teacher);
                session.flush();
            });
            return okText("Successfully added teacher");
        } catch (Exception e) {
            e.printStackTrace();
            return jsonError();
        }
    }

    // Backward compatibility with old endpoints
    @PostMapping(value = "/addTeacher", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addTeacherLegacy(@RequestBody(required = false) String body) {
        return addTeacher(body);
    }

    @GetMapping(value = "/allTeachers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> allTeachersLegacy() {
        return allTeachers();
    }

    @DeleteMapping(value = "/deleteTeacher", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteTeacherLegacy(@RequestBody(required = false) String body) {
        return deleteTeacher(body);
    }

    @PostMapping(value = "/modifyTeacher", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> modifyTeacherLegacy(@RequestBody(required = false) String body) {
        return modifyTeacher(body);
    }

    // Helper methods
    private JSONObject parseJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return new JSONObject(body);
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<String> okText(String body) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body);
    }

    private ResponseEntity<String> okJson(String body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private ResponseEntity<String> badRequest() {
        return ResponseEntity.status(400).contentType(MediaType.TEXT_PLAIN).body("BAD REQUEST: 400");
    }

    private ResponseEntity<String> jsonError() {
        return ResponseEntity.status(410).contentType(MediaType.TEXT_PLAIN).body("Can't parse JSON");
    }
}
