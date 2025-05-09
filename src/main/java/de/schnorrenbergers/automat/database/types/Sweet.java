package de.schnorrenbergers.automat.database.types;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Sweet {
    @Id
    @GeneratedValue
    private Long id;

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
