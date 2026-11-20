package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.manager.AlarmManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Karten vom Scanner am Automaten. Der Scanner schickt sie signiert direkt hierher
 * (früher unsigniert über die Website, da ließ sich jede Karte vortäuschen).
 * <p>
 * "Karte scannen" auf der Website: {@code /scanned/capture/start} merkt vor, dass
 * die nächste Karte für die Website ist; {@code /scanned/capture/check} holt sie ab.
 */
@RestController
@RequestMapping("/scanned")
public class ScannedController {

    /** So lange wartet "Karte scannen" höchstens auf eine Karte. */
    private static final long CAPTURE_MS = 60_000;

    private static final Object captureLock = new Object();
    private static long captureUntil = 0;
    private static int[] captured;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<String> scanned(@RequestBody(required = false) String body) {
        if (body == null || body.isBlank()) {
            return ResponseEntity.status(410).contentType(MediaType.TEXT_PLAIN).body("Can't parse JSON");
        }
        try {
            JSONObject json = new JSONObject(body);
            int[] rfid = json.getJSONArray("rfid").toList().stream().map(Object::toString).mapToInt(Integer::parseInt).toArray();
            // Wartet die Website auf eine Karte, geht sie dorthin und nicht an den Automaten.
            synchronized (captureLock) {
                if (System.currentTimeMillis() < captureUntil && captured == null) {
                    captured = rfid;
                    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("success");
                }
            }
            // Lehrkräfte und Admins beenden mit ihrer Karte den Alarm.
            AlarmManager.disarmByCard(rfid);
            Main.getInstance().setLastScan(rfid);
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("success");
        } catch (Exception e) {
            return ResponseEntity.status(410).contentType(MediaType.TEXT_PLAIN).body("Can't parse JSON");
        }
    }

    /** Die nächste Karte am Scanner ist für die Website ("Karte scannen"). */
    @PostMapping(value = "/capture/start", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> startCapture() {
        synchronized (captureLock) {
            // Bewusst neu: eine liegengebliebene Karte von vorher zählt nicht.
            captured = null;
            captureUntil = System.currentTimeMillis() + CAPTURE_MS;
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("started");
    }

    /** {@code {"status":"waiting"}} oder {@code {"status":"done","rfid":[...]}}. */
    @PostMapping(value = "/capture/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> checkCapture() {
        JSONObject answer = new JSONObject();
        synchronized (captureLock) {
            if (captured != null) {
                answer.put("status", "done").put("rfid", new JSONArray(captured));
                captured = null;
                captureUntil = 0;
            } else {
                answer.put("status", "waiting");
            }
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(answer.toString());
    }
}
