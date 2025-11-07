package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.text.Text;
import org.apache.commons.lang3.ArrayUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;

public class PinController implements Initializable {

    private int[] last;
    private final int[] code = new int[]{1, 2, 3, 4, 5};
    private final int[] settings = new int[]{3, 3, 3, 3, 3};

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

        Main.getInstance().loadScene("main-view.fxml");
    }

    public void btn_0(ActionEvent actionEvent) {
        press(0);
    }

    public void btn_next(ActionEvent actionEvent) {
        ArrayUtils.reverse(last);
        String sceneId = "";
        if (Objects.deepEquals(last, code)) {
            text.setText("Korrekt!");
            sceneId = "admin-view.fxml";
        } else if (Objects.deepEquals(last, settings)) {
            text.setText("Korrekt!");
            sceneId = "settings-view.fxml";
        } else {
            text.setText("_ ".repeat(code.length));
            Arrays.fill(last, -1);
            sceneId = "stats-view.fxml";
        }
        Main.getInstance().loadScene(sceneId);
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (code.length != settings.length) {
            throw new RuntimeException("Code and last have different lengths");
        }
        last = new int[code.length];
        text.setText("_ ".repeat(code.length));
        Arrays.fill(last, -1);
    }

    public void press(int press) {
        if (press >= 0 && press <= 9) {
            int[] temp = last.clone();
            temp[0] = press;
            if (last.length - 1 >= 0) System.arraycopy(last, 0, temp, 1, last.length - 1);
            last = temp;
        }
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        display();
    }

    public void display() {
        StringBuilder s = new StringBuilder();
        for (int i : last) {
            if (i == -1) {
                s.append("_ ");
            } else {
                s.append("* ");
            }
        }
        text.setText(s.toString());
    }
}
