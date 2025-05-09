package de.schnorrenbergers.automat.database.types;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Konto {
    @Id
    @GeneratedValue
    private Long id;

    private Long userId;
    private double balance;
    private boolean isInfinite;

    public Konto(Long userId, double balance, boolean isInfinite) {
        this.userId = userId;
        this.balance = balance;
        this.isInfinite = isInfinite;
    }

    public Konto() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean withdraw(double amount) {
        if (amount > balance) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public boolean deposit(double amount) {
        if (amount < 0) {
            return false;
        }
        System.out.println("Deposit(" + userId + "): " + amount + " (" + balance + ")");
        balance += amount;
        return true;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isInfinite() {
        return isInfinite;
    }

    public void setInfinite(boolean infinite) {
        isInfinite = infinite;
    }

    public double getBalanceRounded() {
        return ((int) (balance * 10)) / 10.0;
    }

    @Override
    public String toString() {
        return "Konto{" + "id=" + id + ", userId=" + userId + ", balance=" + balance + ", isInfinite=" + isInfinite + '}';
    }
}
