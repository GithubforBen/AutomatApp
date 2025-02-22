package de.schnorrenbergers.automat.types;

import java.util.Arrays;

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
        return "{\"name\":\""+name+"\",\"time\":"+timeSec+",\"byteAdress\":" + Arrays.toString(byteAdress)+"}";
    }
}
