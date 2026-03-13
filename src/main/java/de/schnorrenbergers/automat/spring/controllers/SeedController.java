package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Konto;
import de.schnorrenbergers.automat.database.types.Student;
import de.schnorrenbergers.automat.database.types.types.Attandance;
import de.schnorrenbergers.automat.manager.AddUserHandler;
import de.schnorrenbergers.automat.manager.KontenManager;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seed")
public class SeedController {

    @PostMapping(value = "/flush", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> flush(@RequestBody(required = false) String body) {
        JSONObject jsonObject = parseJson(body);
        if (jsonObject == null) {
            return jsonError();
        }
        try {
            int[] lastScan = jsonObject.getJSONArray("rfid").toList().stream().mapToInt((x) -> Integer.parseInt(String.valueOf(x))).toArray();
            AddUserHandler.UserAdd peek = AddUserHandler.addQueue.poll();
            if (peek == null) {
                return badRequest("Queue is empty");
            }

            Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
                session.persist(peek.getWohnort());
                session.persist(new Student(
                        peek.getVorname(),
                        peek.getNachname(),
                        lastScan,
                        peek.getGender(),
                        peek.getDate(),
                        peek.getWohnort(),
                        peek.getKurse()
                ));
                session.flush();
            });
            return okText("Successfully flushed student");
        } catch (Exception e) {
            e.printStackTrace();
            return jsonError();
        }
    }

    @PostMapping(value = "/attendance", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> attendance(@RequestBody(required = false) String body) {
        JSONObject jsonObject = parseJson(body);
        if (jsonObject == null) {
            return jsonError();
        }
        try {
            long id = jsonObject.getLong("id");
            if (id == 0) {
                return badRequest();
            }

            Attandance attendance = new Attandance(
                    jsonObject.getInt("day"),
                    jsonObject.getInt("month"),
                    jsonObject.getInt("year"),
                    jsonObject.getLong("login"),
                    Attandance.Type.valueOf(jsonObject.getString("type"))
            );
            attendance.logout(jsonObject.getLong("logout"));

            KontenManager kontenManager = new KontenManager(id);
            Konto konto = kontenManager.getKonto();
            konto.getAttendances().add(attendance);
            kontenManager.updateKonto(konto);
            return okText("Successfully added attendance");
        } catch (Exception e) {
            e.printStackTrace();
            return jsonError();
        }
    }

    private JSONObject parseJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return new JSONObject(body);
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<String> okText(String body) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body);
    }

    private ResponseEntity<String> badRequest() {
        return ResponseEntity.status(400).contentType(MediaType.TEXT_PLAIN).body("BAD REQUEST: 400");
    }

    private ResponseEntity<String> badRequest(String body) {
        return ResponseEntity.status(400).contentType(MediaType.TEXT_PLAIN).body(body);
    }

    private ResponseEntity<String> jsonError() {
        return ResponseEntity.status(410).contentType(MediaType.TEXT_PLAIN).body("Can't parse JSON");
    }
}
