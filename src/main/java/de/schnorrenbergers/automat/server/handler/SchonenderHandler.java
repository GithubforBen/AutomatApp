package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;

import java.io.IOException;
import java.io.OutputStream;

public class SchonenderHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String s = "200";
        exchange.sendResponseHeaders(200, s.length());
        OutputStream os = exchange.getResponseBody();
        os.write(s.getBytes());
        os.close();
        Main.getInstance().getScreenSaver().setLastMove(0);
    }
}
