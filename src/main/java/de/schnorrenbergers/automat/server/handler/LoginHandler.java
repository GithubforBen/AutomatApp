package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.database.types.User;
import de.schnorrenbergers.automat.manager.KontenManager;
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
            int[] rfids = json.getJSONArray("rfid").toList().stream().map(Object::toString).mapToInt(Integer::parseInt).toArray();
            boolean rfid = new LoginManager().login(rfids);
            KontenManager kontenManager = new KontenManager(rfids);
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("cameIn", rfid);
            jsonResponse.put("time", kontenManager.getKonto().getBalance());
            User user = kontenManager.getKonto().getUser();
            jsonResponse.put("name", user.getFullName());
            respond(exchange, jsonResponse.toString());
        } catch (Exception e) {
            respond(exchange, "There is no user associated with this rfid card. Please register one or start crying.");
        }
    }
}
