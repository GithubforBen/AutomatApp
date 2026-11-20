package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.manager.AlarmManager;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scanned")
public class ScannedController {

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<String> scanned(@RequestBody(required = false) String body) {
        if (body == null || body.isBlank()) {
            return ResponseEntity.status(410).contentType(MediaType.TEXT_PLAIN).body("Can't parse JSON");
        }
        try {
            JSONObject json = new JSONObject(body);
            int[] rfid = json.getJSONArray("rfid").toList().stream().map(Object::toString).mapToInt(Integer::parseInt).toArray();
            // Lehrkräfte und Admins beenden mit ihrer Karte den Alarm.
            AlarmManager.disarmByCard(rfid);
            Main.getInstance().setLastScan(rfid);
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("success");
        } catch (Exception e) {
            return ResponseEntity.status(410).contentType(MediaType.TEXT_PLAIN).body("Can't parse JSON");
        }
    }
}
