package de.schnorrenbergers.automat.spring.mappings;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.*;
import de.schnorrenbergers.automat.database.types.types.*;
import de.schnorrenbergers.automat.manager.AddUserHandler;
import de.schnorrenbergers.automat.manager.KontenManager;
import de.schnorrenbergers.automat.manager.LoginManager;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class LegacyApiController {
    private static final int BAD_REQUEST = 400;
    private static final int JSON_ERROR = 410;
    private static final int DATA_NOT_FOUND = 501;

    @RequestMapping(value = "/scanned", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<String> scanned(@RequestBody(required = false) String body) {
        JSONObject json = parseJson(body);
        if (json == null) {
            return jsonError();
        }
        Main.getInstance().setLastScan(
                json.getJSONArray("rfid").toList().stream().map(Object::toString).mapToInt(Integer::parseInt).toArray());
        return okText("success");
    }

    @RequestMapping(value = "/energetics", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> energetics() {
        Main.getInstance().getScreenSaver().setLastMove(0);
        return okText("200");
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> index() {
        ClassPathResource resource = new ClassPathResource("index.html");
        if (!resource.exists()) {
            return respond(HttpStatus.NOT_FOUND.value(), "index.html not found", MediaType.TEXT_PLAIN);
        }
        try {
            String body = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ok(body, MediaType.TEXT_HTML);
        } catch (IOException e) {
            return respond(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to load index.html", MediaType.TEXT_PLAIN);
        }
    }

    @PostMapping(value = "/addTeacher", produces = MediaType.TEXT_PLAIN_VALUE)
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

    @PostMapping(value = "/addCourse", produces = MediaType.TEXT_PLAIN_VALUE)
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

    @PostMapping(value = "/addStudent", produces = MediaType.TEXT_PLAIN_VALUE)
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

    @GetMapping(value = "/allStudents", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @GetMapping(value = "/allTeachers", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> login(@RequestBody(required = false) String body) {
        JSONObject json = parseJson(body);
        if (json == null) {
            return jsonError();
        }
        try {
            int[] rfids = json.getJSONArray("rfid").toList().stream().map(Object::toString).mapToInt(Integer::parseInt).toArray();
            boolean rfid = new LoginManager().login(rfids);
            KontenManager kontenManager = new KontenManager(rfids);
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("cameIn", rfid);
            jsonResponse.put("time", kontenManager.getKonto().getBalance());
            User user = kontenManager.getKonto().getUser();
            jsonResponse.put("name", user.getFullName());
            jsonResponse.put("text", getText(user, rfid));
            return okJson(jsonResponse.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return respond(DATA_NOT_FOUND, "There is no user associated with this rfid card. Please register one or start crying.", MediaType.TEXT_PLAIN);
        }
    }

    @GetMapping(value = "/allCourses", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @DeleteMapping(value = "/deleteStudent", produces = MediaType.TEXT_PLAIN_VALUE)
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

    @DeleteMapping(value = "/deleteTeacher", produces = MediaType.TEXT_PLAIN_VALUE)
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

    @DeleteMapping(value = "/deleteCourse", produces = MediaType.TEXT_PLAIN_VALUE)
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

    @PostMapping(value = "/modifyStudent", produces = MediaType.TEXT_PLAIN_VALUE)
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

    @PostMapping(value = "/modifyCourse", produces = MediaType.TEXT_PLAIN_VALUE)
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

    @PostMapping(value = "/modifyTeacher", produces = MediaType.TEXT_PLAIN_VALUE)
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

    @PostMapping(value = "/csv", produces = "text/csv")
    public ResponseEntity<String> csv(@RequestBody(required = false) String body) {
        JSONObject json = parseJson(body);
        if (json == null) {
            return jsonError();
        }
        long kurs = json.getLong("kurs");
        List<String[]> dataLines = new ArrayList<>();
        Set<String> header = new HashSet<>();
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        try {
            List<Student> students = session.createSelectionQuery("from Student u join fetch u.kurse k where k.id = :kurs or :kurs = -1", Student.class)
                    .setParameter("kurs", kurs).getResultList();
            students.stream().map((x) -> session.createSelectionQuery("from Statistic stat where stat.type = 'STUDENT_ATTEND_STATIC' and stat.data = :data", Statistic.class)
                    .setParameter("data", String.valueOf(x.getId())).getResultList().stream().map((y) -> {
                        java.util.Date date = new java.util.Date(y.getTimestamp());
                        return date.getDay() + "/" + date.getMonth() + "/" + date.getYear();
                    }).toList()).forEach(header::addAll);
            List<String> strings = header.stream().sorted().toList();
            List<String> finalStrings = strings;
            students.forEach(student -> {
                String[] line = new String[finalStrings.size() + 2];
                line[0] = student.getFirstName();
                line[1] = student.getLastName();
                List<String> studentAttendanceDates = session.createSelectionQuery("from Statistic stat where stat.type = 'STUDENT_ATTEND_STATIC' and stat.data = :data", Statistic.class)
                        .setParameter("data", String.valueOf(student.getId())).getResultList().stream().map((y) -> {
                            java.util.Date date = new java.util.Date(y.getTimestamp());
                            return date.getDay() + "/" + date.getMonth() + "/" + date.getYear();
                        }).toList();
                for (int i = 2; i < finalStrings.size() + 2; i++) {
                    String s = finalStrings.get(i - 2);
                    if (studentAttendanceDates.contains(s)) {
                        line[i] = "Anwesend";
                    } else {
                        line[i] = "Nicht anwesend";
                    }
                }
                dataLines.add(line);
            });
            strings = addFirst(strings, "Nachname");
            strings = addFirst(strings, "Vorname");
            dataLines.addFirst(strings.toArray(new String[0]));
            StringBuilder sb = new StringBuilder();
            dataLines.stream().map(this::convertToCSV).map((x) -> x + "\n").forEach(sb::append);
            return ok(sb.toString(), MediaType.parseMediaType("text/csv"));
        } finally {
            session.close();
        }
    }

    @PostMapping(value = "/attendances", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> attendances(@RequestBody(required = false) String body) {
        JSONObject json = parseJson(body);
        if (json == null) {
            return jsonError();
        }
        long id = json.getLong("id");
        if (id == 0) {
            return badRequest();
        }
        KontenManager kontenManager = new KontenManager(id);
        List<Attandance> attendances = kontenManager.getKonto().getAttendances();
        JSONObject response = new JSONObject();
        response.put("attendances", attendances.stream().map((a) -> new JSONObject().put("day", a.getDay()).put("month", a.getMonth()).put("year", a.getYear()).put("type", a.getType().toString())).toList());
        return okJson(response.toString());
    }

    private JSONArray getText(User user, boolean cameIn) {
        JSONArray array = new JSONArray();
        array.put(cameIn ? "Herzlich Willkommen," : "Auf Wiedersehen,");
        array.put(user.getFullName().substring(0, Math.min(user.getFullName().length(), 20)));
        array.put("im MINT-Zentrum!");
        array.put("Stunden: " + new KontenManager(user.getId()).getKonto().getBalanceRounded());
        return array;
    }

    private JSONObject parseJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return new JSONObject(body);
        } catch (JSONException e) {
            return null;
        }
    }

    private List<String> addFirst(List<String> list, String s) {
        List<String> newList = new ArrayList<>();
        newList.add(s);
        newList.addAll(list);
        return newList;
    }

    private String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(";"));
    }

    private String escapeSpecialCharacters(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }
        String escapedData = data.replaceAll("\\R", " ");
        if (escapedData.contains(",") || escapedData.contains("\"") || escapedData.contains("'")) {
            escapedData = escapedData.replace("\"", "\"\"");
            escapedData = "\"" + escapedData + "\"";
        }
        return escapedData;
    }

    private ResponseEntity<String> okText(String body) {
        return ok(body, MediaType.TEXT_PLAIN);
    }

    private ResponseEntity<String> okJson(String body) {
        return ok(body, MediaType.APPLICATION_JSON);
    }

    private ResponseEntity<String> ok(String body, MediaType mediaType) {
        return ResponseEntity.ok().contentType(mediaType).body(body);
    }

    private ResponseEntity<String> badRequest() {
        return respond(BAD_REQUEST, "BAD REQUEST: " + BAD_REQUEST, MediaType.TEXT_PLAIN);
    }

    private ResponseEntity<String> jsonError() {
        return respond(JSON_ERROR, "Can't parse JSON", MediaType.TEXT_PLAIN);
    }

    private ResponseEntity<String> respond(int code, String body, MediaType mediaType) {
        return ResponseEntity.status(code).contentType(mediaType).body(body);
    }
}
