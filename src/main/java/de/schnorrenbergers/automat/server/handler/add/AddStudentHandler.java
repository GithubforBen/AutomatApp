package de.schnorrenbergers.automat.server.handler.add;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Student;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import de.schnorrenbergers.automat.server.handler.CustomHandler;
import org.hibernate.Session;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Date;
import java.util.stream.Collectors;

public class AddStudentHandler extends CustomHandler implements HttpHandler {

    /**
     * Handles incoming HTTP exchange requests to add a new student to the system.
     * This method expects a POST request with a JSON body containing details of a student,
     * including their address, name, gender, birthday, RFID, and associated courses.
     * If the request method is not POST, responds with a "method not allowed" status.
     * If the request body is invalid or cannot be processed, responds with an error status.
     * <p>
     * The student and their associated address object are persisted in the system's database
     * within a transactional scope after parsing and validation.
     *
     * @param exchange the HTTP exchange object containing the input request, including
     *                 headers, method, and the request body to be processed.
     * @throws IOException if an input/output operation error occurs while processing
     *                     the request or sending a response.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            methodNotAllowed(exchange);
            return;
        }
        JSONObject jsonObject = getJSON(exchange);
        try {
            //int number, String street, String city, int zip, String country
            JSONObject address = jsonObject.getJSONObject("address");
            Wohnort wohnort = new Wohnort(
                    address.getInt("nr"),
                    address.getString("street"),
                    address.getString("city"),
                    address.getInt("zip"),
                    address.getString("country"));
            //String firstName, String lastName, int[] rfid, Gender gender, Date age, Wohnort wohnort, Kurs[] kurse
            Student student = new Student(
                    jsonObject.getString("firstName"),
                    jsonObject.getString("lastName"),
                    jsonObject.getJSONArray("rfid").toList().stream().mapToInt((x) -> {
                        return Integer.parseInt(String.valueOf(x));
                    }).toArray(),
                    Gender.valueOf(jsonObject.getString("gender")),
                    new Date(jsonObject.getLong("birthday")),
                    wohnort,
                    jsonObject.getJSONArray("kurse").toList().stream().map((x) -> {
                        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
                        Kurs k = session.get(Kurs.class, Long.valueOf(String.valueOf(x)));
                        session.close();
                        return k;
                    }).collect(Collectors.toList())
            );
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                session.persist(wohnort);
                session.persist(student);
                session.flush();
            });
            respond(exchange, "Successfully added student");
        } catch (Exception e) {
            jsonError(exchange);
            e.printStackTrace();
        }
    }
}
