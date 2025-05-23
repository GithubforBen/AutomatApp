package de.schnorrenbergers.automat.server;

import com.sun.net.httpserver.HttpServer;
import de.schnorrenbergers.automat.server.handler.*;
import de.schnorrenbergers.automat.server.handler.add.AddCourseHandler;
import de.schnorrenbergers.automat.server.handler.add.AddStudentHandler;
import de.schnorrenbergers.automat.server.handler.add.AddTeatcherHandler;
import de.schnorrenbergers.automat.server.handler.delete.DeleteCourseHandler;
import de.schnorrenbergers.automat.server.handler.delete.DeleteStudentHandler;
import de.schnorrenbergers.automat.server.handler.delete.DeleteTeacherHandler;
import de.schnorrenbergers.automat.server.handler.modify.ModifyCourseHandler;
import de.schnorrenbergers.automat.server.handler.modify.ModifyStudentHandler;
import de.schnorrenbergers.automat.server.handler.modify.ModifyTeatcherHandler;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    /**
     * Initializes and starts an HTTP server with multiple endpoint handlers.
     * This constructor sets up server routes and assigns appropriate request handlers
     * for different API functionalities. The server operates on port 8000 with a thread
     * pool to manage concurrent requests efficiently.
     *
     * <p>The server handles the following endpoints:
     * <ul>
     *   <li><b>/scanned</b>: Handles requests related to RFID scanning using {@link ScannedHandler}.</li>
     *   <li><b>/energetics</b>: Handles energetics-related requests using {@link SchonenderHandler}.</li>
     *   <li><b>/</b>: Serves the primary landing page using {@link IndexHandler}.</li>
     *   <li><b>/addTeacher</b>: Handles adding teacher information using {@link AddTeatcherHandler}.</li>
     *   <li><b>/addCourse</b>: Handles adding course information using {@link AddCourseHandler}.</li>
     *   <li><b>/addStudent</b>: Handles adding student information using {@link AddStudentHandler}.</li>
     *   <li><b>/genders</b>: Provides gender-related data using {@link GenderHandler}.</li>
     *   <li><b>/days</b>: Provides day-related data using {@link DayHandler}.</li>
     *   <li><b>/allStudents</b>: Retrieves a list of all students using {@link GetAllStudentsHandler}.</li>
     *   <li><b>/allTeachers</b>: Retrieves a list of all teachers using {@link GetAllTeatchersHandler}.</li>
     *   <li><b>/login</b>: Handles user login requests using {@link LoginHandler}.</li>
     *   <li><b>/allCourses</b>: Retrieves a list of all courses using {@link GetAllCoursesHandler}.</li>
     *   <li><b>/deleteStudent</b>: Handles student deletion requests using {@link DeleteStudentHandler}.</li>
     *   <li><b>/deleteTeacher</b>: Handles teacher deletion requests using {@link DeleteTeacherHandler}.</li>
     *   <li><b>/deleteCourse</b>: Handles course deletion requests using {@link DeleteCourseHandler}.</li>
     *   <li><b>/modifyStudent</b>: Handles updating student information using {@link ModifyStudentHandler}.</li>
     *   <li><b>/modifyCourse</b>: Handles updating course information using {@link ModifyCourseHandler}.</li>
     *   <li><b>/modifyTeacher</b>: Handles updating teacher information using {@link ModifyTeatcherHandler}.</li>
     * </ul>
     *
     * <p>A {@link ThreadPoolExecutor} with a fixed thread pool of size 10 is utilized to
     * ensure effective handling of multiple requests simultaneously. The server logs
     * information about its startup status and accessible URL.
     *
     * @throws IOException If an I/O error occurs during the server setup or operation,
     *                     such as issues with binding the server to the specified port.
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
        server.createContext("/deleteStudent", new DeleteStudentHandler());
        server.createContext("/deleteTeacher", new DeleteTeacherHandler());
        server.createContext("/deleteCourse", new DeleteCourseHandler());
        server.createContext("/modifyStudent", new ModifyStudentHandler());
        server.createContext("/modifyCourse", new ModifyCourseHandler());
        server.createContext("/modifyTeacher", new ModifyTeatcherHandler());
        server.setExecutor(threadPoolExecutor);
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort() + " ip" + Inet4Address.getLocalHost().getHostAddress());
        System.out.println("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + server.getAddress().getPort());
    }
}
