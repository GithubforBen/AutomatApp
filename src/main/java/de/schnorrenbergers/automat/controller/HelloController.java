package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.utils.CustomRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;

import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (SplitPane.Divider divider : pane.getDividers()) {
            divider.positionProperty().addListener((observable, oldValue, newValue) -> {
                pane.setDividerPosition(0, 0.6967418546365914);
            });
        }
    }

    @FXML
    public SplitPane pane;

    public void reconnect(ActionEvent actionEvent) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        try {
            String ping = new CustomRequest("ping").execute();
            if (ping == null) {
                return;
            }
            Main.getInstance().startWithouthPassword(Main.getInstance().getStage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}