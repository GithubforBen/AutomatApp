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

@Deprecated(forRemoval = true)
public class GetAllTeatchersHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        List<Teacher> teachers = new ArrayList<>();
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            // Kein automatischer Standard-Admin mehr (siehe TeacherController.createFirstAdmin).
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
