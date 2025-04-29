package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.manager.LoginManager;
import org.json.JSONObject;

import java.io.IOException;

public class LoginHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            respond(exchange, "Method not allowed", 405);
        }
        JSONObject json = getJSON(exchange);
        if (new LoginManager().login(json.getJSONArray("rfid").toList().stream().mapToInt((o) -> Integer.parseInt(String.valueOf(o))).toArray())) {
            respond(exchange, "User came in", 200);
        } else {
            respond(exchange, "User left", 200);
        }
    }
}
