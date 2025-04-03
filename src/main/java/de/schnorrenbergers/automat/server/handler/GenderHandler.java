package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.database.types.types.Gender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

//JOKE
public class GenderHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(Gender.values()).map(Enum::toString).forEach((x) -> {
            sb.append(x);
            sb.append('\n');
        });
        final String string = sb.toString();
        exchange.sendResponseHeaders(200, string.getBytes().length);
        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(string.getBytes(StandardCharsets.UTF_8));
        responseBody.flush();
        responseBody.close();
    }
}
