package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.utils.CustomRequest;
import de.schnorrenbergers.automat.utils.DeviceSigner;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
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
    /** Aufgeschobenes Update (Alarm, Gerät beschäftigt/nicht erreichbar): bald erneut versuchen. */
    private static final long RETRY_SECONDS = 60;
    private static ScheduledExecutorService scheduler;
    /** Geräte mit schon geplantem Neuversuch - damit sich keine stapeln. */
    private static final Set<Board> retryPlanned = ConcurrentHashMap.newKeySet();
    /** Zuletzt angestoßene Zielversion je Gerät und wie oft schon. */
    private static final Map<Board, String> lastTarget = new ConcurrentHashMap<>();
    private static final Map<Board, Integer> attempts = new ConcurrentHashMap<>();

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
            checkLogged(board);
        }
    }

    private static void checkLogged(Board board) {
        try {
            check(board);
        } catch (Exception e) {
            LOG.warning("ESP-Update " + board.folder + ": " + e.getMessage());
            retrySoon(board);
        }
    }

    /** Nach RETRY_SECONDS erneut prüfen statt erst beim nächsten regulären Durchlauf. */
    private static void retrySoon(Board board) {
        if (scheduler == null || !retryPlanned.add(board)) return;
        scheduler.schedule(() -> {
            retryPlanned.remove(board);
            checkLogged(board);
        }, RETRY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Fragt die Version des ESP ab und stößt ein Update an, wenn sie nicht passt.
     */
    public static void check(Board board) throws IOException {
        // Ein Neustart des Scanners würde den Summer abschalten. Die anderen
        // Geräte nicht aufhalten - sonst käme z.B. eine Ausgabe, die Fehlalarme
        // auslöst, nie an ihre Korrektur.
        if (board == Board.SCANNER && Main.getInstance().isAlarm()) {
            LOG.info("ESP-Update " + board.folder + " verschoben: Alarm läuft");
            retrySoon(board);
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
        String current = installed.body().trim();
        if (target.equals(current)) {
            if (attempts.remove(board) != null) {
                LOG.info("ESP-Update " + board.folder + ": Version " + target + " läuft");
            }
            lastTarget.remove(board);
            return;
        }
        int tries = target.equals(lastTarget.get(board)) ? attempts.getOrDefault(board, 0) : 0;
        if (tries > 0) {
            // Update lief schon, Gerät ist aber wieder auf der alten Version:
            // die neue startete nicht und wurde zurückgerollt (esp/common/boot.py).
            LOG.warning("ESP-Update " + board.folder + ": nach " + tries + " Versuch(en) immer noch " + current
                    + " statt " + target + " - die neue Version startet vermutlich nicht und wurde"
                    + " zurückgerollt. Ausgabe des ESP per USB ansehen.");
        }
        Response answer = get(base + "/update");
        String reply = answer.body().trim();
        LOG.info("ESP-Update " + board.folder + ": " + current + " -> " + target + ", Antwort: " + reply);
        if ("ok".equals(reply)) {
            lastTarget.put(board, target);
            attempts.put(board, tries + 1);
            pingWhileOnTrial(board, target);
        } else if ("busy".equals(reply) || "läuft schon".equals(reply)) {
            // Ausgabe läuft gerade bzw. Update läuft schon - gleich nochmal.
            retrySoon(board);
        }
    }

    /**
     * Pingt das Gerät eine Weile regelmäßig, damit es seine neue Version nach dem
     * Neustart bestätigt, statt sie wieder zurückzurollen.
     */
    private static void pingWhileOnTrial(Board board, String target) {
        // Je Update-Versuch nur einmal melden.
        java.util.concurrent.atomic.AtomicBoolean answered = new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.atomic.AtomicBoolean crashLogged = new java.util.concurrent.atomic.AtomicBoolean();
        for (long delay = CONFIRM_PING_SECONDS; delay <= CONFIRM_PING_FOR_SECONDS; delay += CONFIRM_PING_SECONDS) {
            scheduler.schedule(() -> {
                try {
                    String base = Main.getInstance().getUrl(board.reviver);
                    Response ping = get(base + "/ping");
                    if (ping.status() == 503) {
                        // Neue Version ist beim Start abgestürzt (esp/<board>/crash.py).
                        if (!crashLogged.getAndSet(true)) {
                            Response crash = get(base + "/crash");
                            LOG.warning("ESP-Update " + board.folder + ": neue Version startet nicht:\n"
                                    + crash.body().trim());
                        }
                    } else if (ping.status() == 200 && !answered.get()
                            && target.equals(get(base + "/version").body().trim())) {
                        // Vorher antwortet noch die alte Version (lädt gerade herunter).
                        answered.set(true);
                        LOG.info("ESP-Update " + board.folder + ": neue Version " + target + " läuft und antwortet");
                    }
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
        // Updates nimmt ein ESP nur mit Signatur des Automaten an.
        DeviceSigner.sign(connection, "");
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
