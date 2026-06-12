package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.manager.AvailabilityManager;
import de.schnorrenbergers.automat.manager.ConfigurationManager;
import de.schnorrenbergers.automat.utils.CustomRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminController implements Initializable {
    @FXML
    public Button alarmBTN;
    @FXML
    public GridPane pane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAlarmBTNColor();
        List<Button> buttons = pane.getChildren().stream().filter(node -> node instanceof Button).map(node -> (Button) node).toList().reversed();
        for (int i = 0; i < 7; i++) {
            int finalI = i;
            buttons.get(i).setOnAction(event -> fill(finalI));
        }
        refreshButtons();
    }

    /**
     * Updates each sweet button's label with its current stock ("Vorrat") and, if the slot was
     * deactivated by a failed dispense, with its "Deaktiviert" status instead.
     */
    private void refreshButtons() {
        AvailabilityManager availabilityManager = new AvailabilityManager();
        List<Button> buttons = pane.getChildren().stream().filter(node -> node instanceof Button).map(node -> (Button) node).toList().reversed();
        for (int i = 0; i < 7; i++) {
            String name = new ConfigurationManager().getString("sweets._" + i + ".name");
            int amount = availabilityManager.getAmount(i);
            if (availabilityManager.isDisabled(i)) {
                buttons.get(i).setText(name + "\nDeaktiviert (Vorrat: " + amount + ")");
            } else {
                buttons.get(i).setText(name + "\nVorrat: " + amount);
            }
        }
    }

    /**
     * Sets the color of the alarm button depending on the alarm status
     */
    private void setAlarmBTNColor() {
        boolean alarm = Main.getInstance().isAlarm();
        if (alarm) {
            alarmBTN.setTextFill(Color.GREEN);
        } else {
            alarmBTN.setTextFill(Color.RED);
        }
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    @FXML
    public Button plus;
    @FXML
    private Slider slider;
    private int positive = 0; //pos->0;neg->1;re-enable->2

    /**
     * Handles the navigation back to the main view by loading the "main-view.fxml" scene.
     * This method also updates the timestamp of the last user interaction in the screen saver.
     *
     * @param actionEvent the event that triggered this method, typically a button press
     */
    public void back(ActionEvent actionEvent) {
        Main.getInstance().loadScene("main-view.fxml");
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    /**
     * Toggles the alarm state in the application. If the alarm is currently active,
     * it sends a request to turn it off, and vice versa. Updates the visual
     * representation of the alarm button and resets the screen saver timeout.
     *
     * @param actionEvent the event that triggered this method, typically a button press
     */
    public void alarm(ActionEvent actionEvent) {
        boolean alarm = Main.getInstance().isAlarm();
        try {
            if (alarm) {
                new CustomRequest("alarm_on", CustomRequest.REVIVER.SCANNER).execute();
            } else {
                new CustomRequest("alarm_off", CustomRequest.REVIVER.SCANNER).execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Main.getInstance().setAlarm(!alarm);
        setAlarmBTNColor();
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }

    /**
     * Fills a specific type of item based on the provided type parameter and the current state of the
     * {@code positive} field. Depending on the value of {@code positive}, the method executes one of the following:
     * <ul>
     *     <li>Adds a positive quantity of the item when {@code positive} is 0.</li>
     *     <li>Adds a negative quantity of the item when {@code positive} is 1.</li>
     *     <li>Sets the absolute stock amount (re-enabling a deactivated sweet) when {@code positive} is 2.</li>
     * </ul>
     * Updates the screen saver's last user interaction timestamp to the current system time.
     *
     * @param type the type of item to be processed, represented as an integer identifier.
     */
    public void fill(int type) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        AvailabilityManager availabilityManager = new AvailabilityManager();
        switch (positive) {
            case 0 -> availabilityManager.addSweet(type, (int) slider.getValue());
            case 1 -> availabilityManager.addSweet(type, (int) slider.getValue() * -1);
            case 2 -> {
                availabilityManager.setAmount(type, (int) slider.getValue());
                availabilityManager.enableSweet(type);
            }
        }
        refreshButtons();
    }

    /**
     * Handles the toggling of the fill mode button. Cycles through three states:
     * "+" (add stock, {@code positive == 0}), "-" (remove stock, {@code positive == 1}),
     * and "Reaktivieren" (set absolute stock / re-enable, {@code positive == 2}).
     * Updates the screen saver's last interaction timestamp.
     *
     * @param actionEvent the event that triggered this method, typically a button press
     */
    public void plus(ActionEvent actionEvent) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        positive = (positive + 1) % 3;
        plus.setText(switch (positive) {
            case 0 -> "+";
            case 1 -> "-";
            default -> "Reaktivieren";
        });
    }
}
