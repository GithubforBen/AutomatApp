package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    @FXML
    public Button shut;

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
        Main.getInstance().getStage().show();
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

    public void shutdown(ActionEvent actionEvent) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (shut.getText().equals("Herunterfahren")) {
            processBuilder.command("shutdown");
            new Thread(() -> {
                try {
                    Thread.sleep(1000 * 30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (!shut.getText().equals("Herunterfahren")) System.exit(0);
            }).start();
            shut.setText("Herunterfahren\n Abbrechen");
        } else {
            processBuilder.command("shutdown", "-c");
            shut.setText("Herunterfahren");
        }
        try {
            processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
