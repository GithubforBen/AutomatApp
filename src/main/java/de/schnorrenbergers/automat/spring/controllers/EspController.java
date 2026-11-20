package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.database.types.auth.HMACToken;
import de.schnorrenbergers.automat.manager.EspUpdateManager;
import de.schnorrenbergers.automat.spring.HmacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Liefert den ESP32 ihre Dateien für Updates über WLAN (siehe {@link EspUpdateManager}).
 * Die Anfragen sind wie alle anderen per HMAC signiert.
 */
@RestController
@RequestMapping("/esp")
public class EspController {

    @Autowired
    private HmacService hmacService;

    /**
     * Dateiliste für ein Gerät. Der Kopf {@code X-Manifest-Signature} signiert
     * {@code nonce:liste} mit dem Schlüssel der Anfrage - der ESP prüft damit, dass
     * die Liste wirklich vom Automaten kommt und frisch ist.
     */
    @GetMapping("/manifest")
    public ResponseEntity<byte[]> manifest(@RequestParam String board,
                                           @RequestHeader("X-API-KEY-ID") String keyId,
                                           @RequestHeader("X-NONCE") String nonce) throws IOException {
        EspUpdateManager.Board target = EspUpdateManager.Board.byFolder(board);
        if (target == null) return notFound();
        HMACToken token = hmacService.getTokenById(keyId);
        if (token == null) return ResponseEntity.status(401).build();
        String json = EspUpdateManager.manifest(target).toJson();
        String signature = hmacService.hmacSha256(nonce + ":" + json, token.secret);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Manifest-Signature", signature)
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Eine Datei aus der Dateiliste des Geräts - nur die, keine anderen Pfade.
     */
    @GetMapping("/file")
    public ResponseEntity<byte[]> file(@RequestParam String board, @RequestParam String path) throws IOException {
        EspUpdateManager.Board target = EspUpdateManager.Board.byFolder(board);
        if (target == null) return notFound();
        Path file = EspUpdateManager.manifest(target).files().get(path);
        if (file == null) return notFound();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(Files.readAllBytes(file));
    }

    private ResponseEntity<byte[]> notFound() {
        return ResponseEntity.status(404).build();
    }
}
