package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Student;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import org.hibernate.Session;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.util.stream.Collectors;

public class AddStudentHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            respond(exchange, "Not a POST request", 400);
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

        } catch (JSONException e) {
            respond(exchange, "The following sting isn't a json object!\n" + builder, 400);
            return;
        }
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
                        System.out.println(x.getClass().getName());
                        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
                        Kurs k = session.get(Kurs.class, Long.valueOf(((String) x)));
                        System.out.println(k.toString());
                        session.close();
                        return k;
                    }).collect(Collectors.toList())
            );
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                session.persist(wohnort);
                session.persist(student);
                session.flush();
            });
            System.out.println(student);
            respond(exchange, "Successfully added student", 200);
        } catch (Exception e) {
            respond(exchange, "Can't parse JSON object!\n" + e.getMessage(), 400);
            e.printStackTrace();
        }
    }
}
