package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.utils.CustomRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Updates der ESP32 über WLAN. Die ESPs holen ihre Dateien hier ab (siehe
 * {@code EspController} und {@code esp/common/ota.py}) - aus dem Ordner {@code esp/}
 * des Projekts, mit dem der Automat gerade läuft. Nach einem Update des Automaten
 * bekommen die ESPs so automatisch dieselbe Version.
 * <p>
 * Jedes Gerät bekommt {@code esp/common/} plus seinen eigenen Ordner; bei gleichem
 * Pfad gewinnt der eigene. Nicht übertragen werden {@code boot.py} (das
 * Sicherheitsnetz, nur per USB), {@code .env}-Dateien und alles, was mit einem
 * Punkt beginnt.
 */
public final class EspUpdateManager {

    private static final Logger LOG = Logger.getLogger(EspUpdateManager.class.getName());
    private static final long FIRST_CHECK_SECONDS = 30;
    private static final long CHECK_EVERY_MINUTES = 15;
    /**
     * Nach einem Update läuft die neue Version auf Probe und muss innerhalb von
     * 3 Minuten einen /ping bekommen (siehe esp/common/boot.py). Scanner und Station
     * pingt der Automat sonst nur beim Start - also hier eine Weile nachhelfen.
     */
    private static final long CONFIRM_PING_SECONDS = 20;
    private static final long CONFIRM_PING_FOR_SECONDS = 6 * 60;
    private static ScheduledExecutorService scheduler;

    /**
     * Die ESPs, ihr Ordner unter {@code esp/} und wie der Automat sie erreicht.
     */
    public enum Board {
        AUSGABE("ausgabe", CustomRequest.REVIVER.DISPENSER),
        SCANNER("scanner", CustomRequest.REVIVER.SCANNER),
        STATION("station", CustomRequest.REVIVER.STATION);

        public final String folder;
        public final CustomRequest.REVIVER reviver;

        Board(String folder, CustomRequest.REVIVER reviver) {
            this.folder = folder;
            this.reviver = reviver;
        }

        public static Board byFolder(String folder) {
            for (Board board : values()) {
                if (board.folder.equals(folder)) return board;
            }
            return null;
        }
    }

    /**
     * @param files Pfad auf dem ESP -> Datei im Projekt (sortiert)
     */
    public record Manifest(Board board, String version, Map<String, Path> files, Map<String, String> hashes) {
        public String toJson() {
            JSONArray list = new JSONArray();
            files.forEach((path, file) -> {
                try {
                    list.put(new JSONObject()
                            .put("path", path)
                            .put("sha256", hashes.get(path))
                            .put("size", Files.size(file)));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            return new JSONObject()
                    .put("board", board.folder)
                    .put("version", version)
                    .put("files", list)
                    .toString();
        }
    }

    private EspUpdateManager() {
    }

    /**
     * Ordner mit den ESP-Programmen. Der Automat läuft im Projektordner (siehe
     * install.sh); mit {@code -Dautomat.esp-dir=...} lässt er sich umstellen.
     */
    public static Path root() {
        return Paths.get(System.getProperty("automat.esp-dir", "esp")).toAbsolutePath().normalize();
    }

    public static Manifest manifest(Board board) throws IOException {
        Map<String, Path> files = new TreeMap<>();
        collect(root().resolve("common"), files);
        collect(root().resolve(board.folder), files);
        Map<String, String> hashes = new TreeMap<>();
        StringBuilder all = new StringBuilder();
        for (Map.Entry<String, Path> entry : files.entrySet()) {
            String hash = sha256(Files.readAllBytes(entry.getValue()));
            hashes.put(entry.getKey(), hash);
            all.append(entry.getKey()).append(':').append(hash).append('\n');
        }
        String version = sha256(all.toString().getBytes(StandardCharsets.UTF_8)).substring(0, 12);
        return new Manifest(board, version, files, hashes);
    }

    private static void collect(Path dir, Map<String, Path> files) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                String path = dir.relativize(file).toString().replace('\\', '/');
                if (isTransferred(path)) files.put(path, file);
            });
        }
    }

    private static boolean isTransferred(String path) {
        if (path.equals("boot.py")) return false;
        if (path.endsWith(".pyc") || path.endsWith(".md")) return false;
        for (String part : path.split("/")) {
            if (part.startsWith(".") || part.equals("__pycache__")) return false;
        }
        return true;
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Prüft kurz nach dem Start (also auch nach einem Update des Automaten) und
     * dann regelmäßig, ob die ESPs aktuell sind. Mehrfache Aufrufe schaden nicht.
     */
    public static synchronized void start() {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "esp-update");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(EspUpdateManager::checkAll,
                FIRST_CHECK_SECONDS, CHECK_EVERY_MINUTES * 60, TimeUnit.SECONDS);
    }

    public static void checkAll() {
        for (Board board : Board.values()) {
            try {
                check(board);
            } catch (Exception e) {
                LOG.warning("ESP-Update " + board.folder + ": " + e.getMessage());
            }
        }
    }

    /**
     * Fragt die Version des ESP ab und stößt ein Update an, wenn sie nicht passt.
     */
    public static void check(Board board) throws IOException {
        // Ein Neustart des Scanners würde den Summer abschalten.
        if (Main.getInstance().isAlarm()) {
            LOG.info("ESP-Update " + board.folder + " verschoben: Alarm läuft");
            return;
        }
        String target = manifest(board).version();
        String base = Main.getInstance().getUrl(board.reviver);
        Response installed = get(base + "/version");
        if (installed.status() == 404) {
            LOG.info("ESP-Update " + board.folder + ": Programm kann sich noch nicht selbst aktualisieren - einmal per USB aufspielen");
            return;
        }
        if (installed.status() != 200) {
            LOG.info("ESP-Update " + board.folder + ": /version antwortet mit " + installed.status());
            return;
        }
        if (target.equals(installed.body().trim())) return;
        Response answer = get(base + "/update");
        LOG.info("ESP-Update " + board.folder + ": " + installed.body().trim() + " -> " + target
                + ", Antwort: " + answer.body().trim());
        if ("ok".equals(answer.body().trim())) pingWhileOnTrial(board);
    }

    /**
     * Pingt das Gerät eine Weile regelmäßig, damit es seine neue Version nach dem
     * Neustart bestätigt, statt sie wieder zurückzurollen.
     */
    private static void pingWhileOnTrial(Board board) {
        for (long delay = CONFIRM_PING_SECONDS; delay <= CONFIRM_PING_FOR_SECONDS; delay += CONFIRM_PING_SECONDS) {
            scheduler.schedule(() -> {
                try {
                    get(Main.getInstance().getUrl(board.reviver) + "/ping");
                } catch (Exception e) {
                    // Startet gerade neu - beim nächsten Mal.
                }
            }, delay, TimeUnit.SECONDS);
        }
    }

    private record Response(int status, String body) {
    }

    private static Response get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(4000);
        // Die ESPs stellen damit ihre Uhr - ihre Anfragen hier sind signiert und
        // nur mit passender Uhrzeit gültig.
        connection.setRequestProperty("X-Unix-Time", String.valueOf(Instant.now().getEpochSecond()));
        try {
            int status = connection.getResponseCode();
            InputStream stream = status < 400 ? connection.getInputStream() : connection.getErrorStream();
            String body = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new Response(status, body);
        } finally {
            connection.disconnect();
        }
    }
}
