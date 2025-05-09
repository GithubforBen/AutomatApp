package de.schnorrenbergers.automat.database.types;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Represents a login entity in the database.
 * <p>
 * This class is annotated as an entity using JPA annotations, allowing it to be
 * stored and managed in a relational database. It is primarily used to record
 * login events of users, with each login associated with a unique identifier,
 * a user ID, and the time of the login.
 *
 * <p>
 * Fields:
 * <ul>
 * <li><b>id:</b> A unique identifier for each login record (auto-generated).</li>
 * <li><b>userId:</b> The identifier of the user associated with the login event.</li>
 * <li><b>loginTime:</b> The timestamp indicating when the login occurred.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Constructors:
 * <ul>
 * <li>A default no-arg constructor used for creating an empty login object.</li>
 * <li>A parameterized constructor to initialize a login instance with a user ID and login time.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Methods:
 * <ul>
 * <li>{@link #getId()} and {@link #setId(Long)} for retrieving and setting the unique identifier.</li>
 * <li>{@link #getUserId()} and {@link #setUserId(Long)} for managing the associated user ID.</li>
 * <li>{@link #getLoginTime()} and {@link #setLoginTime(Long)} for accessing and updating the login timestamp.</li>
 * <li>Overrides {@link #toString()} to provide a string representation of the login entity.</li>
 * </ul>
 * </p>
 */
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
