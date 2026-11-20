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
        // Ohne Besitzer öffnet sich der Dialog hinter dem Automatenfenster
        // (das ist "always on top") und ist am Gerät unsichtbar.
        dialog.initOwner(Main.getInstance().getStage());
        dialog.setTitle("IP Address");
        dialog.setHeaderText("Enter netcat IP Address run nc -l 12345");
        dialog.setContentText("IP Address:");

        Optional<String> result = dialog.showAndWait();
        result.map(String::trim).ifPresent(ip -> {
            // Nur eine IP-Adresse oder ein Hostname - früher landete die Eingabe in
            // einem Shell-Befehl ("1.2.3.4; befehl" wäre ausgeführt worden), und ein
            // führendes "-" wäre bei nc eine Option.
            if (!ip.matches("[A-Za-z0-9][A-Za-z0-9.:-]*")) {
                text.setText("Ungültige Adresse: " + ip);
                return;
            }
            // nc direkt starten, ohne Shell; der Schlüssel geht über stdin.
            ProcessBuilder pb = new ProcessBuilder("nc", ip, "12345");
            try {
                Process start = pb.start();
                try (var stdin = start.getOutputStream()) {
                    stdin.write((secret + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
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
