package de.schnorrenbergers.automat.database.types;

import jakarta.persistence.*;

/**
 * Used as a type for storing SettingsManager.
 * key: used to identify setting
 * value: the value of the setting
 */
@Entity
@Table(name = "settings")
public class Setting {

    @Id
    @Column(name = "id")
    @GeneratedValue
    private long id;

    @Column(name = "key", unique = true, nullable = false)
    private String key;

    @Column(name = "value")
    private String value;

    public Setting(String key, String value) {
        this.value = value;
        this.key = key;
    }

    public Setting() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
