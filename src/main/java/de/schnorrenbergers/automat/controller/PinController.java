package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.IntStream;

public class PinController implements Initializable {

    private int[] last;
    private final int[] code = new int[]{1,2,3,4};

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
            sceneId="admin-view.fxml";
        } else {
            text.setText("_ _ _ _");
            System.out.println(Arrays.toString(last) + "!=" + Arrays.toString(code));
            Arrays.fill(last, -1);
            sceneId="stats-view.fxml";
        }
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(sceneId));
        Scene scene = null;
        try {
            scene = new Scene(fxmlLoader.load(), Main.getInstance().getDimension().getWidth(), Main.getInstance().getDimension().getHeight());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Main.getInstance().getStage().setScene(scene);
        Main.getInstance().getStage().show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        text.setText("_ _ _ _");
        last = new int[4];
        Arrays.fill(last, -1);
    }

    public void press(int press) {
        if (press >= 0 && press <= 9) {
            int[] temp = last.clone();
            temp[0] = press;
            if (last.length - 1 >= 0) System.arraycopy(last, 0, temp, 1, last.length - 1);
            last = temp;
        }
        display();
    }

    public void display() {
        String s ="";
        for (int i : last) {
            if (i == -1) {
                s = s + "_ ";
            } else {
                s = s+ "* ";
            }
        }
        text.setText(s);
    }
}
