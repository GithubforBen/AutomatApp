package de.schnorrenbergers.automat.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Server {
    public Server() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8000),0);
        server.createContext("/scanned", new ScannedHandler());
        server.start();
    }
}
