package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.database.types.User;
import de.schnorrenbergers.automat.manager.KontenManager;
import de.schnorrenbergers.automat.manager.LoginManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class LoginController {

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> login(@RequestBody(required = false) String body) {
        JSONObject json = parseJson(body);
        if (json == null) {
            return jsonError();
        }
        try {
            int[] rfids = json.getJSONArray("rfid").toList().stream().map(Object::toString).mapToInt(Integer::parseInt).toArray();
            boolean rfid = new LoginManager().login(rfids);
            KontenManager kontenManager = new KontenManager(rfids);
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("cameIn", rfid);
            jsonResponse.put("time", kontenManager.getKonto().getBalance());
            User user = kontenManager.getKonto().getUser();
            jsonResponse.put("name", user.getFullName());
            jsonResponse.put("text", getText(user, rfid));
            return okJson(jsonResponse.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(501)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("There is no user associated with this rfid card. Please register one or start crying.");
        }
    }

    private JSONArray getText(User user, boolean cameIn) {
        JSONArray array = new JSONArray();
        array.put(cameIn ? "Herzlich Willkommen," : "Auf Wiedersehen,");
        array.put(user.getFullName().substring(0, Math.min(user.getFullName().length(), 20)));
        array.put("im MINT-Zentrum!");
        array.put("Stunden: " + new KontenManager(user.getId()).getKonto().getBalanceRounded());
        return array;
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

    private ResponseEntity<String> jsonError() {
        return ResponseEntity.status(410).contentType(MediaType.TEXT_PLAIN).body("Can't parse JSON");
    }
}
