package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Statistic;
import de.schnorrenbergers.automat.manager.AlarmManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    @FXML
    public Button shut;
    @FXML
    public CheckBox availability;
    @FXML
    public CheckBox autoLock;
    @FXML
    public CheckBox alarm;
    @FXML
    public CheckBox checkTime;
    @FXML
    public CheckBox screensaver;
    @FXML
    public Slider slider_logout;
    @FXML
    public Text display;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        slider_logout.valueProperty().addListener((observable, oldValue, newValue) -> {
            display();
        });
        slider_logout.setValue(Main.getInstance().getLogoutTime());
        display();
        availability.setSelected(Main.getInstance().isCheckAvailability());
        availability.setOnAction((ActionEvent event) -> {
            Main.getInstance().setCheckAvailability(availability.isSelected());
        });
        // Eigene Einstellung, unabhängig von "Verfügbarkeit überprüfen": sperrt
        // Fächer mit Vorrat 0 und nach Ausgaben ohne Erfolgsmeldung.
        autoLock.setSelected(Main.getInstance().isAutoLock());
        autoLock.setOnAction((ActionEvent event) -> {
            Main.getInstance().setAutoLock(autoLock.isSelected());
        });
        // Aus: Lichtschranken ohne Ausgabe lösen keinen Alarm mehr aus, ein
        // laufender Alarm geht aus.
        alarm.setSelected(Main.getInstance().isAlarmEnabled());
        alarm.setOnAction((ActionEvent event) -> {
            Main.getInstance().setAlarmEnabled(alarm.isSelected());
            if (!alarm.isSelected()) AlarmManager.disarm("Alarm in den Einstellungen abgeschaltet");
            Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        });
        checkTime.setSelected(Boolean.parseBoolean(Main.getInstance().getSettings().getSettingOrDefault("checkTime", "true")));
        checkTime.setOnAction((ActionEvent event) -> {
            Main.getInstance().getSettings().setSetting("checkTime", String.valueOf(checkTime.isSelected()));
        });
        screensaver.setSelected(Main.getInstance().getScreenSaver().isDoSaver());
        screensaver.setOnAction((ActionEvent event) -> {
            Main.getInstance().getScreenSaver().setDoSaver(screensaver.isSelected());
        });
        // Kein Thread, der auf die aktuelle Szene prüft: initialize() läuft, bevor
        // loadScene() die neue Szene setzt (siehe AddUserController).
        shutTicker = new Timeline(new KeyFrame(Duration.millis(250), e -> updateShutButton()));
        shutTicker.setCycleCount(Animation.INDEFINITE);
        shutTicker.play();
        updateShutButton();
    }

    public void fullscreen(ActionEvent actionEvent) {
        Main.getInstance().getStage().setFullScreen(!Main.getInstance().getStage().isFullScreen());
        Main.getInstance().getStage().show();
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void exit(ActionEvent actionEvent) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        System.exit(0);
    }

    public void back(ActionEvent actionEvent) {
        Main.getInstance().loadScene("main-view.fxml");
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void cancel(ActionEvent actionEvent) {
        slider_logout.setValue(Main.getInstance().getLogoutTime());
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void Ok(ActionEvent actionEvent) {
        Main.getInstance().setLogoutTime(((int) slider_logout.getValue()));
        display();
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void display() {
        display.setText(Main.getInstance().getLogoutTime() + "→" + ((int) slider_logout.valueProperty().get()));
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    public void resetStats(ActionEvent actionEvent) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        Main.getInstance().getDatabase().getSessionFactory().inTransaction(session -> {
            List<Statistic> stats = session.createSelectionQuery("from Statistic s", Statistic.class).getResultList();
            stats.forEach(session::remove);
        });
    }

    /**
     * Wann sich der Automat beim geplanten Herunterfahren beendet (0 = keins
     * geplant). Statisch, denn die Einstellungen werden bei jedem Öffnen neu
     * geladen (auch nach dem Bildschirmschoner) - sonst zeigte der Knopf
     * wieder "Herunterfahren" statt "Abbrechen", obwohl es noch läuft.
     */
    private static volatile long shutdownAt;

    private Timeline shutTicker;

    public void shutdown(ActionEvent actionEvent) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        if (shutdownAt == 0) {
            if (!runCommand("shutdown")) return;
            long at = System.currentTimeMillis()
                    + 1000L * Main.getInstance().getConfigurationManager().getInt("shutdown-delay");
            shutdownAt = at;
            Thread timer = new Thread(() -> {
                try {
                    Thread.sleep(Math.max(0, at - System.currentTimeMillis()));
                } catch (InterruptedException e) {
                    return;
                }
                // Abgebrochen (0) oder inzwischen neu geplant: nicht beenden.
                if (shutdownAt == at) System.exit(0);
            }, "shutdown-timer");
            timer.setDaemon(true);
            timer.start();
        } else {
            runCommand("shutdown", "-c");
            shutdownAt = 0;
        }
        updateShutButton();
    }

    private static boolean runCommand(String... command) {
        try {
            new ProcessBuilder(command).inheritIO().start();
            return true;
        } catch (IOException e) {
            System.err.println("Befehl " + String.join(" ", command) + " fehlgeschlagen: " + e.getMessage());
            return false;
        }
    }

    /**
     * Zeigt "Herunterfahren" oder, solange es läuft, "Abbrechen" mit den
     * verbleibenden Sekunden bis der Automat sich beendet.
     */
    private void updateShutButton() {
        Scene scene = shut.getScene();
        if (scene != null && scene != Main.getInstance().getStage().getScene()) {
            // Ansicht wurde verlassen.
            shutTicker.stop();
            return;
        }
        long at = shutdownAt;
        if (at == 0) {
            shut.setText("Herunterfahren");
            shut.setStyle("");
        } else {
            long seconds = Math.max(0, (at - System.currentTimeMillis() + 999) / 1000);
            shut.setText("Abbrechen\n(noch " + seconds + " s)");
            shut.setStyle("-fx-base: #d9534f; -fx-text-fill: white;");
        }
    }

    public void refresh(ActionEvent event) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        Main.getInstance().ping();
    }

    public void addUser(ActionEvent event) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        Main.getInstance().loadScene("add-user-view.fxml");
    }

    public void hmac(ActionEvent event) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        Main.getInstance().loadScene("hmac-view.fxml");
    }
}
