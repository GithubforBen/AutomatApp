package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
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
            Main.getInstance().setLastScan(
                    json.getJSONArray("rfid").toList().stream().map(Object::toString).mapToInt(Integer::parseInt).toArray());
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("success");
        } catch (Exception e) {
            return ResponseEntity.status(410).contentType(MediaType.TEXT_PLAIN).body("Can't parse JSON");
        }
    }
}
