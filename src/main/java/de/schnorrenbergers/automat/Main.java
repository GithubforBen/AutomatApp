package de.schnorrenbergers.automat;

import atlantafx.base.theme.PrimerDark;
import de.schnorrenbergers.automat.controller.MainController;
import de.schnorrenbergers.automat.database.Database;
import de.schnorrenbergers.automat.database.types.Kurs;
import de.schnorrenbergers.automat.database.types.Student;
import de.schnorrenbergers.automat.database.types.Teacher;
import de.schnorrenbergers.automat.database.types.User;
import de.schnorrenbergers.automat.database.types.types.Day;
import de.schnorrenbergers.automat.database.types.types.Gender;
import de.schnorrenbergers.automat.database.types.types.Level;
import de.schnorrenbergers.automat.database.types.types.Wohnort;
import de.schnorrenbergers.automat.manager.*;
import de.schnorrenbergers.automat.server.Server;
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

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class Main extends Application {
    private static Main instance;
    private String url;
    private Stage stage;
    private Dimension2D dimension;
    private Server server;
    private int[] lastScan;
    private int logoutTime;
    private boolean alarm = false;
    private boolean checkAvailability;
    private ScreenSaver screenSaver;
    private Database database;
    private SettingsManager settingsManager;
    private StatisticManager handler;
    private ConfigurationManager configurationManager;

    /**
     * Used to Initialize all objects.
     *
     * @throws IOException            Programm wont work.
     * @throws SQLException           Programm wont work.
     * @throws ClassNotFoundException Programm wont work.
     */
    public void initialise() throws Exception {
        System.out.println("Initialize");
        url = configurationManager.getString("frontend-url");
        logoutTime = configurationManager.getInt("default-logout-time");
        settingsManager = new SettingsManager();
        screenSaver = new ScreenSaver();
        handler = new StatisticManager();
        logoutTime = Integer.parseInt(settingsManager.getSettingOrDefault("logout", String.valueOf(logoutTime)));
        checkAvailability = Boolean.parseBoolean(settingsManager.getSettingOrDefault("availability", String.valueOf(false)));
        database.getSessionFactory().inTransaction((x) -> {
            if (1 == 1) return;
            Wohnort wohnortT = new Wohnort(1, "s", "s", 456, "dsa");
            Wohnort wohnortS = new Wohnort(1, "s", "s", 4456, "dsa");
            try {
                Teacher teacher = new Teacher("Test", "TEzjk", new int[]{0, 0, 0, 0, 0}, Gender.DUAL_GENDER, new Date(System.currentTimeMillis()), wohnortT, "test@gmail.com", "123", Level.ADMIN);
                Kurs kurs = new Kurs("Kurs", List.of(teacher), Day.DÖNNERSTAG);
                Student student = new Student("David", "Junke", new int[]{99, 253, 101, 0, 251}, Gender.OTHER, new Date(System.currentTimeMillis()), wohnortS, List.of(kurs));
                x.persist(wohnortT);
                x.persist(wohnortS);
                x.persist(teacher);
                x.persist(kurs);
                x.persist(student);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (server == null) server = new Server();
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

    public void startWithouthPassword(Stage stage) throws Exception {
        instance = this;
        configurationManager = new ConfigurationManager();
        dimension = new Dimension2D(
                configurationManager.getInt("window-dimension.horizontal"),
                configurationManager.getInt("window-dimension.vertical"));
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), dimension.getWidth(), dimension.getHeight());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
        this.stage = stage;
        stage.setFullScreenExitHint("");
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        stage.setAlwaysOnTop(true);
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
        new CustomRequest("ping").execute();
        new CustomRequest("alarm_off").execute();
    }

    public void kost() throws IOException {
        for (int i = 0; i < MainController.getMainController().getBtns().length; i++) {
            Button btn = MainController.getMainController().getBtns()[i];

            btn.setContentDisplay(ContentDisplay.RIGHT);
            btn.setFont(new Font(40));
            btn.setText(configurationManager.getInt("sweets._" + i + ".kost") + ":");
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(getImage(i))));

            if (checkAvailability && !new AvailabilityManager().checkAvailability(i)) {
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
            MainController.getMainController().setText(id.getFirst().getFullName() + ":" + new KontenManager(lastScan).getKonto().getBalanceRounded() + "h", Color.WHITE, true);
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
            if (lastScan == this.lastScan) this.lastScan = null;
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

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }
}