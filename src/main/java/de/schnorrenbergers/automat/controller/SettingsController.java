package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Slider;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        slider_logout.valueProperty().addListener((observable, oldValue, newValue) -> {
            display();
        });
        slider_logout.setValue(Main.getInstance().getLogoutTime());
        display();
    }

    @FXML
    public Slider slider_logout;
    @FXML
    public Text display;

    public void fullscreen(ActionEvent actionEvent) {
        Main.getInstance().getStage().setFullScreen(!Main.getInstance().getStage().isFullScreen());
    }

    public void exit(ActionEvent actionEvent) {
        System.exit(0);
    }

    public void back(ActionEvent actionEvent) {
        Main.getInstance().loadScene("main-view.fxml");
    }

    public void cancel(ActionEvent actionEvent) {
        slider_logout.setValue(Main.getInstance().getLogoutTime());
    }

    public void Ok(ActionEvent actionEvent) {
        Main.getInstance().setLogoutTime(((int) slider_logout.getValue()));
        display();
        try {
            Main.getInstance().getStatistic().save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void display() {
        display.setText(Main.getInstance().getLogoutTime() + "→" + ((int) slider_logout.valueProperty().get()));
    }

    public void resetStats(ActionEvent actionEvent) {
        Main.getInstance().getStatistic().resetStats();
    }
}
