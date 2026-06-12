package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.controller.MainController;
import de.schnorrenbergers.automat.manager.AvailabilityManager;
import de.schnorrenbergers.automat.manager.DispensationManager;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dispense")
public class DispenseController {

    /**
     * Called by the dispenser (ESP32) when a dispense did not produce a stop signal,
     * i.e. the sweet was not actually dispensed. Deactivates the corresponding Sweet
     * (independently of its stock amount) so it is greyed out until reactivated via
     * "Reaktivieren" in the admin UI.
     */
    @PostMapping(value = "/failed", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> failed(@RequestBody(required = false) String body) {
        JSONObject json = parseJson(body);
        if (json == null || !json.has("nr")) {
            return jsonError();
        }
        int type = DispensationManager.unmap(json.getInt("nr"));
        if (type < 0) {
            return badRequest();
        }
        new AvailabilityManager().disableSweet(type);
        if (MainController.getMainController() != null) {
            try {
                Main.getInstance().kost();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("success");
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

    private ResponseEntity<String> badRequest() {
        return ResponseEntity.status(400).contentType(MediaType.TEXT_PLAIN).body("BAD REQUEST: 400");
    }

    private ResponseEntity<String> jsonError() {
        return ResponseEntity.status(410).contentType(MediaType.TEXT_PLAIN).body("Can't parse JSON");
    }
}
