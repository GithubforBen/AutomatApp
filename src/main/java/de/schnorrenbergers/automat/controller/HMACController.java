package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.auth.HMACToken;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.security.SecureRandom;
import java.util.Base64;

public class HMACController {
    public TextField name;
    public Label text;

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
        String secret = Base64.getEncoder().encodeToString(random.generateSeed(1));
        Main.getInstance().getDatabase().getSessionFactory().inTransaction((session -> {
            HMACToken token = new HMACToken(secret);
            token.id = name.getText();
            session.persist(token);
        }));
        text.setText(name.getText() + ":" + secret);
    }
}
