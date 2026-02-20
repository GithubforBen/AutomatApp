package de.schnorrenbergers.automat.database.types.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class HMACToken {
    @Id
    public String id;
    @Column(length = 16000)
    public String secret;

    public HMACToken(String secret) {
        this.secret = secret;
    }

    public HMACToken() {

    }
}
