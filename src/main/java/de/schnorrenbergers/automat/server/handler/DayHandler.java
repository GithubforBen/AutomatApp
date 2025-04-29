package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.database.types.types.Day;

import java.io.IOException;
import java.util.Arrays;

public class DayHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(Day.values()).map(Enum::toString).forEach((x) -> {
            sb.append(x);
            sb.append('\n');
        });
        respond(exchange, sb.toString(), 200);
    }
}
