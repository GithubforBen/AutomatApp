package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Deprecated(forRemoval = true)
public class IndexHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStreamReader is = new InputStreamReader(classloader.getResourceAsStream("index.html"));
        BufferedReader br = new BufferedReader(is);
        int b;
        StringBuilder buf = new StringBuilder();
        while ((b = br.read()) != -1) {
            buf.append((char) b);
        }
        exchange.getResponseHeaders().add("Content-Type", "text/html");
        respond(exchange, buf.toString());
    }
}
