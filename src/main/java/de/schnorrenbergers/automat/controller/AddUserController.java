package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.manager.AddUserHandler;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class AddUserController implements Initializable {
    public static String lastScan;
    @FXML
    public Label nextUser;
    public Label announce;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        announce.setVisible(false);
        update();
        new Thread(() -> {
            while ("add-user-view.fxml".equals(Main.getInstance().getStage().getScene().getUserData())) {
                try {
                    Platform.runLater(this::update);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void update() {
        AddUserHandler.UserAdd peek = AddUserHandler.addQueue.peek();
        if (lastScan != null) {
            announce.setVisible(true);
            announce.setText("Scan: " + lastScan);
        } else announce.setVisible(false);
        if (peek == null) {
            nextUser.setText("Derzeitig keine neuen Nutzer vorhanden!");
            return;
        }
        nextUser.setText("Nächster Nutzer:" + peek.getVorname() + " " + peek.getNachname());
    }

    public void back(ActionEvent event) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        Main.getInstance().loadScene("main-view.fxml");
    }
}
