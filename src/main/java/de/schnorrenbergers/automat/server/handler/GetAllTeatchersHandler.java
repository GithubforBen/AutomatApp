package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Level;
import de.schnorrenbergers.automat.database.types.types.Wohnort;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class GetAllTeatchersHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        List<Teacher> teachers = new ArrayList<>();
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            if (session.createQuery("from Teacher t", Teacher.class).getResultList().isEmpty()) {
                Wohnort wohnort = new Wohnort(7, "test", "test", 678, "Germany");
                Teacher teacher = null;
                try {
                    teacher = new Teacher("Jon", "Doe", new int[]{100, 100, 100, 100}, Gender.OTHER, new Date(1999, 02, 01), wohnort, "test@gmail.com", "test", Level.ADMIN);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                session.persist(wohnort);
                session.persist(teacher);
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
        respond(exchange, response.toString());
    }
}
