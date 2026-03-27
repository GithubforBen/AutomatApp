package de.schnorrenbergers.automat.manager;

import at.favre.lib.crypto.bcrypt.BCrypt;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CipherManager {

    public static SecretKey generateKey(String pass) {
        return new SecretKeySpec(pass.getBytes(), "AES");
    }

    public String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }
}
