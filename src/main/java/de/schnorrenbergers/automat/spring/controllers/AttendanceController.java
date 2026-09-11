package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.database.types.types.Attandance;
import de.schnorrenbergers.automat.manager.KontenManager;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendances")
public class AttendanceController {

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> attendances(@RequestBody(required = false) String body) {
        JSONObject json = parseJson(body);
        if (json == null) {
            return jsonError();
        }
        long id = json.getLong("id");
        if (id == 0) {
            return badRequest();
        }
        KontenManager kontenManager = new KontenManager(id);
        List<Attandance> attendances = kontenManager.getKonto().getAttendances();
        JSONObject response = new JSONObject();
        response.put("attendances", attendances.stream().map((a) -> new JSONObject()
                .put("id", a.getId())
                .put("day", a.getDay())
                .put("month", a.getMonth())
                .put("year", a.getYear())
                .put("type", a.getType().toString())).toList());
        return okJson(response.toString());
    }

    /**
     * Setzt den Status eines Tages und ersetzt dabei alle vorhandenen Einträge
     * dieses Tages. Body: {@code {"id", "year", "month" (1-12), "day",
     * "type": "NORMAL" | "EXCUSED" | "AWAY" | "NONE"}} - NONE entfernt den Eintrag.
     */
    @PostMapping(value = "/set", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> set(@RequestBody(required = false) String body) {
        JSONObject json = parseJson(body);
        if (json == null) {
            return jsonError();
        }
        try {
            long id = json.getLong("id");
            if (id == 0) {
                return badRequest();
            }
            LocalDate date = LocalDate.of(json.getInt("year"), json.getInt("month"), json.getInt("day"));
            String type = json.getString("type");
            new KontenManager(id).setAttendance(date, "NONE".equals(type) ? null : Attandance.Type.valueOf(type));
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("Successfully set attendance");
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest();
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

    private ResponseEntity<String> okJson(String body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private ResponseEntity<String> badRequest() {
        return ResponseEntity.status(400).contentType(MediaType.TEXT_PLAIN).body("BAD REQUEST: 400");
    }

    private ResponseEntity<String> jsonError() {
        return ResponseEntity.status(410).contentType(MediaType.TEXT_PLAIN).body("Can't parse JSON");
    }
}
