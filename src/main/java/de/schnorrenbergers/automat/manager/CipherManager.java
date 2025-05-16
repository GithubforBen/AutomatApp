package de.schnorrenbergers.automat.manager;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class CipherManager {

    public static SecretKey generateKey(String pass) {
        return new SecretKeySpec(pass.getBytes(), "AES");
    }

    public String encrypt(String valueToEnc, String pass) throws Exception {
        Key key = generateKey(pass);
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encValue = c.doFinal(valueToEnc.getBytes());
        return Base64.getEncoder().encodeToString(encValue);
    }

    public String decrypt(String encryptedValue, String pass) throws Exception {
        Key key = generateKey(pass);
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = Base64.getDecoder().decode(encryptedValue);
        byte[] decValue = c.doFinal(decordedValue);
        return new String(decValue);
    }

    public byte[] hash(String pass) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        return md.digest(pass.getBytes());
    }
}
