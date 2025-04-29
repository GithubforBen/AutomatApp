package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;

import java.io.IOException;

public class SchonenderHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        respond(exchange, "200", 200);
        Main.getInstance().getScreenSaver().setLastMove(0);
    }
}
