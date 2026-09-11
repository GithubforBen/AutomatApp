package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.manager.AddUserHandler;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AddUserController implements Initializable {
    /**
     * So lange bleibt eine Meldung stehen, danach erscheint wieder der nächste
     * Nutzer aus der Warteschlange.
     */
    public static final long MESSAGE_MILLIS = 5000;

    private static final List<String> STATES = List.of("state-next", "state-empty", "state-added", "state-known");
    /** Schriftgröße des Namens je Zustand, entspricht add-user-view.css. */
    private static final Map<String, Integer> NAME_SIZES =
            Map.of("state-next", 50, "state-empty", 50, "state-added", 40, "state-known", 44);

    private static volatile String messageName;
    private static volatile boolean messageIsConfirmation;
    private static volatile long messageUntil;
    private static AddUserController current;

    @FXML
    public VBox panel;
    @FXML
    public Label caption;
    @FXML
    public Label name;
    @FXML
    public Label hint;

    private Timeline refresh;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        current = this;
        update();
        // Kein Thread, der auf die aktuelle Szene prüft: initialize() läuft,
        // bevor loadScene() die neue Szene setzt - so eine Schleife endete
        // sofort, und Meldungen blieben für immer stehen.
        refresh = new Timeline(new KeyFrame(Duration.millis(250), e -> update()));
        refresh.setCycleCount(Animation.INDEFINITE);
        refresh.play();
    }

    /**
     * Bestätigt, dass gerade jemand angelegt wurde. Solange die Bestätigung
     * steht, wird niemand weiteres angelegt (siehe {@link #isConfirming()}),
     * denn der nächste Nutzer ist in der Zeit noch gar nicht zu sehen.
     */
    public static void confirmAdded(String name) {
        show(name, true);
    }

    /**
     * Die gescannte Karte gehört schon jemandem - dann wird niemand angelegt.
     */
    public static void showKnownCard(String name) {
        show(name, false);
    }

    public static boolean isConfirming() {
        return messageIsConfirmation && System.currentTimeMillis() < messageUntil;
    }

    private static void show(String name, boolean confirmation) {
        messageName = name;
        messageIsConfirmation = confirmation;
        messageUntil = System.currentTimeMillis() + MESSAGE_MILLIS;
        AddUserController controller = current;
        if (controller != null) Platform.runLater(controller::update);
    }

    public void update() {
        Scene scene = panel.getScene();
        if (scene != null && scene != Main.getInstance().getStage().getScene()) {
            // Ansicht wurde verlassen.
            refresh.stop();
            if (current == this) current = null;
            return;
        }

        boolean showMessage = messageName != null && System.currentTimeMillis() < messageUntil;
        if (showMessage && messageIsConfirmation) {
            setState("state-added", "Angelegt!", messageName, null);
            return;
        }
        if (showMessage) {
            setState("state-known", "Karte gehört schon", messageName, "Es wurde niemand angelegt.");
            return;
        }

        AddUserHandler.UserAdd peek = AddUserHandler.addQueue.peek();
        if (peek == null) {
            setState("state-empty", "Niemand vorgemerkt", null, "Neue Nutzer auf der Website vormerken.");
            return;
        }
        setState("state-next", "Nächster Nutzer", peek.getVorname() + " " + peek.getNachname(),
                "Jetzt Chipkarte scannen");
    }

    private void setState(String state, String captionText, String nameText, String hintText) {
        if (!panel.getStyleClass().contains(state)) {
            panel.getStyleClass().removeAll(STATES);
            panel.getStyleClass().add(state);
        }
        caption.setText(captionText);
        setOptional(name, nameText);
        setOptional(hint, hintText);
        name.setStyle(nameText == null ? "" : "-fx-font-size: " + fitFontSize(nameText, NAME_SIZES.get(state)) + "px;");
    }

    /**
     * JavaFX bricht zu lange Wörter mitten im Wort um ("Maximiliane-S|ophie").
     * Deshalb die Schrift so weit verkleinern, dass das längste Wort in eine
     * Zeile passt.
     */
    private int fitFontSize(String text, int baseSize) {
        int longestWord = 1;
        for (String word : text.split("\\s+")) longestWord = Math.max(longestWord, word.length());
        double available = panel.getWidth() > 0 ? panel.getWidth() - 70 : 360;
        // Gemessen: ein fettes Zeichen ist im Schnitt gut 0,52 der Schriftgröße
        // breit - 0,56 lässt etwas Luft.
        int fitting = (int) (available / (longestWord * 0.56));
        return Math.max(24, Math.min(baseSize, fitting));
    }

    private static void setOptional(Label label, String text) {
        boolean present = text != null;
        label.setText(present ? text : "");
        label.setVisible(present);
        label.setManaged(present);
    }

    public void back(ActionEvent event) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        Main.getInstance().loadScene("main-view.fxml");
    }
}
