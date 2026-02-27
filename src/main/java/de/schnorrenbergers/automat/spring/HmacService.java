package de.schnorrenbergers.automat.spring;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.auth.HMACToken;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class HmacService {

    public String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

    public HMACToken getTokenById(String id) {
        try (Session session = Main.getInstance().getDatabase().getSessionFactory().openSession()) {
            HMACToken hmacToken = session.get(HMACToken.class, id);
            session.close();
            return hmacToken;
        }
    }

    public boolean verifySignature(String keyId, String timestamp, String nonce, String signature, String body) {
        HMACToken token = getTokenById(keyId);
        if (token == null) {
            return false;
        }

        String data = keyId + ":" + timestamp + ":" + nonce + ":" + (body == null ? "" : body);
        String expectedSignature = hmacSha256(data, token.secret);

        return expectedSignature.equals(signature);
    }
}

