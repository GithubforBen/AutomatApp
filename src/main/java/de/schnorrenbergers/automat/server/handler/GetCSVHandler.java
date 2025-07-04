package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Statistic;
import de.schnorrenbergers.automat.database.types.Student;
import org.hibernate.Session;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetCSVHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            methodNotAllowed(exchange);
            return;
        }
        JSONObject json = getJSON(exchange);
        long kurs = json.getLong("kurs");
        List<String[]> dataLines = new ArrayList<>();
        Set<String> header = new HashSet<>();
        Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
        List<Student> students = session.createSelectionQuery("from Student u join fetch u.kurse k where k.id = :kurs or :kurs = -1", Student.class).
                setParameter("kurs", kurs).getResultList();
        students.stream().map((x) -> session.createSelectionQuery("from Statistic stat where stat.type = 'STUDENT_ATTEND_STATIC' and stat.data = :data", Statistic.class)
                .setParameter("data", String.valueOf(x.getId())).getResultList().stream().map((y) -> {
                    Date date = new Date(y.getTimestamp());
                    return date.getDay() + "/" + date.getMonth() + "/" + date.getYear();
                }).toList()).forEach(header::addAll);
        List<String> strings = header.stream().sorted().toList();
        List<String> finalStrings = strings;
        students.forEach(student -> {
            String[] line = new String[finalStrings.size() + 2];
            line[0] = student.getFirstName();
            line[1] = student.getLastName();
            List<String> studentAttendanceDates = session.createSelectionQuery("from Statistic stat where stat.type = 'STUDENT_ATTEND_STATIC' and stat.data = :data", Statistic.class)
                    .setParameter("data", String.valueOf(student.getId())).getResultList().stream().map((y) -> {
                        Date date = new Date(y.getTimestamp());
                        return date.getDay() + "/" + date.getMonth() + "/" + date.getYear();
                    }).toList();
            for (int i = 2; i < finalStrings.size() + 2; i++) {
                String s = finalStrings.get(i - 2);
                if (studentAttendanceDates.contains(s)) {
                    line[i] = "Anwesend";
                } else {
                    line[i] = "Nicht anwesend";
                }
            }
            dataLines.add(line);
        });
        strings = addFirst(strings, "Nachname");
        strings = addFirst(strings, "Vorname");
        dataLines.addFirst(strings.toArray(new String[0]));
        StringBuilder sb = new StringBuilder();
        dataLines.stream().map(this::convertToCSV).map((x) -> x + "\n").forEach(sb::append);
        session.close();
        respond(exchange, sb.toString());
    }

    private List<String> addFirst(List<String> list, String s) {
        List<String> newList = new ArrayList<>();
        newList.add(s);
        newList.addAll(list);
        return newList;
    }

    public String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(";"));
    }

    public String escapeSpecialCharacters(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }
        String escapedData = data.replaceAll("\\R", " ");
        if (escapedData.contains(",") || escapedData.contains("\"") || escapedData.contains("'")) {
            escapedData = escapedData.replace("\"", "\"\"");
            escapedData = "\"" + escapedData + "\"";
        }
        return escapedData;
    }
}
