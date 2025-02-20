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
import java.util.Arrays;
import java.util.ResourceBundle;

public class PinController implements Initializable {

    private int[] last;
    private int[] code = new int[]{1,2,3,4};

    @FXML
    private Text text;

    public void btn_1(ActionEvent actionEvent) {
        press(1);
    }

    public void btn_2(ActionEvent actionEvent) {
        press(2);
    }

    public void btn_3(ActionEvent actionEvent) {
        press(3);
    }

    public void btn_4(ActionEvent actionEvent) {
        press(4);
    }

    public void btn_5(ActionEvent actionEvent) {
        press(5);
    }

    public void btn_6(ActionEvent actionEvent) {
        press(6);
    }

    public void btn_7(ActionEvent actionEvent) {
        press(7);
    }

    public void btn_8(ActionEvent actionEvent) {
        press(8);
    }

    public void btn_9(ActionEvent actionEvent) {
        press(9);
    }

    public void btn_back(ActionEvent actionEvent) {//Loads pin scene
    Stage stage = ((Stage) ((Button) actionEvent.getSource()).getScene().getWindow());
    FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
    Scene scene = null;
        try {
        scene = new Scene(fxmlLoader.load(), 600, 400);
    } catch (
    IOException e) {
        throw new RuntimeException(e);
    }
        stage.setScene(scene);
        stage.show();
}

    public void btn_0(ActionEvent actionEvent) {
    }

    public void btn_next(ActionEvent actionEvent) {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        text.setText("_ _ _ _");
        last = new int[4];
    }

    public void press(int press) {
        if (press >= 0 && press <= 9) {
            int[] temp = last.clone();
            temp[0] = press;
            for (int i = 0; i < last.length-1; i++) {
                temp[i +1 ] = last[i];
            }
            last = temp;
            System.out.println(Arrays.toString(last));
        }
    }
}
