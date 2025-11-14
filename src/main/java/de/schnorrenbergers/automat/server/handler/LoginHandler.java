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
            long userID = json.getLong("id");
            long time = json.getLong("time");
            new LoginManager().login(userID, time);
            new LoginManager().login(userID, time + 1000);
            respond(exchange, "GOOD BOY");
        } catch (Exception e) {
            badRequest(exchange);
            e.printStackTrace();
        }

    }
}
