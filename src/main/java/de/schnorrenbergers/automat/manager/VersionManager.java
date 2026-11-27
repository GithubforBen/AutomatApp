package de.schnorrenbergers.automat.manager;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version des Automaten und ob es eine neuere gibt.
 * <p>
 * Jeder Push auf master bekommt automatisch eine Nummer (v0.1, v0.2, ...;
 * siehe .github/workflows/version-tag.yml). Der Automat läuft in seinem
 * git-Ordner (install.sh) - die installierte Version kommt aus
 * {@code git describe}, die neueste aus {@code git ls-remote} (höchstens alle
 * 30 Minuten gefragt).
 */
public final class VersionManager {

    private static final Logger LOG = Logger.getLogger(VersionManager.class.getName());
    private static final long REMOTE_CACHE_MILLIS = TimeUnit.MINUTES.toMillis(30);
    private static final Pattern TAG = Pattern.compile("^([0-9a-f]{40})\\s+refs/tags/(v0\\.(\\d+))$");

    private static Remote cachedRemote;
    private static long cachedAt;

    private VersionManager() {
    }

    /** Neueste Version auf GitHub: Name und Commit. */
    private record Remote(String name, int number, String commit) {
    }

    /**
     * @return z.B. {@code {"installed":"v0.5","latest":"v0.7","updateAvailable":true,"checkedAt":...}};
     * {@code latest} fehlt, wenn GitHub nicht erreichbar war
     */
    public static JSONObject status() {
        JSONObject json = new JSONObject();
        String head = git("rev-parse", "HEAD");
        Remote latest = latest();
        String installed = installed();
        boolean updateAvailable = false;
        if (latest != null) {
            json.put("latest", latest.name());
            if (latest.commit().equals(head)) {
                // Tag kam erst nach dem letzten Holen dazu - gleiche Version.
                installed = latest.name();
            } else {
                // Nicht vorhanden oder kein Vorgänger von HEAD: es gibt Neueres.
                updateAvailable = head == null || !isAncestor(latest.commit(), head);
            }
        }
        json.put("installed", installed);
        json.put("updateAvailable", updateAvailable);
        json.put("checkedAt", cachedAt == 0 ? JSONObject.NULL : Instant.ofEpochMilli(cachedAt).toString());
        return json;
    }

    /** Installierte Version, z.B. "v0.5", "v0.5-2-gabc1234" oder nur der Commit. */
    public static String installed() {
        String described = git("describe", "--tags", "--match", "v0.*");
        if (described != null) return described;
        String shortHead = git("rev-parse", "--short", "HEAD");
        return shortHead == null ? "unbekannt" : shortHead;
    }

    private static synchronized Remote latest() {
        if (cachedRemote != null && System.currentTimeMillis() - cachedAt < REMOTE_CACHE_MILLIS) {
            return cachedRemote;
        }
        String output = git("ls-remote", "--tags", "origin", "v0.*");
        if (output == null) return cachedRemote;  // offline: letzter Stand
        Remote best = null;
        for (String line : output.split("\n")) {
            Matcher m = TAG.matcher(line.trim());
            if (!m.matches()) continue;
            int number = Integer.parseInt(m.group(3));
            if (best == null || number > best.number()) best = new Remote(m.group(2), number, m.group(1));
        }
        cachedRemote = best;
        cachedAt = System.currentTimeMillis();
        return best;
    }

    private static boolean isAncestor(String commit, String head) {
        return run(List.of("git", "merge-base", "--is-ancestor", commit, head)) != null;
    }

    /** Gibt die Ausgabe zurück, oder {@code null} bei Fehler. */
    private static String git(String... args) {
        List<String> command = new java.util.ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        String out = run(command);
        return out == null || out.isBlank() ? null : out.trim();
    }

    private static String run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            // Nie nach Zugangsdaten fragen (öffentliches Repository).
            process.getOutputStream().close();
            byte[] out;
            try (InputStream in = process.getInputStream()) {
                out = in.readAllBytes();
            }
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            return process.exitValue() == 0 ? new String(out, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            LOG.fine("git nicht ausführbar: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
