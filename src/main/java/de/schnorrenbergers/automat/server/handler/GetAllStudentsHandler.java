package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Student;
import org.hibernate.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetAllStudentsHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        List<Student> users = new ArrayList<>();
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        users.addAll(session.createSelectionQuery("from Student u", Student.class).getResultList());
        StringBuilder response = new StringBuilder();
        response.append("{ \"students\": [");
        users.forEach(user -> {
            response.append(user.toJSON().toString());
            response.append(",");
        });
        if (!users.isEmpty()) response.replace(response.length() - 1, response.length(), "");
        response.append("] }");
        session.close();
        respond(exchange, response.toString());
    }
}
