package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.types.CustomRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminView {
    @FXML
    private Slider slider;
    private boolean alarm = false;

    public void back(ActionEvent actionEvent) {
        Stage stage = ((Stage) ((Button) actionEvent.getSource()).getScene().getWindow());
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
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

    public void alarm(ActionEvent actionEvent) {
        if (alarm) {
            new CustomRequest("alarm_on");
        } else  {
            new CustomRequest("alarm_off");
        }
        alarm = !alarm;
    }
}
