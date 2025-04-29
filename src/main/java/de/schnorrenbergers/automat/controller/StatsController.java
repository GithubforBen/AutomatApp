package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Statistic;
import de.schnorrenbergers.automat.manager.StatisticManager;
import de.schnorrenbergers.automat.types.CustomRequest;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import org.hibernate.Session;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.List;
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
            Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
            List<Statistic> sq = session.createSelectionQuery("from Statistic stat where stat.data in :sq AND type = 'SWEET_DISPENSE'", Statistic.class)
                    .setParameter("sq", "type=" + i).getResultList();
            session.close();
            pieData.add(new PieChart.Data(new StatisticManager().getFromId(i), sq.size()));
            //TODO: test
        }
        pie.setData(pieData);
        pie.setAnimated(true);
        JSONObject object;
        try {
            //TODO: get attending students
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
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }
}
