package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Student;
import org.hibernate.Session;
import org.json.JSONObject;

import java.io.IOException;

public class DeleteStudentHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
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
        Student teacher = session.get(Student.class, id);
        if (teacher == null) {
            badRequest(exchange);
        }
        session.close();
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((session1 -> {
            session1.remove(session1.get(Student.class, id));
        }));
        respond(exchange, "Successfully deleted Student");
    }
}
