package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.types.CustomRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.json.JSONObject;

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
    private Text text;

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
        text.setText("Bitte Karte scannen!");
        MainController.mainController = this;
        try {
            Main.getInstance().setKost();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MainController getMainController() {
        return mainController;
    }

    public Button[] getBtns() {
        return new Button[]{btn_1, btn_2, btn_3, btn_4, btn_5, btn_6, btn_7, btn_8};
    }

    public void click(int number) {
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
        if (Main.getInstance().getLastScan() == null) {
            text.setFill(Color.RED);
            new Thread(() -> {
                try {
                    Thread.sleep(1000 * 2);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                text.setFill(Color.WHITE);
            }).start();
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(new CustomRequest("sweets").execute()).getJSONObject(String.valueOf(number));

            if (Main.getInstance().getLastScan().time.getHour() >= jsonObject.getInt("hours")
                    || Main.getInstance().getLastScan().time.getHour() == Integer.MIN_VALUE
                    || !((Boolean) Main.getInstance().getStatistic().getSettingOrDefault("checkTime", true))) {
                new CustomRequest("dispense").executeComplex("{\"nr\":" + number + ",\"cost\":" + jsonObject.getInt("hours")+ ",\"usr\":" + Arrays.toString(Main.getInstance().getLastScan().getByteAdress()) + "}");
                Main.getInstance().getStatistic().addOne(number);
                Main.getInstance().setLastScan(null);
            } else {
                text.setText("Nicht genug Stunden");
            }
        } catch (IOException e) {
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }

    public Text getText() {
        return text;
    }
}
