package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.types.Day;
import org.hibernate.Session;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/course")
public class CourseController {

    @PostMapping(value = "/add", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addCourse(@RequestBody(required = false) String body) {
        JSONObject jsonObject = parseJson(body);
        if (jsonObject == null) {
            return jsonError();
        }
        try {
            List<Teacher> teachers = new ArrayList<>();
            jsonObject.getJSONArray("tutor").forEach(teacher -> Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                Teacher fin = session.get(Teacher.class, Long.valueOf((String) teacher));
                teachers.add(fin);
            }));
            Kurs kurs = new Kurs(jsonObject.getString("name"), teachers, Day.valueOf(jsonObject.getString("day")));
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                session.persist(kurs);
                session.flush();
            });
            StringBuilder created = new StringBuilder();
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                List<Kurs> resultList = session.createSelectionQuery("from Kurs k where k.name = :name AND k.day = :day", Kurs.class)
                        .setParameter("name", kurs.getName())
                        .setParameter("day", kurs.getDay()).getResultList();

                if (resultList.size() != 1) throw new RuntimeException("The Course already exists!");
                System.out.println("Created course: " + resultList.getFirst());
                created.append(resultList.getFirst());
            });
            if (!created.isEmpty()) {
                return okText("Successfully added course: " + created);
            }
            return okText("Successfully added course");
        } catch (JSONException e) {
            return jsonError();
        }
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> allCourses() {
        List<Kurs> courses = new ArrayList<>();
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        try {
            courses.addAll(session.createSelectionQuery("from Kurs u", Kurs.class).getResultList());
            StringBuilder response = new StringBuilder();
            response.append("{ \"courses\": [");
            courses.forEach(kurs -> {
                response.append(kurs.toJSON().toString());
                response.append(",");
            });
            if (!courses.isEmpty()) response.replace(response.length() - 1, response.length(), "");
            response.append("] }");
            return okJson(response.toString());
        } finally {
            session.close();
        }
    }

    @DeleteMapping(value = "/delete", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteCourse(@RequestBody(required = false) String body) {
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
            Kurs kurs = session.get(Kurs.class, id);
            if (kurs == null) {
                return badRequest();
            }
        } finally {
            session.close();
        }
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((session1 -> session1.remove(session1.get(Kurs.class, id))));
        return okText("Successfully deleted Kurs");
    }

    @PostMapping(value = "/modify", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> modifyCourse(@RequestBody(required = false) String body) {
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
            Kurs kurs = session.get(Kurs.class, id);
            if (kurs == null) {
                return badRequest();
            }
            if (json.has("day")) kurs.setDay(Day.valueOf(json.getString("day")));
            if (json.has("name")) kurs.setName(json.getString("name"));
            if (json.has("addTeacher")) {
                Teacher teacher = session.get(Teacher.class, Long.valueOf(json.getString("addTeacher")));
                if (teacher == null) {
                    return badRequest();
                }
                kurs.getTutor().add(teacher);
            }
            if (json.has("removeTeacher")) {
                Teacher teacher = session.get(Teacher.class, Long.valueOf(json.getString("removeTeacher")));
                if (teacher == null) {
                    return badRequest();
                }
                kurs.getTutor().remove(teacher);
            }
            session.getTransaction().begin();
            session.merge(kurs);
            session.getTransaction().commit();
        } finally {
            session.close();
        }
        return okText("Successfully deleted Kurs");
    }

    // Backward compatibility with old endpoints
    @PostMapping(value = "/addCourse", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addCourseLegacy(@RequestBody(required = false) String body) {
        return addCourse(body);
    }

    @GetMapping(value = "/allCourses", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> allCoursesLegacy() {
        return allCourses();
    }

    @DeleteMapping(value = "/deleteCourse", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteCourseLegacy(@RequestBody(required = false) String body) {
        return deleteCourse(body);
    }

    @PostMapping(value = "/modifyCourse", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> modifyCourseLegacy(@RequestBody(required = false) String body) {
        return modifyCourse(body);
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
