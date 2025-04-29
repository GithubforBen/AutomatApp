package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.IOException;

public class CustomHandler {

    public void respond(HttpExchange exchange, String answer, int code) throws IOException {
        exchange.sendResponseHeaders(code, answer.getBytes().length);
        exchange.getResponseBody().write(answer.getBytes());
        exchange.getResponseBody().close();
    }

    public JSONObject getJSON(HttpExchange exchange) throws IOException {
        return new JSONObject(new String(exchange.getRequestBody().readAllBytes()));
    }

}
