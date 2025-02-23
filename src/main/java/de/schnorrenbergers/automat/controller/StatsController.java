package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.types.CustomRequest;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.text.Text;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class StatsController implements Initializable {
    public PieChart pie;
    @FXML
    public BarChart chart;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<PieChart.Data> pieData = pie.getData();
        pieData.clear();
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(new CustomRequest("sweets").execute());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < 7; i++) {
            pieData.add(new PieChart.Data(jsonObject.getJSONObject("" + i).getString("name"), Main.getInstance().getStatistic().getStat(i)));
        }
        pie.setData(pieData);
        pie.setAnimated(true);
        JSONObject object;
        try {
            object = new JSONObject(new CustomRequest("mint").execute());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        XYChart.Series chartData = new XYChart.Series();
        chart.getData().clear();
        chartData.getData().add(new XYChart.Data<>("Anwesend", object.getInt("da")));
        chartData.getData().add(new XYChart.Data<>("Abwesend", object.getInt("weg")));
        chart.getData().addAll(chartData);
    }

    public void back(ActionEvent actionEvent) {
        Main.getInstance().loadScene("main-view.fxml");
    }
}
