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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class Main extends Application {

    private static Main instance;
    private String url = "http://127.0.0.1:5000";
    private Stage stage;
    private Dimension2D dimension;
    private Server server;
    private ScannedCard lastScan;
    private Statistic statistic;

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
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        load();
        setKost();
    }

    public void loadScene(String sceneName) {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(sceneName));
        Scene scene = null;
        try {
            scene = new Scene(fxmlLoader.load(), Main.getInstance().getDimension().getWidth(), Main.getInstance().getDimension().getHeight());
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
        stage.setScene(scene);
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
        if (json == null) {return;}
        JSONObject jsonObject = new JSONObject(json);
        for (int i = 0; i < MainController.getMainController().getBtns().length; i++) {
            Button btn = (Button) MainController.getMainController().getBtns()[i];
            JSONObject subObj = jsonObject.getJSONObject(String.valueOf(i));
            System.out.println(subObj);
            System.out.println(i);
            btn.setText("Kosten(" + subObj.getString("name") + "):" + subObj.getInt("hours"));
        }
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
        if (lastScan == null) return;
        MainController.getMainController().getText().setText(lastScan.name + ":" + lastScan.time.getHour() + "h");
        new Thread(() -> {
            try {
                Thread.sleep(1000*10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            MainController.getMainController().getText().setText("Bitte Karte Scannen");
            System.out.println(this.lastScan.toString());
            this.lastScan = null;
        }).start();
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
}