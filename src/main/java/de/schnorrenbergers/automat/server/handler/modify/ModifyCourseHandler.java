package de.schnorrenbergers.automat.server.handler.modify;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.types.Day;
import de.schnorrenbergers.automat.server.handler.CustomHandler;
import org.hibernate.Session;
import org.json.JSONObject;

import java.io.IOException;

@Deprecated(forRemoval = true)
public class ModifyCourseHandler extends CustomHandler implements HttpHandler {

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
            return;
        }
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        Kurs kurs = session.get(Kurs.class, id);
        if (kurs == null) {
            session.close();
            badRequest(exchange);
            return;
        }
        if (json.has("day")) kurs.setDay(Day.valueOf(json.getString("day")));
        if (json.has("name")) kurs.setName(json.getString("name"));
        if (json.has("addTeacher")) {
            Teacher teacher = session.get(Teacher.class, Long.valueOf(json.getString("addTeacher")));
            if (teacher == null) {
                badRequest(exchange);
                session.close();
                return;
            }
            kurs.getTutor().add(teacher);
        }
        if (json.has("removeTeacher")) {
            Teacher teacher = session.get(Teacher.class, Long.valueOf(json.getString("removeTeacher")));
            if (teacher == null) {
                badRequest(exchange);
                session.close();
                return;
            }
            kurs.getTutor().remove(teacher);
        }
        session.getTransaction().begin();
        session.merge(kurs);
        session.getTransaction().commit();
        session.close();
        respond(exchange, "Successfully deleted Kurs");
    }
}
