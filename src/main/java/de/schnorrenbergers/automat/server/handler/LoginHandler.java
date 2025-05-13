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
            methodNotAllowed(exchange);
            return;
        }
        JSONObject json = getJSON(exchange);
        try {
            if (new LoginManager().login(json.getJSONArray("rfid").toList().stream().mapToInt((o) -> Integer.parseInt(String.valueOf(o))).toArray())) {
                respond(exchange, "User came in");
            } else {
                respond(exchange, "User left");
            }
        } catch (Exception e) {
            badRequest(exchange);
            e.printStackTrace();
        }

    }
}
