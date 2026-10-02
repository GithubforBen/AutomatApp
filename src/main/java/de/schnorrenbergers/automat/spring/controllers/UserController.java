package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.User;
import de.schnorrenbergers.automat.database.types.types.Level;
import de.schnorrenbergers.automat.manager.CipherManager;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * Ändert den Typ eines Zugangs: Schüler*in ({@code STUDENT}), Lehrkraft
     * ({@code TEACHER}) oder Admin ({@code ADMIN}).
     * <p>
     * Alle Nutzer liegen in einer Tabelle ("user"); die Spalte DTYPE sagt, ob
     * es eine Schüler*in oder eine Lehrkraft ist. Umgestellt wird deshalb direkt
     * per SQL - so behält die Person ihre ID und damit Zeitkonto, Anwesenheiten,
     * Chipkarte und Statistik. Kurse gehen dabei verloren, denn Schüler*innen
     * nehmen an Kursen teil und Lehrkräfte leiten sie: Wer wechselt, wird dort
     * ausgetragen.
     * <p>
     * Body: {@code {"id", "type", "email", "password"}} - E-Mail und Passwort nur
     * beim Wechsel von Schüler*in zu Lehrkraft/Admin (sie brauchen einen Zugang
     * zur Website). Antworten: 200, 400 (ungültig), 404 (unbekannte ID),
     * 409 (letzter Admin oder E-Mail schon vergeben) - jeweils mit Klartext.
     */
    @PostMapping(value = "/changeType", produces = MediaType.TEXT_PLAIN_VALUE)
    public synchronized ResponseEntity<String> changeType(@RequestBody(required = false) String body) {
        JSONObject json;
        try {
            json = new JSONObject(body == null ? "" : body);
        } catch (Exception e) {
            return text(400, "Can't parse JSON");
        }
        long id = json.optLong("id", 0);
        String type = json.optString("type");
        if (id == 0 || !(type.equals("STUDENT") || type.equals("TEACHER") || type.equals("ADMIN"))) {
            return text(400, "id oder type ungültig");
        }

        ResponseEntity<String>[] result = new ResponseEntity[]{text(200, "success")};
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            User user = session.get(User.class, id);
            if (user == null) {
                result[0] = text(404, "Unbekannte Person");
                return;
            }
            Level current = user instanceof Teacher teacher ? teacher.getLevel() : null;

            // Ohne Admin käme niemand mehr an die Verwaltung.
            if (current == Level.ADMIN && !type.equals("ADMIN")) {
                long admins = session.createSelectionQuery(
                        "select count(*) from Teacher t where t.level = :level", Long.class)
                        .setParameter("level", Level.ADMIN).getSingleResult();
                if (admins <= 1) {
                    result[0] = text(409, "Das ist der letzte Admin - zuerst jemand anderen zum Admin machen.");
                    return;
                }
            }

            if (type.equals("STUDENT")) {
                if (current == null) {
                    return; // ist schon Schüler*in
                }
                session.createNativeMutationQuery("delete from \"Kurs_user\" where \"tutor_id\" = :id")
                        .setParameter("id", id).executeUpdate();
                session.createNativeMutationQuery("update \"user\" set \"DTYPE\" = 'Student', \"mail\" = null, "
                                + "\"password\" = null, \"level\" = null where \"id\" = :id")
                        .setParameter("id", id).executeUpdate();
                return;
            }

            Level level = type.equals("ADMIN") ? Level.ADMIN : Level.NORMAL;
            if (current != null) {
                // Lehrkraft <-> Admin: nur die Stufe.
                session.createNativeMutationQuery("update \"user\" set \"level\" = :level where \"id\" = :id")
                        .setParameter("level", level.name()).setParameter("id", id).executeUpdate();
                return;
            }

            // Schüler*in -> Lehrkraft/Admin: braucht einen Zugang zur Website.
            String email = json.optString("email").trim();
            String password = json.optString("password");
            if (!email.contains("@") || password.length() < 8) {
                result[0] = text(400, "Für eine Lehrkraft braucht es eine E-Mail-Adresse und ein Passwort mit mindestens 8 Zeichen.");
                return;
            }
            long taken = session.createSelectionQuery(
                    "select count(*) from Teacher t where lower(t.email) = lower(:email)", Long.class)
                    .setParameter("email", email).getSingleResult();
            if (taken > 0) {
                result[0] = text(409, "Diese E-Mail-Adresse hat schon jemand anderes.");
                return;
            }
            session.createNativeMutationQuery("delete from \"user_Kurs\" where \"Student_id\" = :id")
                    .setParameter("id", id).executeUpdate();
            session.createNativeMutationQuery("update \"user\" set \"DTYPE\" = 'Teacher', \"mail\" = :email, "
                            + "\"password\" = :password, \"level\" = :level where \"id\" = :id")
                    .setParameter("email", email)
                    .setParameter("password", new CipherManager().hashPassword(password))
                    .setParameter("level", level.name())
                    .setParameter("id", id).executeUpdate();
        });
        return result[0];
    }

    private static ResponseEntity<String> text(int status, String body) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(body);
    }
}
