package de.schnorrenbergers.automat.database.types.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class HMACToken {
    @Id
    public String id;
    public String secret;

    public HMACToken(String secret) {
        this.secret = secret;
    }

    public HMACToken() {

    }
}
