package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.utils.CustomRequest;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class HelloController implements Initializable {

    @FXML
    public SplitPane pane;
    @FXML
    public Button button;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        button.setDisable(false);
        for (SplitPane.Divider divider : pane.getDividers()) {
            divider.positionProperty().addListener((observable, oldValue, newValue) -> {
                pane.setDividerPosition(0, 0.6967418546365914);
            });
            button.setText("Verbindung wiederherstellen");
        }
    }

    public void loadConnection() {
        button.setDisable(true);
        button.setText("Ping...");
        new Thread(() -> {
            String website;
            try {
                website =
                        new CustomRequest("ping", CustomRequest.REVIVER.WEBSITE).execute();
            } catch (Exception e) {
                website = null;
            }
            String dispenser;
            try {
                dispenser = new CustomRequest("ping", CustomRequest.REVIVER.DISPENSER).execute();
            } catch (Exception e) {
                dispenser = null;
            }
            String scanner;
            try {
                scanner = new CustomRequest("ping", CustomRequest.REVIVER.SCANNER).execute();
            } catch (IOException e) {
                scanner = null;
            }
            String finalWebsite = website;
            String finalScanner = scanner;
            String finalDispenser = dispenser;
            Platform.runLater(() -> {
                button.setDisable(false);
                button.setText("Verbindung wiederherstellen\n" +
                        "Website: " + Objects.requireNonNullElseGet(finalWebsite, () -> "/") + "\n" +
                        "Ausgabe: " + Objects.requireNonNullElseGet(finalDispenser, () -> "/") + "\n" +
                        "Scanner: " + Objects.requireNonNullElseGet(finalScanner, () -> "/") + "\n");
            });
            if (website != null && dispenser != null && scanner != null) {
                try {
                    Main.getInstance().startWithouthPassword(Main.getInstance().getStage());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public void reconnect(ActionEvent actionEvent) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        try {
            loadConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}