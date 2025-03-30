package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;

public class IndexHandler implements HttpHandler {
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
        String string = buf.toString();
        exchange.sendResponseHeaders(200, string.length());
        exchange.getResponseBody().write(string.getBytes());
        exchange.getResponseBody().close();
    }
}
