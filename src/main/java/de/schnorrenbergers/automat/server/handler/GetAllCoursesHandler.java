package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Kurs;
import org.hibernate.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetAllCoursesHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        List<Kurs> courses = new ArrayList<>();
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        courses.addAll(session.createSelectionQuery("from Kurs u", Kurs.class).getResultList());
        StringBuilder response = new StringBuilder();
        response.append("{ \"courses\": [");
        courses.forEach(kurs -> {
            response.append(kurs.toJSON().toString());
            response.append(",");
        });
        if (!courses.isEmpty()) response.replace(response.length() - 1, response.length(), "");
        response.append("] }");
        session.close();
        respond(exchange, response.toString());
    }
}
