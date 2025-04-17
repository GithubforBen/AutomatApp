package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.types.Day;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AddCourseHandler implements HttpHandler {
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
        } catch (JSONException e) {
            respond(exchange, "The following sting isn't a json object!\n" + builder.toString(), 400);
            return;
        }
    }

    private void respond(HttpExchange exchange, String answer, int code) throws IOException {
        exchange.sendResponseHeaders(code, answer.getBytes().length);
        exchange.getResponseBody().write(answer.getBytes());
        exchange.getResponseBody().close();
    }
}
