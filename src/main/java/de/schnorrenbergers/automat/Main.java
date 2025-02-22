package de.schnorrenbergers.automat;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import de.schnorrenbergers.automat.controller.MainController;
import de.schnorrenbergers.automat.types.CustomRequest;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.json.JSONObject;

import java.io.IOException;

public class Main extends Application {

    private static Main instance;
    private String url = "http://127.0.0.1:5000/";
    private Stage stage;
    private Dimension2D dimension;

    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        dimension = new Dimension2D(480, 800);
        instance = this;
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), dimension.getWidth(), dimension.getHeight());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
        setKost();
        this.stage = stage;
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        load();
    }

    public void load() throws IOException, InterruptedException {

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), dimension.getWidth(), dimension.getHeight());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public void setKost() throws IOException {
        return;
        /*
        //TODO: actual implementation
        String json = new CustomRequest("sweets").execute();
        JSONObject jsonObject = new JSONObject(json);
        for (int i = 0; i < MainController.getMainController().getBtns().length; i++) {
            Button btn = (Button) MainController.getMainController().getBtns()[i];
            JSONObject subObj = jsonObject.getJSONObject(String.valueOf(i));
            System.out.println(subObj);
            System.out.println(i);
            btn.setText("Kosten(" + subObj.getString("name") + "):" + subObj.getInt("hours"));
        }

         */
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
}