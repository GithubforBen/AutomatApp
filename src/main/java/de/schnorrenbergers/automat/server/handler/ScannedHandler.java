package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import org.json.JSONObject;

import java.io.IOException;

public class ScannedHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        JSONObject json = getJSON(exchange);
        Main.getInstance().setLastScan(
                json.getJSONArray("rfid").toList().stream().map(Object::toString).mapToInt(Integer::parseInt).toArray());
        respond(exchange, "success");
    }
}
