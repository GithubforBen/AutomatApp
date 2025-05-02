package de.schnorrenbergers.automat;

import atlantafx.base.theme.PrimerDark;
import de.schnorrenbergers.automat.controller.MainController;
import de.schnorrenbergers.automat.database.Database;
import de.schnorrenbergers.automat.database.types.Konto;
import de.schnorrenbergers.automat.database.types.User;
import de.schnorrenbergers.automat.manager.KontenManager;
import de.schnorrenbergers.automat.manager.SettingsManager;
import de.schnorrenbergers.automat.manager.StatisticManager;
import de.schnorrenbergers.automat.server.Server;
import de.schnorrenbergers.automat.types.CustomRequest;
import de.schnorrenbergers.automat.types.ScreenSaver;
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
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Main extends Application {
    //TODO: login
    private static Main instance;
    private final String url = "http://127.0.0.1:5000";
    private Stage stage;
    private Dimension2D dimension;
    private Server server;
    private int[] lastScan;
    private int logoutTime = 10;
    private boolean alarm = false;
    private boolean checkAvailability;
    private ScreenSaver screenSaver;
    private Database database;
    private SettingsManager settingsManager;
    private StatisticManager handler;

    /**
     * Used to Initialize all objects.
     *
     * @throws IOException            Programm wont work.
     * @throws SQLException           Programm wont work.
     * @throws ClassNotFoundException Programm wont work.
     */
    private void initialise() throws IOException, SQLException, ClassNotFoundException {
        instance = this;
        database = new Database();
        settingsManager = new SettingsManager();
        dimension = new Dimension2D(480, 800);
        if (server == null) server = new Server();
        screenSaver = new ScreenSaver();
        handler = new StatisticManager();
        logoutTime = Integer.parseInt(settingsManager.getSettingOrDefault("logout", String.valueOf(logoutTime)));
        checkAvailability = Boolean.parseBoolean(settingsManager.getSettingOrDefault("availability", String.valueOf(false)));/*
        database.getSessionFactory().inTransaction(session -> {
            session.createSelectionQuery("from User u", User.class).getResultList().forEach((x) -> {
                System.out.println(x.toString());
            });
        });

        database.getSessionFactory().inTransaction(session -> {
            Kurs kurs = new Kurs("Test", "Paul", Day.DONNERSTAG);
            session.persist(kurs);
            Wohnort wohnort = new Wohnort(1, " ", "sad", 48, "germany");
            session.persist(wohnort);
            session.persist(new User("Ben", "Schnorrenberger", new int[]{0,1,2,3}, Gender.DUAL_GENDER, 10, wohnort, new Kurs[]{kurs}));
        });

 */
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
    public void start(Stage stage) throws IOException, InterruptedException, SQLException, ClassNotFoundException {
        initialise();
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), dimension.getWidth(), dimension.getHeight());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
        this.stage = stage;
        stage.setFullScreenExitHint("");
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        checkForStuff();
        load();
        new CustomRequest("ping").execute();
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
                        Runnable runnable = () -> loadScene("screenSaver.fxml");
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
        stage.setFullScreen(fullScreen);
        stage.setScene(scene);
        stage.setFullScreen(fullScreen);
        stage.show();
    }

    public void load() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), dimension.getWidth(), dimension.getHeight());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
        new CustomRequest("alarm_off").execute();
    }

    public void setKost() throws IOException {
        if (checkAvailability) {
            kost("sweetsDa");
            return;
        }
        kost("sweets");
    }

    private void kost(String url) throws IOException {
        String json = new CustomRequest(url).execute();
        if (json == null) {
            return;
        }
        JSONObject jsonObject = new JSONObject(json);
        for (int i = 0; i < MainController.getMainController().getBtns().length; i++) {
            Button btn = MainController.getMainController().getBtns()[i];
            JSONObject subObj = jsonObject.getJSONObject(String.valueOf(i));

            btn.setContentDisplay(ContentDisplay.RIGHT);
            btn.setFont(new Font(40));
            btn.setText(subObj.getInt("hours") + ":");
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(getImage(i))));
            if (checkAvailability && !subObj.getBoolean("available")) {
                image = convertToGrayscale(image);
                btn.setDisable(true);
            }
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(187);
            imageView.setFitWidth(200);
            btn.setPadding(Insets.EMPTY);
            btn.setBorder(Border.EMPTY);
            btn.setGraphic(imageView);
            if (i == 7) btn.setText("");
        }
    }

    private String getImage(int id) {
        return switch (id) {
            case 0 -> "/image/mentos_0.png";
            case 1 -> "/image/duplo_1.png";
            case 2 -> "/image/kinder_2.png";
            case 3 -> "/image/mauam_3.png";
            case 4 -> "/image/smarties_4.png";
            case 5 -> "/image/haribo_5.png";
            case 6 -> "/image/brause_6.png";
            default -> "/image/Logo.png";
        };
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
            MainController.getMainController().setText("Bitte Karte Scannen", Color.WHITE, true);
            return;
        }
        getDatabase().getSessionFactory().inTransaction(session -> {
            List<User> id = session.createSelectionQuery("from User u where u.rfid = :id", User.class)
                    .setParameter("id", lastScan).getResultList();
            if (id.isEmpty()) {
                MainController.getMainController().setText("Unbekannte Karte", Color.WHITE, true);
                return;
            }
            List<Konto> konten = session.createSelectionQuery("from Konto k where k.userId = :id", Konto.class)
                    .setParameter("id", id.getFirst().getId()).getResultList();
            if (konten.isEmpty()) {
            }
            MainController.getMainController().setText(id.getFirst().getFullName() + ":" + new KontenManager(id.getFirst().getId()).getKonto().getBalance(TimeUnit.HOURS) + "h", Color.WHITE, true);
        });
    }

    public void setCheckAvailability(boolean checkAvailability) {
        this.checkAvailability = checkAvailability;
        settingsManager.setSetting("availability", String.valueOf(checkAvailability));
    }

    public int[] getLastScan() {
        return lastScan;
    }

    public static void main(String[] args) {
        launch();
    }

    public static Main getInstance() {
        return instance;
    }

    public String getUrl() {
        return url;
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

    public void setLastScan(int[] lastScan) {
        this.lastScan = lastScan;
        updateDisplay();
        if (lastScan == null) return;
        new Thread(() -> {
            try {
                Thread.sleep(1000L * logoutTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            this.lastScan = null;
            updateDisplay();
        }).start();
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

    public ScreenSaver getScreenSaver() {
        return screenSaver;
    }

    public Database getDatabase() {
        return database;
    }

    public SettingsManager getSettings() {
        return settingsManager;
    }

    public StatisticManager getHandler() {
        return handler;
    }
}