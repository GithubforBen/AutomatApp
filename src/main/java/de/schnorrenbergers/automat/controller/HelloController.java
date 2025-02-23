package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (SplitPane.Divider divider : pane.getDividers()) {
            divider.positionProperty().addListener((observable, oldValue, newValue) -> {pane.setDividerPosition(0, 0.6967418546365914);});
        }
    }

    @FXML
    public SplitPane pane;

    public void reconnect(ActionEvent actionEvent) {
        try {
            Main.getInstance().start(Main.getInstance().getStage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}