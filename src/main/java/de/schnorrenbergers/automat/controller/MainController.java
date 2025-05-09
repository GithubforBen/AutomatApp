package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.manager.AvailabilityManager;
import de.schnorrenbergers.automat.manager.ConfigurationManager;
import de.schnorrenbergers.automat.manager.KontenManager;
import de.schnorrenbergers.automat.manager.StatisticManager;
import de.schnorrenbergers.automat.utils.CustomRequest;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private static MainController mainController;
    @FXML
    public ImageView image;
    @FXML
    private Button btn_1;
    @FXML
    private Button btn_2;
    @FXML
    private Button btn_3;
    @FXML
    private Button btn_4;
    @FXML
    private Button btn_5;
    @FXML
    private Button btn_6;
    @FXML
    private Button btn_7;
    @FXML
    private Button btn_8;
    @FXML
    private Label text;

    public void button1(ActionEvent actionEvent) {
        click(0);
    }

    public void button2(ActionEvent actionEvent) {
        click(1);
    }

    public void button3(ActionEvent actionEvent) {
        click(2);
    }

    public void button4(ActionEvent actionEvent) {
        click(3);
    }

    public void button5(ActionEvent actionEvent) {
        click(4);
    }

    public void button6(ActionEvent actionEvent) {
        click(5);
    }

    public void button7(ActionEvent actionEvent) {
        click(6);
    }

    public void button8(ActionEvent actionEvent) {//Loads pin scene
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        Main.getInstance().loadScene("pin-view.fxml");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        setText("Bitte Karte scannen", Color.WHITE, true);
        MainController.mainController = this;
        try {
            Main.getInstance().kost();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        text.setWrapText(true);
    }

    public static MainController getMainController() {
        return mainController;
    }

    public Button[] getBtns() {
        return new Button[]{btn_1, btn_2, btn_3, btn_4, btn_5, btn_6, btn_7, btn_8};
    }

    /**
     * Handles the logic for dispensing an item identified by its number.
     * Verifies user's available balance in hours against the cost,
     * processes the request, updates statistics, and manages screen messages.
     *
     * @param number The identifier of the item to be dispensed.
     */
    public void click(int number) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        if (Main.getInstance().getLastScan() == null) {
            setText("Bitte Karte Scannen", Color.RED, true);
            new Thread(() -> {
                try {
                    Thread.sleep(1000 * 2);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                setText("Bitte Karte Scannen", Color.WHITE, true);
            }).start();
            return;
        }
        try {
            KontenManager kontenManager = new KontenManager(Main.getInstance().getLastScan());
            ConfigurationManager configurationManager = new ConfigurationManager();
            AvailabilityManager availabilityManager = new AvailabilityManager();
            availabilityManager.addSweet(number, -1);
            if (kontenManager.getKonto().getBalance() >= configurationManager.getInt("sweets._" + number + ".kost")
                    || kontenManager.getKonto().getBalance() == Integer.MIN_VALUE
                    || !Boolean.parseBoolean(Main.getInstance().getSettings().getSettingOrDefault("checkTime", String.valueOf(true)))) {
                new CustomRequest("dispense").executeComplex("{\"nr\":" + number + ",\"cost\":" + configurationManager.getInt("sweets._" + number + ".kost") + ",\"usr\":" + Arrays.toString(Main.getInstance().getLastScan()) + "}");
                Main.getInstance().setLastScan(null);
                kontenManager.withdraw(configurationManager.getInt("sweets._" + number + ".kost"));
                Main.getInstance().kost();
                new StatisticManager().persistDispense(number);
            } else {
                setText("Nicht genug Stunden", Color.RED, true);
            }
        } catch (Exception e) {
            setText("Unbekannte karte[login]", Color.RED, true);
        }
    }

    /**
     * Updates the textual content and appearance of the `Text` node on the JavaFX application thread.
     * The method clears any existing text, sets the new text string, adjusts the font size if specified,
     * and applies the provided `Paint` color to the text.
     *
     * @param s     The new text to display. If null, no text update is performed.
     * @param paint The paint color for the text. If null, the text color is not updated.
     * @param first A boolean indicating whether this is the first time setting the text.
     *              If true, a larger font size is applied.
     */
    public void setText(String s, Paint paint, boolean first) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (s != null) {
                    text.setText("");
                    text.setText(s);
                    double fontSize = text.getFont().getSize();
                    if (first) fontSize = 48;
                    text.setFont(new Font(fontSize));
                    text.widthProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue.doubleValue() > 480) {
                            double fontSizee = text.getFont().getSize() - 0.5;
                            text.setFont(new Font(fontSizee));
                        }
                    });
                }
                if (paint != null) text.setTextFill(paint);
            }
        });
    }
}
