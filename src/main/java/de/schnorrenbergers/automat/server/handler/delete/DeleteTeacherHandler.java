package de.schnorrenbergers.automat.server.handler.delete;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.server.handler.CustomHandler;
import org.hibernate.Session;
import org.json.JSONObject;

import java.io.IOException;

public class DeleteTeacherHandler extends CustomHandler implements HttpHandler {

    /**
     * Handles HTTP DELETE requests to delete a teacher entity from the system.
     * This method performs the following steps:
     * <p>
     * <ul>
     * <li>Validates that the HTTP request method is DELETE.</li>
     * <li>Parses the JSON body of the request to retrieve the teacher ID.</li>
     * <li>Validates the ID from the request.</li>
     * <li>Checks if the teacher with the specified ID exists in the database.</li>
     * <li>If the teacher exists, deletes the teacher record from the database.</li>
     * <li>Sends an appropriate response back to the client.</li>
     * </ul>
     * <p>
     * In case of errors such as invalid request methods, invalid or missing JSON,
     * invalid ID, or non-existent teacher records, appropriate error responses are returned.
     *
     * @param exchange the {@link HttpExchange} object that provides access to the request
     *                 and response information.
     * @throws IOException if an I/O error occurs during handling of the exchange.
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
        Teacher teacher = session.get(Teacher.class, id);
        if (teacher == null) {
            badRequest(exchange);
        }
        session.close();
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((session1 -> {
            session1.remove(session1.get(Teacher.class, id));
        }));
        respond(exchange, "Successfully deleted teacher");
    }
}
