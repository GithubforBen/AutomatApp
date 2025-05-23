package de.schnorrenbergers.automat.server.handler.modify;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Student;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.server.handler.CustomHandler;
import org.hibernate.Session;
import org.json.JSONObject;

import java.io.IOException;

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
