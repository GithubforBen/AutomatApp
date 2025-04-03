package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;

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
        exchange.getResponseHeaders().add("Content-Type", "text/html");
        final String string = buf.toString();
        exchange.sendResponseHeaders(200, string.getBytes().length);
        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(string.getBytes(StandardCharsets.UTF_8));
        responseBody.flush();
        responseBody.close();
    }
}
