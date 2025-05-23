package de.schnorrenbergers.automat.server.handler.modify;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Level;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import de.schnorrenbergers.automat.server.handler.CustomHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;

public class ModifyTeatcherHandler extends CustomHandler implements HttpHandler {
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
            //String firstName, String lastName, int[] rfid, Gender gender, Date age, Wohnort wohnort, Kurs[] kurse, String email, String password
            Teacher teacher = new Teacher(
                    jsonObject.getString("firstName"),
                    jsonObject.getString("lastName"),
                    jsonObject.getJSONArray("rfid").toList().stream().mapToInt((x) -> {
                        return Integer.parseInt(String.valueOf(x));
                    }).toArray(),
                    Gender.valueOf(jsonObject.getString("gender")),
                    new Date(jsonObject.getLong("birthday")),
                    wohnort,
                    jsonObject.getString("email"),
                    jsonObject.getString("password"),
                    Level.valueOf(jsonObject.getString("level"))
            );
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                session.persist(wohnort);
                session.persist(teacher);
                session.flush();
            });
            respond(exchange, "Successfully added teacher");
        } catch (Exception e) {
            jsonError(exchange);
            e.printStackTrace();
        }
     }
}
