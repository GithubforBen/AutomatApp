package de.schnorrenbergers.automat.server.handler.delete;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.server.handler.CustomHandler;
import org.hibernate.Session;
import org.json.JSONObject;

import java.io.IOException;

@Deprecated(forRemoval = true)
public class DeleteCourseHandler extends CustomHandler implements HttpHandler {

    /**
     * Handles an HTTP DELETE request to delete a "Kurs" resource identified by its ID.
     * <p>
     * This method verifies that the request is a DELETE method, parses the JSON body
     * to extract the ID, and deletes the corresponding "Kurs" resource from the database.
     * If any errors are encountered during the process, appropriate error responses are sent.
     *
     * @param exchange the {@link HttpExchange} object representing the HTTP request and response.
     *                 This contains information such as the request method, headers, and body.
     * @throws IOException if an I/O error occurs while handling the request.
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
        Kurs teacher = session.get(Kurs.class, id);
        if (teacher == null) {
            badRequest(exchange);
        }
        session.close();
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((session1 -> {
            session1.remove(session1.get(Kurs.class, id));
        }));
        respond(exchange, "Successfully deleted Kurs");
    }
}
