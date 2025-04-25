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

    public void back(ActionEvent actionEvent) {
        Main.getInstance().loadScene("main-view.fxml");
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

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
