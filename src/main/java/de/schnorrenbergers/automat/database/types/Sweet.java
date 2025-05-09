package de.schnorrenbergers.automat.database.types;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Sweet {
    @Id
    @GeneratedValue
    private Long id;

    /**
     * Represents the type of the sweet.
     * <p>
     * This variable is used to define different categories or classifications
     * of sweets. The value is an integer that can correspond to specific
     * predefined types of sweets. Defined in the configuration.
     * <p>
     * It is encapsulated in the {@link Sweet} class and can be accessed or
     * modified using the {@link Sweet#getType()} and {@link Sweet#setType(int)} methods.
     */
    private int type;
    private int amount;

    public Sweet() {

    }

    public Sweet(int type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
