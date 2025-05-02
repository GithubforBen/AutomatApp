package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CustomHandler {
    // Standard HTTP status codes
    protected static final int OK = 200;
    protected static final int BAD_REQUEST = 400;
    protected static final int NOT_FOUND = 404;
    protected static final int METHOD_NOT_ALLOWED = 405;
    protected static final int CONFLICT = 409;
    protected static final int JSON_ERROR = 410;

    protected void respond(HttpExchange exchange, String answer) throws IOException {
        respond(exchange, answer, OK);
    }

    protected void respondJson(HttpExchange exchange, JSONObject json, int code) throws IOException {
        respond(exchange, json.toString());
    }

    private void respond(HttpExchange exchange, String answer, int code) throws IOException {
        exchange.sendResponseHeaders(code, answer.getBytes().length);
        exchange.getResponseBody().write(answer.getBytes());
        exchange.getResponseBody().close();
    }

    protected JSONObject getJSON(HttpExchange exchange) throws IOException {
        try {
            return new JSONObject(new String(exchange.getRequestBody().readAllBytes()));
        } catch (JSONException e) {
            respond(exchange, "Invalid JSON format", BAD_REQUEST);
            throw e;
        }
    }

    protected void methodNotAllowed(HttpExchange exchange) throws IOException {
        respond(exchange, "Method not allowed", METHOD_NOT_ALLOWED);
    }

    protected void badRequest(HttpExchange exchange, String message) throws IOException {
        respond(exchange, message, BAD_REQUEST);
    }

    protected void notFound(HttpExchange exchange, String message) throws IOException {
        respond(exchange, message, NOT_FOUND);
    }

    protected void conflict(HttpExchange exchange, String message) throws IOException {
        respond(exchange, message, CONFLICT);
    }

    protected void jsonError(HttpExchange exchange) throws IOException {
        respond(exchange, "Can't parse JSON", JSON_ERROR);
    }
}