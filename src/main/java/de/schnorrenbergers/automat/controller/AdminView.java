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
        Main.getInstance().loadScene("main-view.fxml");
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
