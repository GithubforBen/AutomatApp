package de.schnorrenbergers.automat.manager;

import at.favre.lib.crypto.bcrypt.BCrypt;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CipherManager {

    public static SecretKey generateKey(String pass) {
        return new SecretKeySpec(pass.getBytes(), "AES");
    }

    public String encrypt(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }
/*
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
 */
}
