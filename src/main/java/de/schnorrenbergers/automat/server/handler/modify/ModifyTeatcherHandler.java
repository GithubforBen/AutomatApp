package de.schnorrenbergers.automat.server.handler.modify;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.server.handler.CustomHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
            //String firstName, String lastName, int[] rfid, Gender gender, Date age, Wohnort wohnort, Kurs[] kurse, String email, String password
            Teacher teacher = Teacher.fromJSON(jsonObject);
            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                Teacher teacher1 = session.get(Teacher.class, teacher.getId());
                if (teacher1 == null) {
                    session.persist(teacher.getWohnort());
                    session.persist(teacher);
                    session.flush();
                    return;
                }
                if (teacher.equals(teacher1)) {
                    return;
                }
                session.merge(teacher.getWohnort());
                session.merge(teacher);
                session.flush();
            });
            respond(exchange, "Successfully added teacher");
        } catch (Exception e) {
            jsonError(exchange);
            e.printStackTrace();
        }
     }
}
