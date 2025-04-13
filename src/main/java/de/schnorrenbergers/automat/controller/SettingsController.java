package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    @FXML
    public Button shut;
    @FXML
    public CheckBox availability;
    @FXML
    public CheckBox checkTime;
    @FXML
    public CheckBox screensaver;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        slider_logout.valueProperty().addListener((observable, oldValue, newValue) -> {
            display();
        });
        slider_logout.setValue(Main.getInstance().getLogoutTime());
        display();
        availability.setSelected(Main.getInstance().isCheckAvailability());
        availability.setOnAction((ActionEvent event) -> {
            Main.getInstance().setCheckAvailability(availability.isSelected());
            //TODO: use new Statistic instead of Main.getInstance().getStatistic().save();
        });
        checkTime.setSelected(Boolean.parseBoolean(Main.getInstance().getSettings().getSettingOrDefault("checkTime", "true")));
        checkTime.setOnAction((ActionEvent event) -> {
            Main.getInstance().getSettings().setSetting("checkTime", String.valueOf(checkTime.isSelected()));
            //TODO: use new Statistic instead of Main.getInstance().getStatistic().save();
        });
        screensaver.setSelected(Main.getInstance().getScreenSaver().isDoSaver());
        screensaver.setOnAction((ActionEvent event) -> {
            Main.getInstance().getScreenSaver().setDoSaver(screensaver.isSelected());
            //TODO: use new Statistic instead of Main.getInstance().getStatistic().save();
        });
    }

    @FXML
    public Slider slider_logout;
    @FXML
    public Text display;

    public void fullscreen(ActionEvent actionEvent) {
        Main.getInstance().getStage().setFullScreen(!Main.getInstance().getStage().isFullScreen());
        Main.getInstance().getStage().show();
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void exit(ActionEvent actionEvent) {
        //TODO: use new Statistic instead of Main.getInstance().getStatistic().save();
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        System.exit(0);
    }

    public void back(ActionEvent actionEvent) {
        Main.getInstance().loadScene("main-view.fxml");
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void cancel(ActionEvent actionEvent) {
        slider_logout.setValue(Main.getInstance().getLogoutTime());
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void Ok(ActionEvent actionEvent) {
        Main.getInstance().setLogoutTime(((int) slider_logout.getValue()));
        display();
        //TODO: use new Statistic instead of Main.getInstance().getStatistic().save();
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void display() {
        display.setText(Main.getInstance().getLogoutTime() + "→" + ((int) slider_logout.valueProperty().get()));
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void resetStats(ActionEvent actionEvent) {
        //TODO: use new Statistic instead of Main.getInstance().getStatistic().resetStats();
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void shutdown(ActionEvent actionEvent) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        //TODO: use new Statistic instead of Main.getInstance().getStatistic().save();
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
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }
}
