package de.schnorrenbergers.automat.server.handler.modify;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Student;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import de.schnorrenbergers.automat.server.handler.CustomHandler;
import org.hibernate.Session;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

public class ModifyStudentHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            methodNotAllowed(exchange);
            return;
        }
        JSONObject json = getJSON(exchange);
        if (json == null) {
            jsonError(exchange);
            return;
        }
        long id = json.getLong("id");
        if (id == 0) {
            badRequest(exchange);
        }
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        Student student = session.get(Student.class, id);
        if (student == null) {
            badRequest(exchange);
        }
        if (json.has("gender")) student.setGender(Gender.valueOf(json.getString("gender")));
        if (json.has("firstName")) student.setFirstName(json.getString("firstName"));
        if (json.has("lastName")) student.setLastName(json.getString("lastName"));
        if (json.has("rfid"))
            student.setRfid(json.getJSONArray("rfid").toList().stream().mapToInt((x) -> (int) x).toArray());
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
        /*
        if (json.)

        if (json.has("addTeacher")) {
            Teacher teacher = session.get(Teacher.class, Long.valueOf(json.getString("addTeacher")));
            if (teacher == null) {
                badRequest(exchange);
                return;
            }
            student.getTutor().add(teacher);
        }
        if (json.has("removeTeacher")) {
            Teacher teacher = session.get(Teacher.class, Long.valueOf(json.getString("removeTeacher")));
            if (teacher == null) {
                badRequest(exchange);
                return;
            }
            student.getTutor().remove(teacher);
        }
        session.getTransaction().begin();
        session.merge(student);
        session.getTransaction().commit();
        session.close();
        respond(exchange, "Successfully deleted Kurs");

         */
    }
}
