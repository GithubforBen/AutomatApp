package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.auth.HMACToken;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HMACController implements Initializable {
    public TextField name;
    public Label text;
    public ChoiceBox<String> drop;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        load();
    }

    private void load() {
        drop.getItems().clear();
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((session -> {
            drop.getItems().addAll(session.createSelectionQuery("from HMACToken", HMACToken.class).list().stream().map((t) -> t.id).collect(Collectors.toCollection(ArrayList::new)));
        }));
    }

    public void back(ActionEvent event) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        Main.getInstance().loadScene("main-view.fxml");
    }

    public void ok(ActionEvent event) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        if (name.getText().isEmpty()) {
            text.setText("Bitte einen Namen angeben");
            return;
        }
        SecureRandom random = new SecureRandom();
        random.setSeed(random.generateSeed(16));
        String secret = Base64.getEncoder().encodeToString(random.generateSeed(10000));
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((session -> {
            HMACToken token = new HMACToken(secret);
            token.id = name.getText();
            session.persist(token);
        }));
        text.setText(name.getText() + ":" + secret);
        TextInputDialog dialog = new TextInputDialog("127.0.0.1");
        dialog.setTitle("IP Address");
        dialog.setHeaderText("Enter netcat IP Address run nc -l 12345");
        dialog.setContentText("IP Address:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(ip -> {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "echo \"" + secret + "\" | nc " + ip + " 12345");
            try {
                Process start = pb.start();
                Thread.sleep(1000);
                start.destroy();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        });
        load();
    }

    public void delete(ActionEvent event) {
        if (drop.getValue() == null) return;
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((session -> {
            HMACToken hmacToken = session.find(HMACToken.class, drop.getValue());
            if (hmacToken == null) return;
            session.remove(hmacToken);
        }));
        load();
    }
}
