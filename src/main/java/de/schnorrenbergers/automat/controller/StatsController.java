package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.types.CustomRequest;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class StatsController implements Initializable {
    public PieChart pie;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<PieChart.Data> data = pie.getData();
        data.clear();
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(new CustomRequest("sweets").execute());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < 7; i++) {
            data.add(new PieChart.Data(jsonObject.getJSONObject("" + i).getString("name"), Main.getInstance().getStatistic().getStat(i)));
        }
        pie.setData(data);
    }

    public void back(ActionEvent actionEvent) {
        Main.getInstance().loadScene("main-view.fxml");
    }
}
