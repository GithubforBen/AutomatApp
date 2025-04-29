package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Student;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetAllStudentsHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        List<Student> users = new ArrayList<>();
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            users.addAll(session.createSelectionQuery("from Student u", Student.class).getResultList());
        });
        StringBuilder response = new StringBuilder();
        response.append("{ \"users\": [");
        users.forEach(user -> {
            response.append(user.toJSONString());
            response.append(",");
        });
        response.replace(response.length() - 1, response.length(), "");
        response.append("] }");
        if (users.isEmpty()) {
            response.replace(0, response.length(), "");
            response.append("No Students found");
        }
        respond(exchange, response.toString(), 200);
    }
}
