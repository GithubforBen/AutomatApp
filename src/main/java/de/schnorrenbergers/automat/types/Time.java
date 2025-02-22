package de.schnorrenbergers.automat.types;

public class Time {
    public int seconds;

    public double getHour() {
        return seconds / 3600.0;
    }

    public double getMinute() {
        return seconds / 60.0;
    }

    public double getSecond() {
        return seconds;
    }

    public Time(int seconds) {
        this.seconds = seconds;
    }

    public Time(double hours) {
        this.seconds = (int) (hours * 3600.0);
    }
}
