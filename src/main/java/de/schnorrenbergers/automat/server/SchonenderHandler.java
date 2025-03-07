package de.schnorrenbergers.automat.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;

import java.io.IOException;

public class SchonenderHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String s = "Ist deutschland ein Freies Land? Ich denke ja eigentlich schon. " +
                "Deshalb nehme ich Drogen." +
                "- David der Glänzende";
        exchange.sendResponseHeaders(200, s.length());
        exchange.getResponseBody().write(s.getBytes());
        exchange.getResponseBody().close();
        Main.getInstance().getScreenSaver().setLastMove(0);
    }
}
