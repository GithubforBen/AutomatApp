package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
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
        // Früher wurde hier bei leerer Datenbank ein Admin "Jon Doe" mit bekanntem
        // Passwort angelegt. Den ersten Admin legt jetzt die erste Person selbst an
        // (siehe createFirstAdmin).
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session ->
                teachers.addAll(session.createSelectionQuery("from Teacher t", Teacher.class).getResultList()));
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
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session1 -> {
            Teacher teacher = session1.get(Teacher.class, id);
            List<Kurs> courses = session1.createSelectionQuery(
                            "from Kurs k where :t member of k.tutor", Kurs.class)
                    .setParameter("t", teacher).getResultList();
            for (Kurs k : courses) {
                k.getTutor().remove(teacher);
                session1.merge(k);
            }
            session1.remove(teacher);
        });
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
            boolean[] notFound = {false};
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                Teacher existing = session.get(Teacher.class, teacher.getId());
                if (existing == null) {
                    notFound[0] = true;
                    return;
                }
                if (teacher.equals(existing)) {
                    return;
                }
                session.merge(teacher.getWohnort());
                session.merge(teacher);
                session.flush();
            });
            if (notFound[0]) {
                return badRequest();
            }
            return okText("Successfully added teacher");
        } catch (Exception e) {
            e.printStackTrace();
            return jsonError();
        }
    }

    /**
     * Gibt an, ob es schon mindestens eine Lehrkraft gibt. Solange nicht, darf
     * sich die erste Person über {@link #createFirstAdmin(String)} einen
     * Admin-Zugang anlegen.
     */
    @GetMapping(value = "/exists", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> exists() {
        return okJson(new JSONObject().put("exists", countTeachers() > 0).toString());
    }

    /**
     * Legt den ersten Admin-Zugang an - aber nur, solange es noch gar keine
     * Lehrkraft gibt. Prüfen und Anlegen passieren gemeinsam (synchronized und in
     * einer Transaktion), damit nicht zwei Personen gleichzeitig "die erste" sind.
     * Body: firstName, lastName, email, password (Klartext, wird gehasht), gender.
     * Antwort: die neue Lehrkraft als JSON; 409, wenn es schon eine gibt.
     */
    @PostMapping(value = "/createFirstAdmin", produces = MediaType.APPLICATION_JSON_VALUE)
    public synchronized ResponseEntity<String> createFirstAdmin(@RequestBody(required = false) String body) {
        JSONObject json = parseJson(body);
        if (json == null) {
            return jsonError();
        }
        String firstName = json.optString("firstName").trim();
        String lastName = json.optString("lastName").trim();
        String email = json.optString("email").trim();
        String password = json.optString("password");
        Gender gender;
        try {
            gender = Gender.valueOf(json.optString("gender"));
        } catch (IllegalArgumentException e) {
            return badRequest();
        }
        if (firstName.isEmpty() || lastName.isEmpty() || !email.contains("@") || password.length() < 8) {
            return badRequest();
        }

        Teacher[] created = {null};
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            Long count = session.createSelectionQuery("select count(*) from Teacher t", Long.class).getSingleResult();
            if (count > 0) {
                return;
            }
            try {
                Wohnort wohnort = new Wohnort(0, "", "", 0, "Deutschland");
                Teacher teacher = new Teacher(firstName, lastName, new int[0], gender,
                        new Date(0), wohnort, email, password, Level.ADMIN);
                session.persist(wohnort);
                session.persist(teacher);
                session.flush();
                created[0] = teacher;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (created[0] == null) {
            return ResponseEntity.status(409).contentType(MediaType.TEXT_PLAIN).body("A teacher already exists");
        }
        return okJson(created[0].toJSON().toString());
    }

    private long countTeachers() {
        try (Session session = Main.getInstance().getDatabase().getSessionFactory().openSession()) {
            return session.createSelectionQuery("select count(*) from Teacher t", Long.class).getSingleResult();
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
