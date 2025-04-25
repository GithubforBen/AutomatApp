package de.schnorrenbergers.automat.database.types.types;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Represents a location with address details such as street, city, postal code, and country.
 * This class is annotated as an entity for persistence in a database.
 */
@Entity
public class Wohnort {

    private int number;
    private String street;
    private String city;
    private int zip;
    private String country;
    @Id
    @GeneratedValue
    private Long id;

    public Wohnort(int number, String street, String city, int zip, String country) {
        this.number = number;
        this.street = street;
        this.city = city;
        this.zip = zip;
        this.country = country;
    }

    public Wohnort() {}

    @Override
    public String toString() {
        return street + " " + number + "\n" + city + " " + zip + "\n" + country;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getZip() {
        return zip;
    }

    public void setZip(int zip) {
        this.zip = zip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
