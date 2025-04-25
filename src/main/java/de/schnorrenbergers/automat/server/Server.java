package de.schnorrenbergers.automat.server;

import com.sun.net.httpserver.HttpServer;
import de.schnorrenbergers.automat.server.handler.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    public Server() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.createContext("/scanned", new ScannedHandler());
        server.createContext("/energetics", new SchonenderHandler());
        server.createContext("/", new IndexHandler());
        server.createContext("/addTeacher", new AddTeatcherHandler());
        server.createContext("/addCourse", new AddCourseHandler());
        server.createContext("/addStudent", new AddStudentHandler());
        server.createContext("/genders", new GenderHandler());
        server.createContext("/days", new DayHandler());
        server.createContext("/allStudents", new GetAllStudentsHandler());
        server.createContext("/allTeachers", new GetAllTeatchersHandler());
        server.setExecutor(threadPoolExecutor);
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort() + " ip" + Inet4Address.getLocalHost().getHostAddress());
        System.out.println("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + server.getAddress().getPort());
    }
}
