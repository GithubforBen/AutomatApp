package de.schnorrenbergers.automat.utils;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.auth.HMACToken;
import org.hibernate.Session;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Signiert Anfragen des Automaten an die ESP32 (Ausgabe, Scanner, Station) - wie
 * die Website ihre Anfragen ans Backend: HMAC-SHA256 über
 * {@code keyId:zeitstempel:nonce:body}. Die ESPs führen Ausgabe, Alarm und Updates
 * nur mit gültiger Signatur aus (esp/common/signing.py); sonst könnte jeder im WLAN
 * den Automaten bedienen.
 * <p>
 * Der Schlüssel ist der HMAC-Schlüssel der ESPs ({@code HMAC_KEY_ID} in deren .env);
 * welcher das ist, steht in der config.yaml unter {@code esp-key-id}.
 */
public final class DeviceSigner {

    private static final Logger LOG = Logger.getLogger(DeviceSigner.class.getName());
    private static final String DEFAULT_KEY_ID = "ausgabe";
    private static final long CACHE_MS = 60_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile String cachedKeyId;
    private static volatile String cachedSecret;
    private static volatile long cachedAt;
    private static volatile boolean warned;

    private DeviceSigner() {
    }

    /**
     * Köpfe für eine signierte Anfrage. Leer, wenn der Schlüssel fehlt - dann lehnen
     * die ESPs Ausgabe, Alarm und Updates ab.
     */
    public static Map<String, String> headers(String body) {
        String keyId = keyId();
        String secret = secret(keyId);
        Map<String, String> headers = new LinkedHashMap<>();
        if (secret == null) {
            if (!warned) {
                warned = true;
                LOG.warning("HMAC-Schlüssel \"" + keyId + "\" (esp-key-id) fehlt - Anfragen an die ESPs sind unsigniert");
            }
            return headers;
        }
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        byte[] nonceBytes = new byte[16];
        RANDOM.nextBytes(nonceBytes);
        String nonce = HexFormat.of().formatHex(nonceBytes);
        String data = keyId + ":" + timestamp + ":" + nonce + ":" + (body == null ? "" : body);
        headers.put("X-API-KEY-ID", keyId);
        headers.put("X-TIMESTAMP", timestamp);
        headers.put("X-NONCE", nonce);
        headers.put("X-SIGNATURE", hmac(secret, data));
        return headers;
    }

    /** Signiert eine Anfrage, deren Körper {@code body} ist (bei GET: leer). */
    public static void sign(URLConnection connection, String body) {
        headers(body).forEach(connection::setRequestProperty);
    }

    private static String keyId() {
        try {
            Object configured = Main.getInstance().getConfigurationManager().get("esp-key-id");
            if (configured != null && !String.valueOf(configured).isBlank()) return String.valueOf(configured).trim();
        } catch (Exception ignored) {
            // Ohne Konfiguration der Standard.
        }
        return DEFAULT_KEY_ID;
    }

    private static String secret(String keyId) {
        long now = System.currentTimeMillis();
        if (keyId.equals(cachedKeyId) && cachedSecret != null && now - cachedAt < CACHE_MS) {
            return cachedSecret;
        }
        try (Session session = Main.getInstance().getDatabase().getSessionFactory().openSession()) {
            HMACToken token = session.get(HMACToken.class, keyId);
            cachedKeyId = keyId;
            cachedSecret = token == null ? null : token.secret;
            cachedAt = now;
            return cachedSecret;
        } catch (Exception e) {
            return null;
        }
    }

    private static String hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }
}
