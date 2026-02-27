package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Student;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import de.schnorrenbergers.automat.manager.AddUserHandler;
import org.hibernate.Session;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student")
public class StudentController {

    @PostMapping(value = "/add", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addStudent(@RequestBody(required = false) String body) {
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
            AddUserHandler.add(new AddUserHandler.UserAdd(
                    wohnort,
                    jsonObject.getString("firstName"),
                    jsonObject.getString("lastName"),
                    Gender.valueOf(jsonObject.getString("gender")),
                    new Date(jsonObject.getLong("birthday")),
                    jsonObject.getJSONArray("kurse").toList().stream().map((x) -> {
                        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
                        Kurs k = session.get(Kurs.class, Long.valueOf(String.valueOf(x)));
                        session.close();
                        return k;
                    }).collect(Collectors.toList())
            ));
            return okText("Successfully added student");
        } catch (Exception e) {
            e.printStackTrace();
            return jsonError();
        }
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> allStudents() {
        List<Student> users = new ArrayList<>();
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        try {
            users.addAll(session.createSelectionQuery("from Student u", Student.class).getResultList());
            StringBuilder response = new StringBuilder();
            response.append("{ \"students\": [");
            users.forEach(user -> {
                response.append(user.toJSON().toString());
                response.append(",");
            });
            if (!users.isEmpty()) response.replace(response.length() - 1, response.length(), "");
            response.append("] }");
            return okJson(response.toString());
        } finally {
            session.close();
        }
    }

    @DeleteMapping(value = "/delete", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteStudent(@RequestBody(required = false) String body) {
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
            Student student = session.get(Student.class, id);
            if (student == null) {
                return badRequest();
            }
        } finally {
            session.close();
        }
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((session1 -> session1.remove(session1.get(Student.class, id))));
        return okText("Successfully deleted Student");
    }

    @PostMapping(value = "/modify", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> modifyStudent(@RequestBody(required = false) String body) {
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
            Student student = session.get(Student.class, id);
            if (student == null) {
                return badRequest();
            }
            if (json.has("gender")) student.setGender(Gender.valueOf(json.getString("gender")));
            if (json.has("firstName")) student.setFirstName(json.getString("firstName"));
            if (json.has("lastName")) student.setLastName(json.getString("lastName"));
            if (json.has("rfid")) {
                student.setRfid(json.getJSONArray("rfid").toList().stream().mapToInt((x) -> (int) x).toArray());
            }
            if (json.has("birthday")) student.setBirthday(new Date(json.getLong("birthday")));
            if (json.has("wohnort")) student.setWohnort(Wohnort.fromJson(json.getJSONObject("wohnort")));
            if (json.has("kurse")) {
                List<Kurs> kurse1 = json.getJSONArray("kurse").toList().stream().map((x) -> {
                    try {
                        return Kurs.fromJSON((JSONObject) x);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
                student.setKurse(kurse1);
            }
            session.getTransaction().begin();
            session.merge(student);
            session.getTransaction().commit();
        } finally {
            session.close();
        }
        return okText("Successfully modified Student");
    }

    // Backward compatibility with old endpoints
    @PostMapping(value = "/addStudent", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addStudentLegacy(@RequestBody(required = false) String body) {
        return addStudent(body);
    }

    @GetMapping(value = "/allStudents", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> allStudentsLegacy() {
        return allStudents();
    }

    @DeleteMapping(value = "/deleteStudent", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteStudentLegacy(@RequestBody(required = false) String body) {
        return deleteStudent(body);
    }

    @PostMapping(value = "/modifyStudent", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> modifyStudentLegacy(@RequestBody(required = false) String body) {
        return modifyStudent(body);
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
