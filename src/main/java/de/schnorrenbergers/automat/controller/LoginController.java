package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.Database;
import de.schnorrenbergers.automat.database.DatabaseSetup;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    /** Mindestlänge eines neuen Passworts, zugleich die Zahl der Platzhalter. */
    private final int passwordLenght = 10;
    /** Die ersten Ziffern verschlüsseln die Datei, der Rest ist das Passwort des Datenbanknutzers. */
    private static final int FILE_PASSWORD_DIGITS = 5;

    @FXML
    public Label text;
    @FXML
    public Label hint;
    @FXML
    public GridPane keypad;
    @FXML
    public ProgressIndicator progress;
    List<Integer> input = new ArrayList<>();

    private enum Mode {
        /** Im Hintergrund wird geprüft oder entsperrt - keine Eingabe möglich. */
        BUSY,
        /** Passwort einer geschützten Datenbank eingeben. */
        UNLOCK,
        /** Einmalig ein eigenes Passwort festlegen ... */
        NEW_PASSWORD,
        /** ... und zur Kontrolle wiederholen. */
        CONFIRM_PASSWORD
    }

    private Mode mode = Mode.BUSY;
    private DatabaseSetup.State databaseState;
    private String newPassword;

    /** Akzentblau des PrimerDark-Themes (-color-accent-fg). */
    private static final Color GLINT_COLOR = Color.web("#58a6ff");
    /** Halbe Breite des Lichtstreifens, als Anteil der Textbreite. */
    private static final double GLINT_WIDTH = 0.18;
    private Timeline glint;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        busy("Prüfe…");
        Thread check = new Thread(() -> {
            DatabaseSetup.State state = DatabaseSetup.detect();
            Platform.runLater(() -> {
                databaseState = state;
                switch (state) {
                    case PROTECTED -> ready(Mode.UNLOCK, "Password eingeben", null, Color.WHITE);
                    case LEGACY_KEY -> ready(Mode.NEW_PASSWORD, "Neues Passwort",
                            "Einmalig ein eigenes Passwort festlegen (mindestens " + passwordLenght + " Ziffern). "
                                    + "Danach öffnet nur noch dieses die Datenbank.",
                            Color.WHITE);
                    case MISSING -> ready(Mode.NEW_PASSWORD, "Neues Passwort",
                            "Neue Datenbank: Passwort festlegen (mindestens " + passwordLenght + " Ziffern).",
                            Color.WHITE);
                }
            });
        }, "db-check");
        check.setDaemon(true);
        check.start();
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
        if (input.isEmpty()) return;
        input.removeLast();
        display();
    }

    public void btn_0(ActionEvent actionEvent) {
        press(0);
    }

    public void btn_next(ActionEvent actionEvent) {
        StringBuilder sb = new StringBuilder();
        input.forEach(sb::append);
        String pin = sb.toString();

        switch (mode) {
            case UNLOCK -> {
                if (pin.length() <= FILE_PASSWORD_DIGITS) {
                    ready(Mode.UNLOCK, "Passwort zu kurz", null, Color.RED);
                    return;
                }
                start(pin, false);
            }
            case NEW_PASSWORD -> {
                if (pin.length() < passwordLenght) {
                    ready(Mode.NEW_PASSWORD, "Zu kurz", "Mindestens " + passwordLenght + " Ziffern.", Color.RED);
                    return;
                }
                newPassword = pin;
                ready(Mode.CONFIRM_PASSWORD, "Wiederholen", "Zur Kontrolle dasselbe Passwort noch einmal.", Color.WHITE);
            }
            case CONFIRM_PASSWORD -> {
                if (!pin.equals(newPassword)) {
                    newPassword = null;
                    ready(Mode.NEW_PASSWORD, "Nicht gleich", "Bitte das neue Passwort noch einmal von vorn eingeben.", Color.RED);
                    return;
                }
                start(pin, databaseState == DatabaseSetup.State.LEGACY_KEY);
            }
            case BUSY -> {
            }
        }
    }

    /**
     * Entschlüsselt (und stellt bei Bedarf vorher um) im Hintergrund. Entschlüsseln,
     * Hibernate und Spring hochfahren dauert auf dem Gerät lange; auf dem UI-Thread
     * war das Fenster so lange eingefroren. Nur load() kehrt auf den UI-Thread zurück.
     */
    private void start(String pin, boolean rekey) {
        String filePW = pin.substring(0, FILE_PASSWORD_DIGITS);
        String userPW = pin.substring(FILE_PASSWORD_DIGITS);
        boolean settingUp = mode == Mode.CONFIRM_PASSWORD;
        busy(rekey ? "Verschlüssele…" : "Entsperre…");

        Thread worker = new Thread(() -> {
            if (rekey) {
                try {
                    DatabaseSetup.rekey(filePW, userPW);
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> ready(Mode.NEW_PASSWORD, "Fehler",
                            "Umstellung fehlgeschlagen - bitte erneut versuchen.", Color.RED));
                    return;
                }
            }
            try {
                Main.getInstance().setDatabase(new Database(DatabaseSetup.USER, filePW, userPW));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> ready(Mode.UNLOCK, settingUp ? "Start fehlgeschlagen" : "Falsches Passwort",
                        null, Color.RED));
                return;
            }
            try {
                Main.getInstance().initialise();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> ready(Mode.UNLOCK, "Start fehlgeschlagen", null, Color.RED));
                return;
            }
            Platform.runLater(() -> {
                // Sonst liefe die Animation nach dem Szenenwechsel ewig weiter.
                stopGlint();
                try {
                    Main.getInstance().checkForStuff();
                    Main.getInstance().load();
                } catch (Exception e) {
                    e.printStackTrace();
                    ready(Mode.UNLOCK, "Start fehlgeschlagen", null, Color.RED);
                }
            });
        }, "db-unlock");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Arbeitet im Hintergrund: Tastenfeld gesperrt, Spinner und Lichtstreifen an.
     */
    private void busy(String message) {
        mode = Mode.BUSY;
        keypad.setDisable(true);
        progress.setVisible(true);
        progress.setManaged(true);
        setHint(null);
        setText(message, Color.WHITE, true);
        startGlint();
    }

    /**
     * Wartet auf Eingabe: Tastenfeld frei, bisherige Eingabe verworfen.
     */
    private void ready(Mode next, String message, String hintText, Color color) {
        stopGlint();
        mode = next;
        keypad.setDisable(false);
        progress.setVisible(false);
        progress.setManaged(false);
        input.clear();
        setHint(hintText);
        setText(message, color, true);
    }

    private void setHint(String hintText) {
        hint.setText(hintText == null ? "" : hintText);
        hint.setVisible(hintText != null);
        hint.setManaged(hintText != null);
    }

    public void press(int press) {
        if (mode == Mode.BUSY) return;
        if (press >= 0 && press <= 9) {
            input.add(press);
        }
        display();
    }

    /**
     * Lässt einen Lichtstreifen in der Akzentfarbe in Dauerschleife über den
     * Text laufen, solange die Datenbank im Hintergrund geladen wird.
     */
    private void startGlint() {
        stopGlint();
        DoubleProperty position = new SimpleDoubleProperty();
        position.addListener((observable, oldValue, value) -> text.setTextFill(glintPaint(value.doubleValue())));
        glint = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(position, -GLINT_WIDTH)),
                new KeyFrame(Duration.millis(1400), new KeyValue(position, 1 + GLINT_WIDTH, Interpolator.EASE_BOTH)));
        glint.setCycleCount(Animation.INDEFINITE);
        glint.play();
    }

    private void stopGlint() {
        if (glint != null) glint.stop();
        glint = null;
    }

    private static Paint glintPaint(double position) {
        if (position + GLINT_WIDTH <= 0 || position - GLINT_WIDTH >= 1) return Color.WHITE;
        return new LinearGradient(0, 0, 1, 0.4, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(clamp(position - GLINT_WIDTH), Color.WHITE),
                new Stop(clamp(position), GLINT_COLOR),
                new Stop(clamp(position + GLINT_WIDTH), Color.WHITE),
                new Stop(1, Color.WHITE));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    public void display() {
        String s = "";
        // Längere Passwörter sollen sichtbar länger werden, nicht bei 10 stehen bleiben.
        for (int i = 0; i < Math.max(passwordLenght, input.size()); i++) {
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
