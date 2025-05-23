package de.schnorrenbergers.automat.server.handler.add;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.types.Day;
import de.schnorrenbergers.automat.server.handler.CustomHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AddCourseHandler extends CustomHandler implements HttpHandler {

    /**
     * Handles HTTP requests for adding a course into the system.
     * This method parses the request body to retrieve course details, including the course name,
     * the tutors (teachers) associated with the course, and the day of the course. It persists the
     * course to the database, ensuring that the course does not already exist.
     * <p>
     * If the request method is not POST or the request body is invalid, it responds with the appropriate
     * HTTP error codes and messages.
     *
     * @param exchange the HttpExchange object representing the HTTP request and response
     * @throws IOException if an I/O error occurs during the handling of the request
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            methodNotAllowed(exchange);
            return;
        }
        BufferedReader requestBodyReaderBuffer = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder builder = new StringBuilder();
        while (requestBodyReaderBuffer.ready()) {
            builder.append(requestBodyReaderBuffer.readLine());
        }
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(builder.toString());
            List<Teacher> teachers = new ArrayList<>();
            jsonObject.getJSONArray("tutor").forEach(teacher -> {
                Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                    Teacher fin = session.get(Teacher.class, Long.valueOf((String) teacher));
                    teachers.add(fin);
                });
            });
            Kurs kurs = new Kurs(jsonObject.getString("name"), teachers, Day.valueOf(jsonObject.getString("day")));

            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                session.persist(kurs);
                session.flush();
            });
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                List<Kurs> resultList = session.createSelectionQuery("from Kurs k where k.name = :name AND k.day = :day", Kurs.class)
                        .setParameter("name", kurs.getName())
                        .setParameter("day", kurs.getDay()).getResultList();

                if (resultList.size() != 1) throw new RuntimeException("The Course already exists!");
                try {
                    System.out.println("Created course: " + resultList.getFirst().toString());
                    respond(exchange, "Successfully added course: " + resultList.getFirst().toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (JSONException e) {
            jsonError(exchange);
        }
    }
}
