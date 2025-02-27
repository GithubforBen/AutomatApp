package de.schnorrenbergers.automat;

import atlantafx.base.theme.PrimerDark;
import de.schnorrenbergers.automat.controller.MainController;
import de.schnorrenbergers.automat.server.Server;
import de.schnorrenbergers.automat.statistic.Statistic;
import de.schnorrenbergers.automat.types.CustomRequest;
import de.schnorrenbergers.automat.types.ScannedCard;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {

    private static Main instance;
    private String url = "http://127.0.0.1:5000";
    private Stage stage;
    private Dimension2D dimension;
    private Server server;
    private ScannedCard lastScan;
    private Statistic statistic;
    private int logoutTime = 10;

    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        dimension = new Dimension2D(480, 800);
        instance = this;
        if (server == null) server = new Server();
        statistic = new Statistic();
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), dimension.getWidth(), dimension.getHeight());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
        this.stage = stage;
        stage.setFullScreenExitHint("");
        //Font.loadFont(Main.class.getResource("/fonts/Russo_One.ttf").toExternalForm(), 10);
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        load();
        new CustomRequest("ping").execute();
        logoutTime = ((Integer) statistic.getSetting("logout"));
    }

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

    public void load() throws IOException, InterruptedException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), dimension.getWidth(), dimension.getHeight());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public void setKost() throws IOException {
        //TODO: actual implementation
        String json = new CustomRequest("sweets").execute();
        if (json == null) {
            return;
        }
        JSONObject jsonObject = new JSONObject(json);
        for (int i = 0; i < MainController.getMainController().getBtns().length; i++) {
            Button btn = (Button) MainController.getMainController().getBtns()[i];
            JSONObject subObj = jsonObject.getJSONObject(String.valueOf(i));
            System.out.println(subObj);
            System.out.println(i);

            btn.setContentDisplay(ContentDisplay.RIGHT);
            btn.setFont(new Font(40));
            btn.setText(subObj.getInt("hours") + ":");
            ImageView imageView = new ImageView(getImage(i));
            imageView.setFitHeight(187);
            imageView.setFitWidth(200);
            btn.setPadding(Insets.EMPTY);
            btn.setBorder(Border.EMPTY);
            btn.setGraphic(imageView);
            if (i == 7) btn.setText("");
        }
    }

    private Image getImage(int id) {
        switch (id) {
            case 0 -> {
                return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/mentos_0.png")));
            }
            case 1 -> {
                return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/duplo_1.png")));
            }
            case 2 -> {
                return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/kinder_2.png")));
            }
            case 3 -> {
                return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/mauam_3.png")));
            }
            case 4 -> {
                return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/smarties_4.png")));
            }
            case 5 -> {
                return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/haribo_5.png")));
            }
            case 6 -> {
                return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/brause_6.png")));
            }
        }
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/Logo.png")));
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

    public void setLastScan(ScannedCard lastScan) {
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

    private void updateDisplay() {
        if (lastScan == null) {
            MainController.getMainController().getText().setText("Bitte Karte Scannen");
            return;
        }
        MainController.getMainController().getText().setText(lastScan.name + ":" + lastScan.time.getHour() + "h");
    }

    public Server getServer() {
        return server;
    }

    public ScannedCard getLastScan() {
        return lastScan;
    }

    public Statistic getStatistic() {
        return statistic;
    }

    public int getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(int logoutTime) {
        this.logoutTime = logoutTime;
    }
}