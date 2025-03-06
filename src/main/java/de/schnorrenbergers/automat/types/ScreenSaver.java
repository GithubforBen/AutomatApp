package de.schnorrenbergers.automat.types;

import de.schnorrenbergers.automat.Main;

import java.util.Date;

public class ScreenSaver {
    private long lastMove;
    private boolean permanent = false;
    private boolean doSaver;
    private boolean isSaverr = false;

    public ScreenSaver() {
        lastMove = System.currentTimeMillis() - (10L * 60000);
        setDoSaver((Boolean) Main.getInstance().getStatistic().getSettingOrDefault("doSaver", true));
    }

    public boolean isSaver() {
        Date date = new Date();
        if (!doSaver) {
            return false;
        }
        if (date.getHours() >= 14-1 && date.getHours() <= 17-1) {
            return false;
        }
        return (lastMove+(10L * 60000) <= System.currentTimeMillis() || permanent);
    }

    public long getLastMove() {
        return lastMove;
    }

    public void setLastMove(long lastMove) {
        this.lastMove = lastMove;
    }

    public boolean isDoSaver() {
        return doSaver;
    }

    public void setDoSaver(boolean doSaver) {
        this.doSaver = doSaver;
        Main.getInstance().getStatistic().setSetting("doSaver", doSaver);
    }

    public void setSaver(boolean saverr) {
        isSaverr = saverr;
    }

    public boolean isSaverr() {
        return isSaverr;
    }
}
