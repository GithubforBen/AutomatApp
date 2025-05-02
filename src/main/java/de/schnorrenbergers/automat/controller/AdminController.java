package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.types.CustomRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminController implements Initializable {
    @FXML
    public Button alarmBTN;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAlarmBTNColor();
    }

    /**
     * Sets the color of the alarm button depending on the alarm status
     */
    private void setAlarmBTNColor() {
        boolean alarm = Main.getInstance().isAlarm();
        if (alarm) {
            alarmBTN.setTextFill(Color.GREEN);
        } else {
            alarmBTN.setTextFill(Color.RED);
        }
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    @FXML
    public Button plus;
    @FXML
    private Slider slider;
    private int positive = 3; //pos->0;neg->1;re-enable->2

    /**
     * Handles the navigation back to the main view by loading the "main-view.fxml" scene.
     * This method also updates the timestamp of the last user interaction in the screen saver.
     *
     * @param actionEvent the event that triggered this method, typically a button press
     */
    public void back(ActionEvent actionEvent) {
        Main.getInstance().loadScene("main-view.fxml");
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    /**
     * Toggles the alarm state in the application. If the alarm is currently active,
     * it sends a request to turn it off, and vice versa. Updates the visual
     * representation of the alarm button and resets the screen saver timeout.
     *
     * @param actionEvent the event that triggered this method, typically a button press
     */
    public void alarm(ActionEvent actionEvent) {
        boolean alarm = Main.getInstance().isAlarm();
        try {
            if (alarm) {
                new CustomRequest("alarm_on").execute();
            } else {
                new CustomRequest("alarm_off").execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Main.getInstance().setAlarm(!alarm);
        setAlarmBTNColor();
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void mentos(ActionEvent actionEvent) {
        fill("mentos");
    }

    public void haribo(ActionEvent actionEvent) {
        fill("haribo");
    }

    public void brause(ActionEvent actionEvent) {
        fill("brause");
    }

    public void smarties(ActionEvent actionEvent) {
        fill("smarties");
    }

    public void maoam(ActionEvent actionEvent) {
        fill("maoam");
    }

    /**
     * Performs a fill operation based on the specified parameters
     * and the current state of the system.
     * Depending on the value of the `positive` field, this method
     * either fills a specific amount, re-enables the related entity,
     * or performs no operation.
     *
     * @param name the name of the item or entity to be filled or re-enabled
     * @throws RuntimeException if an I/O error occurs during the operation
     */
    public void fill(String name) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        try {
            if (positive == 0)
                new CustomRequest("fill").executeComplex("{\"name\":\"" + name + "\",\"nr\":" + slider.getValue() + "}");
            if (positive == 1)
                new CustomRequest("fill").executeComplex("{\"name\":\"" + name + "\",\"nr\":" + slider.getValue() * -1 + "}");
            if (positive == 2)
                new CustomRequest("re-enable").executeComplex("{\"name\":\"" + name + "\"}");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles the toggling of a button's state and its associated text representation.
     * This method updates the screen saver's last interaction timestamp and cycles through three states:
     * "+" when positive is 0, "-" when positive is 1, and "Reaktivieren" when positive is reset to 0.
     *
     * @param actionEvent the event that triggered this method, typically a button press
     */
    public void plus(ActionEvent actionEvent) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        if (positive == 0) {
            plus.setText("+");
            positive += 1;
        } else if (positive == 1) {
            plus.setText("-");
            positive += 1;
        } else {
            positive = 0;
            plus.setText("Reaktivieren");
        }
    }

    public void duplo(ActionEvent actionEvent) {
        fill("duplo");
    }

    public void kinder(ActionEvent actionEvent) {
        fill("kinder");
    }
}
