package de.schnorrenbergers.automat;

import atlantafx.base.theme.PrimerDark;
import de.schnorrenbergers.automat.controller.AddUserController;
import de.schnorrenbergers.automat.controller.MainController;
import de.schnorrenbergers.automat.database.Database;
import de.schnorrenbergers.automat.database.DatabaseSetup;
import de.schnorrenbergers.automat.database.types.*;
import de.schnorrenbergers.automat.manager.*;
import de.schnorrenbergers.automat.server.Server;
import de.schnorrenbergers.automat.spring.SpringApi;
import de.schnorrenbergers.automat.utils.CustomRequest;
import de.schnorrenbergers.automat.utils.types.ScreenSaver;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.*;
import javafx.scene.layout.Border;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Für jeden der sich denkt, dass man das "nur mal kurz" fixt, NEIN lass ab. Selbst ich habe keine ahnung was hier wie geschieht.
 * Das Projekt ist entstanden in "Ich mache mal ne richtige Datenbank und programm!" knapp 4500 Zeilen Code später weiß ich nicht mehr was wo was tut.
 */
public class Main extends Application {
    private static Main instance;
    private Stage stage;
    private Dimension2D dimension;
    private Server server;
    private int[] lastScan;
    private int logoutTime;
    private volatile boolean alarm = false;
    private boolean checkAvailability;
    private volatile boolean autoLock;
    /** Alarm bei Eingriff (Lichtschranke ohne Ausgabe) - in den Einstellungen abschaltbar. */
    private volatile boolean alarmEnabled = true;
    private boolean useLocalEndpoints = false;
    private ScreenSaver screenSaver;
    private Database database;
    private SettingsManager settingsManager;
    private StatisticManager handler;
    private ConfigurationManager configurationManager;
    private ConfigurableApplicationContext run;
    private boolean initialized = false;
    private static final long DISPENSER_CHECK_MS = 5000;
    private Thread dispenserWatchdog;
    /**
     * Die Verbindung ist weg, während der Bildschirmschoner läuft - der soll
     * weiterlaufen; "Verbindung wiederherstellen" kommt erst, wenn ihn jemand beendet.
     */
    private volatile boolean reconnectPending = false;
    /** Welche Ansicht zu sehen war, als der Bildschirmschoner anging. */
    private volatile String sceneBeforeSaver;

    public static void main(String[] args) {
        launch();
    }

    public static Main getInstance() {
        return instance;
    }

    /**
     * Probelauf für das Startarchiv (Class Data Sharing, siehe {@link CdsTraining}):
     * lädt einmal alles, was nach der PIN-Eingabe gebraucht wird - Datenbank,
     * Hibernate, Spring -, mit einer Wegwerf-Datenbank im aktuellen Ordner.
     */
    static void warmUpForClassArchive() throws Exception {
        Main main = new Main();
        instance = main;
        main.configurationManager = new ConfigurationManager();
        main.database = new Database(DatabaseSetup.USER, "00000", "00000");
        main.initialise();
        main.run.close();
        main.database.getSessionFactory().close();
    }

    public void initialise() throws Exception {
        if (initialized) {
            System.out.println("Already initialized, skipping...");
            return;
        }
    
        System.out.println("Initialize");
        logoutTime = configurationManager.getInt("default-logout-time");
        settingsManager = new SettingsManager();
        screenSaver = new ScreenSaver();
        handler = new StatisticManager();
        logoutTime = Integer.parseInt(settingsManager.getSettingOrDefault("logout", String.valueOf(logoutTime)));
        checkAvailability = Boolean.parseBoolean(settingsManager.getSettingOrDefault("availability", String.valueOf(false)));
        autoLock = Boolean.parseBoolean(settingsManager.getSettingOrDefault("autoLock", String.valueOf(false)));
        alarmEnabled = Boolean.parseBoolean(settingsManager.getSettingOrDefault("alarmEnabled", String.valueOf(true)));
        if (run == null) {
            run = SpringApplication.run(SpringApi.class);
        }

        initialized = true;
    }

    @Override
    public void stop() throws Exception {
        run.close();
        try {
            configurationManager.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param stage the primary stage for this application, onto which
     *              the application scene can be set.
     *              Applications may create other stages, if needed, but they will not be
     *              primary stages.
     * @throws IOException            Inherited form {@link #initialise()}
     * @throws InterruptedException   Inherited form {@link #initialise()}
     * @throws SQLException           Programm wont work.
     * @throws ClassNotFoundException Inherited form {@link #initialise()}
     */
    @Override
    public void start(Stage stage) throws Exception {
        instance = this;
        configurationManager = new ConfigurationManager();
        dimension = new Dimension2D(
                configurationManager.getInt("window-dimension.horizontal"),
                configurationManager.getInt("window-dimension.vertical"));
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), dimension.getWidth(), dimension.getHeight());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
        this.stage = stage;
        stage.setFullScreenExitHint("");
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        stage.setAlwaysOnTop(true);
    }

    public void startWithoutPassword(Stage stage) throws Exception {
        instance = this;
        configurationManager = new ConfigurationManager();
        dimension = new Dimension2D(
                configurationManager.getInt("window-dimension.horizontal"),
                configurationManager.getInt("window-dimension.vertical"));
        this.stage = stage;
        stage.setTitle("Hello!");
        stage.setFullScreenExitHint("");
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        stage.setAlwaysOnTop(true);
        // Über loadScene: das behält den Vollbildmodus (setScene allein beendet ihn,
        // nach "Verbindung wiederherstellen" war er weg) und merkt sich den Namen der
        // Ansicht, an dem Verbindungsbildschirm und Bildschirmschoner sie erkennen.
        loadScene("main-view.fxml");
    }

    /**
     * Continuously monitors the state of the screensaver and triggers the loading of the screensaver scene
     * when certain conditions are met.
     * <p>
     * This method runs indefinitely in a separate thread, periodically checking whether the screensaver
     * should be activated. If the screensaver is enabled (`isSaver` returns true) and has not been
     * previously activated (`isSaverr` is false), the screensaver scene is loaded, and its state is updated
     * accordingly. The screensaver's state is managed through the `ScreenSaver` object.
     * <p>
     * Functionality:
     * - Executes in an infinite loop.
     * - Pauses the thread for 400 milliseconds between checks.
     * - Retrieves the current screensaver state and verifies whether it needs to be activated.
     * - Utilizes the `Platform.runLater` method to ensure that UI changes (e.g., scene loading) are performed
     * on the JavaFX Application Thread.
     * - Recursively calls itself to maintain the monitoring process.
     * <p>
     * Potential exceptions during execution, such as `InterruptedException`, are caught and logged to the console.
     * <p>
     * Note: This method should only be called once, typically during the application's initialization phase,
     * as it creates a new thread for monitoring and recursion. Misuse could lead to multiple redundant threads.
     */
    public void checkForStuff() {
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(400);
                    boolean saver = Main.getInstance().getScreenSaver().isSaver();
                    if (saver && !Main.getInstance().getScreenSaver().isSaverr()) {
                        Runnable runnable = () -> {
                            Object current = stage.getScene() == null ? null : stage.getScene().getUserData();
                            sceneBeforeSaver = current == null ? null : current.toString();
                            loadScene("screenSaver.fxml");
                        };
                        Platform.runLater(runnable);
                        Main.getInstance().getScreenSaver().setSaver(true);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            checkForStuff();
        }).start();
    }

    /**
     * Loads a new scene for the application, based on the provided scene file name.
     * The scene dimensions are determined by the application's main instance dimensions.
     * It preserves the fullscreen state of the current stage after loading the new scene.
     *
     * @param sceneName the name of the FXML file to load, which defines the new scene.
     *                  The file should be located in the application's resources directory.
     *                  If the file cannot be loaded, a {@link RuntimeException} is thrown.
     */
    public void loadScene(String sceneName) {
        boolean fullScreen = stage.isFullScreen();
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(sceneName));
        Scene scene = null;
        try {
            scene = new Scene(fxmlLoader.load(), Main.getInstance().getDimension().getWidth(), Main.getInstance().getDimension().getHeight());
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
        scene.setUserData(sceneName);
        stage.setFullScreen(fullScreen);
        stage.setScene(scene);
        stage.setFullScreen(fullScreen);
        stage.show();
    }

    public void load() throws IOException {
        System.out.println("Load");
        stage.setTitle("Hello!");
        // Wie jeder Wechsel der Ansicht über loadScene (Vollbild bleibt, Name gemerkt).
        loadScene("main-view.fxml");
        ping();
        watchDispenser();
        // Nach einem Update des Automaten bekommen die ESPs dieselbe Version.
        EspUpdateManager.start();
        // Nach einem Neustart hält die App den Alarm für aus - der Scanner soll
        // dann nicht mehr piepen, falls er vorher an war.
        new Thread(() -> {
            try {
                new CustomRequest("alarm_off", CustomRequest.REVIVER.SCANNER).execute();
            } catch (Exception e) {
                System.out.println("Alarm am Scanner konnte nicht ausgeschaltet werden: " + e.getMessage());
            }
        }, "alarm-off").start();
        System.out.println("Load done");
    }

    public void ping() {
        new Thread(() -> {
            try {
                System.out.println("Ping");
                new CustomRequest("ping", CustomRequest.REVIVER.WEBSITE).execute();
                new CustomRequest("ping", CustomRequest.REVIVER.SCANNER).execute();
                new CustomRequest("ping", CustomRequest.REVIVER.DISPENSER).execute();
                new CustomRequest("ping", CustomRequest.REVIVER.STATION).execute();
            } catch (Exception e) {
                showReconnect();
            }
        }).start();
    }

    /**
     * Zeigt den Bildschirm "Verbindung wiederherstellen" - von jedem Thread aus.
     * Bleibt auf dem HMAC-Bildschirm, den man von dort aus öffnet, um die
     * Verbindung einzurichten. Läuft gerade der Bildschirmschoner, läuft er weiter;
     * der Verbindungsbildschirm kommt, sobald ihn jemand beendet
     * (siehe {@link #sceneAfterSaver()}).
     */
    public void showReconnect() {
        Platform.runLater(() -> {
            if (stage == null || stage.getScene() == null) return;
            Object current = stage.getScene().getUserData();
            if ("screenSaver.fxml".equals(current) || getScreenSaver() != null && getScreenSaver().isSaverr()) {
                reconnectPending = true;
                return;
            }
            if ("hello-view.fxml".equals(current) || "hmac-view.fxml".equals(current)) return;
            loadScene("hello-view.fxml");
        });
    }

    /**
     * Wohin es nach dem Bildschirmschoner geht: zurück zu "Verbindung
     * wiederherstellen", wenn die Verbindung inzwischen weg ist oder dieser
     * Bildschirm vorher zu sehen war - sonst wie bisher zur Startansicht.
     */
    public String sceneAfterSaver() {
        boolean reconnect = reconnectPending || "hello-view.fxml".equals(sceneBeforeSaver);
        reconnectPending = false;
        sceneBeforeSaver = null;
        return reconnect ? "hello-view.fxml" : "main-view.fxml";
    }

    /**
     * Ohne Ausgabe kann der Automat nichts ausgeben - ist sie nicht erreichbar,
     * soll immer der Bildschirm "Verbindung wiederherstellen" zu sehen sein.
     * Prüft dazu alle paar Sekunden; erst zwei Fehlschläge hintereinander zählen,
     * damit ein einzelner verlorener Ping nicht gleich alles unterbricht.
     */
    private void watchDispenser() {
        if (dispenserWatchdog != null) return;
        dispenserWatchdog = new Thread(() -> {
            int misses = 0;
            while (true) {
                try {
                    Thread.sleep(DISPENSER_CHECK_MS);
                    // Scanner und Station nur anpingen: Die signierten Pings stellen ihre
                    // Uhr und sagen ihnen, wo der Automat ist - sie brauchen beides, um
                    // Karten signiert zu schicken (auch nach einem Neustart ohne NTP).
                    new CustomRequest("ping", CustomRequest.REVIVER.SCANNER).ping();
                    new CustomRequest("ping", CustomRequest.REVIVER.STATION).ping();
                    if (new CustomRequest("ping", CustomRequest.REVIVER.DISPENSER).ping()) {
                        misses = 0;
                        // Wieder da, bevor jemand den Bildschirmschoner beendet hat.
                        reconnectPending = false;
                    } else if (++misses >= 2) {
                        System.out.println("Ausgabe nicht erreichbar - zeige Verbindungsbildschirm");
                        showReconnect();
                    }
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "dispenser-watchdog");
        dispenserWatchdog.setDaemon(true);
        dispenserWatchdog.start();
    }

    /**
     * Refreshes all sweet buttons' text/images/disabled-state.
     * Runs on the JavaFX application thread via {@link Platform#runLater}, since this is
     * also called from background threads (the dispense worker thread and the
     * {@code /dispense/failed} Spring request thread).
     */
    public void kost() throws IOException {
        Platform.runLater(() -> {
            AvailabilityManager availabilityManager = new AvailabilityManager();
            for (int i = 0; i < MainController.getMainController().getBtns().length; i++) {
                Button btn = MainController.getMainController().getBtns()[i];

                btn.setContentDisplay(ContentDisplay.RIGHT);
                btn.setFont(new Font(40));
                btn.setText(ConfigurationManager.formatHours(configurationManager.getCost(i)) + ":");
                Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(getImage(i))));

                if (!isSellable(availabilityManager, i)) {
                    image = convertToGrayscale(image);
                    btn.setDisable(true);
                } else {
                    btn.setDisable(false);
                }

                ImageView imageView = new ImageView(image);
                imageView.setFitHeight(187);
                imageView.setFitWidth(200);
                btn.setPadding(Insets.EMPTY);
                btn.setBorder(Border.EMPTY);
                btn.setGraphic(imageView);
                if (i == 7) btn.setText("");
            }
        });
    }

    /**
     * Ob eine Süßigkeit gerade gewählt werden darf. Gesperrt ist sie, wenn
     * "Verfügbarkeit überprüfen" an ist und sie nicht verfügbar ist, oder - davon
     * unabhängig - wenn "Fächer automatisch sperren" an ist und ihr Vorrat
     * aufgebraucht oder das Fach nach einer fehlgeschlagenen Ausgabe gesperrt ist.
     */
    public boolean isSellable(int sweet) {
        return isSellable(new AvailabilityManager(), sweet);
    }

    private boolean isSellable(AvailabilityManager availabilityManager, int sweet) {
        if (checkAvailability && !availabilityManager.checkAvailability(sweet)) return false;
        return !autoLock || availabilityManager.isInStockAndEnabled(sweet);
    }

    private String getImage(int id) {
        if (id < 7 && id > -1) {
            return ("/image/" + configurationManager.getString("sweets._" + id + ".name") + "_" + id + ".png").toLowerCase();
        }
        return "/image/Logo.png";
    }

    private Image convertToGrayscale(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        WritableImage grayscaleImage = new WritableImage(width, height);
        PixelReader pixelReader = image.getPixelReader();
        PixelWriter pixelWriter = grayscaleImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = pixelReader.getArgb(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int grayArgb = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
                pixelWriter.setArgb(x, y, grayArgb);
            }
        }
        return grayscaleImage;
    }

    private void updateDisplay() {
        if (lastScan == null) {
            MainController.getMainController().setText("Bitte Karte scannen", Color.WHITE, true);
            return;
        }
        getDatabase().getSessionFactory().inTransaction(session -> {
            List<User> id = session.createSelectionQuery("from User u where u.rfid = :id", User.class)
                    .setParameter("id", lastScan).getResultList();
            if (id.isEmpty()) {
                MainController.getMainController().setText("Unbekannte Karte", Color.WHITE, true);
                return;
            }
            KontenManager konten = new KontenManager(lastScan);
            String hours = konten.hasUnlimitedHours() ? " ∞" : konten.getKonto().getBalanceRounded() + "h";
            MainController.getMainController().setText(id.getFirst().getFullName() + ":" + hours, Color.WHITE, true);
        });
    }

    public int[] getLastScan() {
        return lastScan;
    }

    public void setLastScan(int[] lastScan) {
        if ("add-user-view.fxml".equals(Main.getInstance().getStage().getScene().getUserData())) {
            try {
                Konto konto = new KontenManager(lastScan).getKonto();
                AddUserController.showKnownCard(konto.getUser().getFullName());
            } catch (Exception e) {
                // Während die Bestätigung steht, ist der nächste Nutzer noch nicht
                // zu sehen - die Karte würde also jemandem Unbekanntem zugeordnet.
                if (AddUserController.isConfirming()) return;
                AddUserHandler.UserAdd peek = AddUserHandler.addQueue.poll();
                if (peek == null) return;

                Main.getInstance().getDatabase().getSessionFactory().inTransaction(session ->
                        AddUserHandler.create(peek, lastScan, session));
                AddUserController.confirmAdded(peek.getVorname() + " " + peek.getNachname());
            }
            return;
        }
        // Erst merken, dann anzeigen - updateDisplay() liest this.lastScan. In der
        // alten Reihenfolge zeigte der erste Scan nichts und der zweite den Namen.
        this.lastScan = lastScan;
        updateDisplay();
        if (lastScan == null) return;
        new Thread(() -> {
            try {
                Thread.sleep(1000L * logoutTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (lastScan == this.lastScan) this.lastScan = null;
            updateDisplay();
        }).start();
    }

    public String getUrl(CustomRequest.REVIVER reviver) {
        if (useLocalEndpoints) {
            return switch (reviver) {
                case WEBSITE -> "http://localhost:8080";
                case DISPENSER -> "http://localhost:8081";
                case SCANNER -> "http://localhost:8082";
                case STATION -> "http://localhost:8083";
            };
        }
        return switch (reviver) {
            case WEBSITE -> configurationManager.getString("website-url");
            case DISPENSER -> configurationManager.getString("dispenser-url");
            case SCANNER -> configurationManager.getString("scanner-url");
            case STATION -> configurationManager.getString("station-url");
        };
    }

    public boolean isUseLocalEndpoints() {
        return useLocalEndpoints;
    }

    public void setUseLocalEndpoints(boolean useLocalEndpoints) {
        this.useLocalEndpoints = useLocalEndpoints;
    }

    public Stage getStage() {
        return stage;
    }

    public Dimension2D getDimension() {
        return dimension;
    }

    public Server getServer() {
        return server;
    }

    public int getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(int logoutTime) {
        settingsManager.setSetting("logout", String.valueOf(logoutTime));
        this.logoutTime = logoutTime;
    }

    public boolean isAlarm() {
        return alarm;
    }

    public void setAlarm(boolean alarm) {
        this.alarm = alarm;
    }

    public boolean isCheckAvailability() {
        return checkAvailability;
    }

    public void setCheckAvailability(boolean checkAvailability) {
        this.checkAvailability = checkAvailability;
        settingsManager.setSetting("availability", String.valueOf(checkAvailability));
    }

    public boolean isAlarmEnabled() {
        return alarmEnabled;
    }

    public void setAlarmEnabled(boolean alarmEnabled) {
        this.alarmEnabled = alarmEnabled;
        settingsManager.setSetting("alarmEnabled", String.valueOf(alarmEnabled));
    }

    public boolean isAutoLock() {
        return autoLock;
    }

    public void setAutoLock(boolean autoLock) {
        this.autoLock = autoLock;
        settingsManager.setSetting("autoLock", String.valueOf(autoLock));
    }

    public ScreenSaver getScreenSaver() {
        return screenSaver;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public SettingsManager getSettings() {
        return settingsManager;
    }

    public StatisticManager getHandler() {
        return handler;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}