package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private static MainController mainController;
    @FXML
    private Button btn_1;
    @FXML
    private Button btn_2;
    @FXML
    private Button btn_3;
    @FXML
    private Button btn_4;
    @FXML
    private Button btn_5;
    @FXML
    private Button btn_6;
    @FXML
    private Button btn_7;
    @FXML
    private Button btn_8;
    @FXML
    private Text text;

    public void button1(ActionEvent actionEvent) {

    }

    public void button2(ActionEvent actionEvent) {

    }

    public void button3(ActionEvent actionEvent) {

    }

    public void button4(ActionEvent actionEvent) {

    }

    public void button5(ActionEvent actionEvent) {

    }

    public void button6(ActionEvent actionEvent) {

    }

    public void button7(ActionEvent actionEvent) {

    }

    public void button8(ActionEvent actionEvent) {//Loads pin scene
        Stage stage = ((Stage) MainController.getMainController().getBtns()[0].getScene().getWindow());
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("pin-view.fxml"));
        Scene scene = null;
        try {
            scene = new Scene(fxmlLoader.load(), 600, 400);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        text.setText("Bitte Karte scannen!");
        MainController.mainController = this;
    }

    public static MainController getMainController() {
        return mainController;
    }

    public Button[] getBtns() {
        return new Button[]{btn_1, btn_2, btn_3, btn_4, btn_5, btn_6, btn_7, btn_8};
    }
}
