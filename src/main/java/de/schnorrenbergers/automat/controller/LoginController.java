package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.Database;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private final int passwordLenght = 10;
    @FXML
    public Label text;
    List<Integer> input = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }


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

    public void btn_delete(ActionEvent actionEvent) {
        input.removeLast();
        display();
    }

    public void btn_0(ActionEvent actionEvent) {
        press(0);
    }

    public void btn_next(ActionEvent actionEvent) {
        StringBuilder sb = new StringBuilder();
        input.forEach(sb::append);
        String userPW = sb.substring(passwordLenght / 2);
        String filePW = sb.toString().replace(userPW, "");
        try {
            // Swap userPW and filePW to match the expected order in Database constructor
            Main.getInstance().setDatabase(new Database("MiNt-ZeNtRuM", filePW, userPW));
            Main.getInstance().initialise();
            Main.getInstance().checkForStuff();
            Main.getInstance().load();
        } catch (Exception e) {
            e.printStackTrace();
            text.setText("Falsches Passwort");
        }
    }

    public void press(int press) {
        if (press >= 0 && press <= 9) {
            input.add(press);
        }
        display();
    }

    public void display() {
        String s = "";
        for (int i = 0; i < passwordLenght; i++) {
            if (input.size() < i + 1) {
                s = s + "_ ";
            } else {
                s = s + "* ";
            }
        }
        setText(s, Color.WHITE, true);
    }

    public void setText(String s, Paint paint, boolean first) {
        Platform.runLater(() -> {
            if (s != null) {
                text.setAlignment(javafx.geometry.Pos.CENTER);
                text.setText("");
                text.setText(s);
                double fontSize = text.getFont().getSize();
                text.widthProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.doubleValue() > 480) {
                        double fontSizee = text.getFont().getSize() - 0.5;
                        text.setFont(new Font(fontSizee));
                        setText(s, paint, false);
                    }
                });
                if (first) fontSize = 49;
                text.setFont(new Font(fontSize));
            }
            if (paint != null) text.setTextFill(paint);
        });
    }
}
