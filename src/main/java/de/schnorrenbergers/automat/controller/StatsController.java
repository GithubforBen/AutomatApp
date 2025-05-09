package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.database.types.Statistic;
import de.schnorrenbergers.automat.manager.LoginManager;
import de.schnorrenbergers.automat.manager.StatisticManager;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import org.hibernate.Session;
import org.json.JSONObject;

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

        for (int i = 0; i < 7; i++) {
            Session session = Main.getInstance().getDatabase().getSessionFactory().openSession();
            List<Statistic> sq = session.createSelectionQuery("from Statistic stat where stat.type = 'SWEET_DISPENSE' and stat.data = :data", Statistic.class)
                    .setParameter("data", new JSONObject().append("type", i).toString()).getResultList();
            session.close();
            if (sq.isEmpty()) continue;
            pieData.add(new PieChart.Data(new StatisticManager().getFromId(i), sq.size()));
        }
        pie.setData(pieData);
        pie.setAnimated(true);
        XYChart.Series chartData = new XYChart.Series();
        chart.getData().clear();
        int[] attendance = new LoginManager().getAttendance();
        chartData.getData().add(new XYChart.Data<>("Anwesend", attendance[0]));
        chartData.getData().add(new XYChart.Data<>("Abwesend", attendance[1]));
        chart.getData().addAll(chartData);
    }

    public void back(ActionEvent actionEvent) {
        Main.getInstance().loadScene("main-view.fxml");
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }
}
