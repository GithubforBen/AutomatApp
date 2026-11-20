package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.manager.AlarmManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.text.Text;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class PinController implements Initializable {

    private static final Logger LOG = Logger.getLogger(PinController.class.getName());

    /** Die zuletzt getippten Ziffern, neueste vorne; -1 = noch nichts. */
    private int[] last;
    /**
     * Admin- und Einstellungs-PIN kommen aus der config.yaml ("admin-pin",
     * "settings-pin"). Fehlen sie dort, gelten die bisherigen Codes - die stehen
     * aber öffentlich im Quelltext und sollten in der config.yaml ersetzt werden.
     */
    private int[] code;
    private int[] settings;

    @FXML
    private Text text;

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

    public void btn_back(ActionEvent actionEvent) {//Loads pin scene

        Main.getInstance().loadScene("main-view.fxml");
    }

    public void btn_0(ActionEvent actionEvent) {
        press(0);
    }

    public void btn_next(ActionEvent actionEvent) {
        String sceneId = "";
        if (endsWith(code)) {
            text.setText("Korrekt!");
            // Wer die Admin-PIN kennt, darf den Alarm beenden.
            AlarmManager.disarm("Admin-PIN");
            sceneId = "admin-view.fxml";
        } else if (endsWith(settings)) {
            text.setText("Korrekt!");
            sceneId = "settings-view.fxml";
        } else {
            text.setText("_ ".repeat(last.length));
            Arrays.fill(last, -1);
            sceneId = "stats-view.fxml";
        }
        Main.getInstance().loadScene(sceneId);
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        code = readPin("admin-pin", "12345");
        settings = readPin("settings-pin", "33333");
        if (Arrays.equals(code, settings)) {
            LOG.warning("admin-pin und settings-pin sind gleich - die Einstellungen sind so nicht erreichbar");
        }
        last = new int[Math.max(code.length, settings.length)];
        text.setText("_ ".repeat(last.length));
        Arrays.fill(last, -1);
    }

    /**
     * Liest eine PIN (nur Ziffern) aus der config.yaml. Ohne gültigen Eintrag gilt
     * der bisherige Code.
     */
    private static int[] readPin(String key, String fallback) {
        Object value = Main.getInstance().getConfigurationManager().get(key);
        String pin = value == null ? "" : String.valueOf(value).trim();
        if (!pin.matches("[0-9]{4,12}")) {
            if (!pin.isEmpty()) LOG.warning(key + " in der config.yaml ist ungültig (4 bis 12 Ziffern) - es gilt der alte Code");
            pin = fallback;
        }
        return pin.chars().map(c -> c - '0').toArray();
    }

    /**
     * Sind die zuletzt getippten Ziffern genau diese PIN? Wie bisher zählen nur
     * die letzten Ziffern - ein vertippter Anfang stört nicht.
     */
    private boolean endsWith(int[] pin) {
        if (pin.length > last.length) return false;
        for (int i = 0; i < pin.length; i++) {
            // last[0] ist die neueste Ziffer, pin[pin.length - 1] die letzte der PIN.
            if (last[i] != pin[pin.length - 1 - i]) return false;
        }
        return true;
    }

    public void press(int press) {
        if (press >= 0 && press <= 9) {
            int[] temp = last.clone();
            temp[0] = press;
            if (last.length - 1 >= 0) System.arraycopy(last, 0, temp, 1, last.length - 1);
            last = temp;
        }
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        display();
    }

    public void display() {
        StringBuilder s = new StringBuilder();
        for (int i : last) {
            if (i == -1) {
                s.append("_ ");
            } else {
                s.append("* ");
            }
        }
        text.setText(s.toString());
    }
}
