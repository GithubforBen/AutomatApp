package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.manager.VersionManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Angaben zum Automaten selbst, für die Website. Wie alle Anfragen per HMAC signiert.
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    /**
     * Installierte und neueste Version, z.B.
     * {@code {"installed":"v0.5","latest":"v0.7","updateAvailable":true,"checkedAt":"2026-11-27T15:00:00Z"}}.
     * Aktualisiert wird am Automaten mit "Automat aktualisieren" auf dem Desktop.
     */
    @GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> version() {
        return ResponseEntity.ok(VersionManager.status().toString());
    }
}
