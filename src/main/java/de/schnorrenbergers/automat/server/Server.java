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
    /**
     * Initializes and starts an HTTP server with different endpoint handlers for various functionalities.
     * <p>
     * The server operates on port 8000 and supports multiple contexts for handling specific routes.
     * Each handler is responsible for processing and responding to client requests to its corresponding endpoint.
     *
     * <pre>
     * The following contexts are handled:
     * - "/scanned": Handles scanned Cards
     * - "/energetics": Enables Screensaver.
     * - "/": Serves the index page.
     * - "/addTeacher": Handles requests for adding a new teacher.
     * - "/addCourse": Handles requests for registering a new course.
     * - "/addStudent": Handles adding a new student.
     * - "/genders": Provides list with all system supported genders.
     * - "/days": Provides information related to days.
     * - "/allStudents": Fetches a list of all students.
     * - "/allTeachers": Fetches a list of all teachers.
     * - "/login": Handles user login requests. When a user comes to th MINT-Zentrum.
     * </pre>
     * The server uses a fixed thread pool executor with a pool size of 10 for handling client requests.
     *
     * @throws IOException if an I/O error occurs during server setup or startup.
     */
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
        server.createContext("/login", new LoginHandler());
        server.createContext("/allCourses", new GetAllCoursesHandler());
        server.setExecutor(threadPoolExecutor);
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort() + " ip" + Inet4Address.getLocalHost().getHostAddress());
        System.out.println("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + server.getAddress().getPort());
    }
}
