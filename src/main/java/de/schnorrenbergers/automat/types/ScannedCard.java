package de.schnorrenbergers.automat.types;

import java.util.Arrays;

/**
 * This is used for storing a card which was scanned.
 * Represents a scanned card with details such as a name, the time in seconds since a specific event,
 * the formatted time, and a byte address array.
 * This class provides methods to access these properties and offers a string representation of the card's information.
 */
public class ScannedCard {
    public String name;
    public int timeSec;
    public Time time;
    int[] byteAdress;

    public ScannedCard(int timeSec, String name, int[] byteAdress) {
        this.timeSec = timeSec;
        this.name = name;
        this.byteAdress = byteAdress;
        this.time = new Time(timeSec);
    }

    public String toString() {
        return "{\"name\":\"" + name + "\",\"time\":" + timeSec + ",\"byteAdress\":" + Arrays.toString(byteAdress) + "}";
    }

    public String getName() {
        return name;
    }

    public int getTimeSec() {
        return timeSec;
    }

    public Time getTime() {
        return time;
    }

    public int[] getByteAdress() {
        return byteAdress;
    }
}
