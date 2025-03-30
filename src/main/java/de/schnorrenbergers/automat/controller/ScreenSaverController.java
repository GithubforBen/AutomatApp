package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;

import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

public class ScreenSaverController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new Thread(() -> {
            try {
                while (true) {
                    while (Main.getInstance().getScreenSaver().isSaver()) {
                        display();
                    }
                    Thread.sleep(40);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Main.getInstance().getScreenSaver().setSaver(false);
            loadDefault();
        }).start();//.start();
    }

    public void display() throws InterruptedException {
        int r = new Random().nextInt(255);
        int g = new Random().nextInt(255);
        int b = new Random().nextInt(255);
        int i = 0;
        for (int j = 0; j < 360 * 2; j++) {
            if (r >= 255) {
                r = -255;
            } else {
                r++;
            }
            if (g >= 255) {
                g = -255;
            } else {
                g++;
            }
            if (b >= 255) {
                b = -255;
            } else {
                b++;
            }
            if (i >= 360) {
                i = -360;
            } else {
                i++;
            }
            arc.setFill(Color.rgb(Math.abs(r), Math.abs(g), Math.abs(b)));
            arc.setLength(Math.abs(i));
            arc.setRotate(j);
            Thread.sleep(40);
            if (!Main.getInstance().getScreenSaver().isSaver()) {
                Main.getInstance().getScreenSaver().setSaver(false);
                loadDefault();
                return;
            }
        }
    }

    public void loadDefault() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Main.getInstance().loadScene("main-view.fxml");
            }
        };
        Platform.runLater(runnable);
    }

    @FXML
    public Arc arc;

    public void btn(ActionEvent actionEvent) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }
}
