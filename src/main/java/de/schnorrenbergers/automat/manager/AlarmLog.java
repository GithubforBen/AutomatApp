package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Protokoll aller Alarm-Auslösungen und -Abschaltungen, zur Suche nach
 * Fehlalarmen: {@code alarm.log} neben {@code automat.log} (install.sh:
 * ~/automat/logs), sonst im Arbeitsordner. Mit {@code -Dautomat.log-dir=...}
 * umstellbar. Ab 5 MB wird die Datei nach {@code alarm.log.1} verschoben.
 * <p>
 * Eine Zeile je Ereignis, z.B.:
 * <pre>
 * 2026-11-27 15:02:11  AUSGELÖST  Fach 3 (Haribo)  Lichtschranke 4  unterbrochen 230 ms
 *     Lichtschranken 0001000  kurze Impulse {"4":[2,40]}  seit Ausgabe 812 s  ESP läuft 3605 s  WLAN -61 dBm
 * </pre>
 */
public final class AlarmLog {

    private static final Logger LOG = Logger.getLogger(AlarmLog.class.getName());
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AlarmLog() {
    }

    /**
     * Meldung der Ausgabe: Lichtschranke ohne Ausgabe.
     *
     * @param type    Süßigkeiten-Typ (Fach)
     * @param details Rohdaten der Ausgabe ({@code sensor}, {@code blocked_ms}, ...), darf fehlen
     * @param outcome was daraus wurde (AUSGELÖST, IGNORIERT ...)
     */
    public static void unexpected(int type, JSONObject details, String outcome) {
        StringBuilder line = new StringBuilder();
        line.append(String.format("%-10s Fach %d (%s)", outcome, type, sweetName(type)));
        if (details != null) {
            if (details.has("sensor")) line.append("  Lichtschranke ").append(details.opt("sensor"));
            if (details.has("blocked_ms")) line.append("  unterbrochen ").append(details.opt("blocked_ms")).append(" ms");
            if (details.has("sensors")) line.append("  Lichtschranken ").append(details.opt("sensors"));
            JSONObject glitches = details.optJSONObject("glitches");
            if (glitches != null && !glitches.isEmpty()) {
                line.append("  kurze Impulse ").append(glitches);
            }
            if (details.has("since_dispense_ms")) {
                line.append("  seit Ausgabe ").append(details.optLong("since_dispense_ms") / 1000).append(" s");
            }
            if (details.has("uptime_s")) line.append("  ESP läuft ").append(details.opt("uptime_s")).append(" s");
            if (details.has("rssi")) line.append("  WLAN ").append(details.opt("rssi")).append(" dBm");
            if (!details.has("blocked_ms")) line.append("  (alte Ausgabe-Version ohne Details)");
        }
        write(line.toString());
    }

    /** Alarm ausgeschaltet (Admin-PIN, Karte, Einstellungen ...). */
    public static void disarmed(String reason) {
        write(String.format("%-10s %s", "AUS", reason));
    }

    /** Sonstiges Ereignis (z.B. Alarmknopf im Admin-Bildschirm). */
    public static void event(String what) {
        write(String.format("%-10s %s", "INFO", what));
    }

    private static synchronized void write(String text) {
        String line = LocalDateTime.now().format(TIME) + "  " + text + System.lineSeparator();
        try {
            Path file = file();
            if (Files.exists(file) && Files.size(file) > MAX_BYTES) {
                Files.move(file, file.resolveSibling("alarm.log.1"), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.warning("Alarm-Protokoll nicht geschrieben: " + e.getMessage());
        }
    }

    private static Path file() throws IOException {
        String configured = System.getProperty("automat.log-dir");
        Path dir;
        if (configured != null && !configured.isBlank()) {
            dir = Paths.get(configured);
        } else {
            // install.sh startet den Automaten in ~/automat/AutomatApp, die
            // Protokolle liegen in ~/automat/logs.
            Path installLogs = Paths.get("..", "logs");
            dir = Files.isDirectory(installLogs) ? installLogs : Paths.get(".");
        }
        Files.createDirectories(dir);
        return dir.resolve("alarm.log");
    }

    private static String sweetName(int type) {
        try {
            return Main.getInstance().getConfigurationManager().getString("sweets._" + type + ".name");
        } catch (Exception e) {
            return "?";
        }
    }
}
