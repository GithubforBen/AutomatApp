package de.schnorrenbergers.automat.database.types;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Login {
    @Id
    @GeneratedValue
    private Long id;

    private Long userId;
    private Long loginTime;

    public Login(Long userId, Long loginTime) {
        this.userId = userId;
        this.loginTime = loginTime;
    }

    public Login() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    @Override
    public String toString() {
        return "Login{" +
                "id=" + id +
                ", userId=" + userId +
                ", loginTime=" + loginTime +
                '}';
    }
}
