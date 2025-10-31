package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.manager.KontenManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class UserAttandanceHandler extends CustomHandler implements HttpHandler {
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
        KontenManager kontenManager = new KontenManager(id);
        List<Long> attendances = kontenManager.getKonto().getAttendances();
        JSONObject response = new JSONObject();
        response.put("attendances", attendances);
        respond(exchange, response.toString());
    }
}
