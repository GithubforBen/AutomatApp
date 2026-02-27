package de.schnorrenbergers.automat.server.handler.delete;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Student;
import de.schnorrenbergers.automat.server.handler.CustomHandler;
import org.hibernate.Session;
import org.json.JSONObject;

import java.io.IOException;

@Deprecated(forRemoval = true)
public class DeleteStudentHandler extends CustomHandler implements HttpHandler {

    /**
     * Handles HTTP DELETE requests to delete a specific Student resource based on the provided JSON input.
     *
     * <p>
     * The method expects a JSON payload containing the ID of the Student to be deleted.
     * If the request method is not DELETE, it responds with a "Method not allowed" message.
     * If the JSON payload cannot be parsed or is invalid, it responds with appropriate errors.
     * If the specified student does not exist, it returns a "Bad Request" response.
     * Otherwise, it deletes the Student record from the database and responds with a success message.
     *
     * @param exchange the {@link HttpExchange} object that contains the HTTP request and response details.
     * @throws IOException if an I/O error occurs during the handling process.
     */
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
