package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.manager.AlarmManager;
import de.schnorrenbergers.automat.manager.DispensationManager;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Meldungen der Ausgabe (ESP32). Alle bekommen {@code {"nr": <Fach der Ausgabe>}}.
 */
@RestController
@RequestMapping("/dispense")
public class DispenseController {

    /**
     * Die Lichtschranke hat die Süßigkeit gesehen - die Ausgabe hat geklappt.
     * Bleibt diese Meldung aus, wird das Fach gesperrt (siehe {@link DispensationManager}).
     */
    @PostMapping(value = "/success", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> success(@RequestBody(required = false) String body) {
        Integer type = readType(body);
        if (type == null) return badRequest();
        DispensationManager.succeeded(type);
        return ok();
    }

    /**
     * Called by the dispenser (ESP32) when a dispense did not produce a stop signal,
     * i.e. the sweet was not actually dispensed. Deactivates the corresponding Sweet
     * (independently of its stock amount) if "Fächer automatisch sperren" is on, so it
     * is greyed out until reactivated via "Reaktivieren" in the admin UI.
     */
    @PostMapping(value = "/failed", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> failed(@RequestBody(required = false) String body) {
        Integer type = readType(body);
        if (type == null) return badRequest();
        DispensationManager.reportedFailed(type);
        return ok();
    }

    /**
     * Eine Lichtschranke hat angesprochen, obwohl nichts ausgegeben wurde - jemand
     * greift in den Automaten. Löst den Alarm aus.
     */
    @PostMapping(value = "/unexpected", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> unexpected(@RequestBody(required = false) String body) {
        Integer type = readType(body);
        if (type == null) return badRequest();
        AlarmManager.trigger("Lichtschranke von Fach " + type + " ohne Ausgabe ausgelöst");
        return ok();
    }

    /**
     * @return der Süßigkeiten-Typ zum gemeldeten Fach, oder {@code null} bei ungültiger Meldung
     */
    private Integer readType(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JSONObject json = new JSONObject(body);
            if (!json.has("nr")) return null;
            int type = DispensationManager.unmap(json.getInt("nr"));
            return type < 0 ? null : type;
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<String> ok() {
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("success");
    }

    private ResponseEntity<String> badRequest() {
        return ResponseEntity.status(400).contentType(MediaType.TEXT_PLAIN).body("BAD REQUEST: 400");
    }
}
