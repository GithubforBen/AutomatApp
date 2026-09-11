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
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;
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
        useWholeNumbers((NumberAxis) chart.getYAxis(), Math.max(attendance[0], attendance[1]));
    }

    /**
     * Es gibt nur ganze Schüler*innen: Die automatische Skalierung wählt bei
     * kleinen Zahlen aber Schritte wie 0,5 oder 0,25. Deshalb die Achse selbst
     * in ganzen Schritten einteilen (höchstens etwa zehn Striche).
     */
    static void useWholeNumbers(NumberAxis axis, int max) {
        int top = Math.max(1, max);
        int step = Math.max(1, (int) Math.ceil(top / 10.0));
        axis.setAutoRanging(false);
        axis.setLowerBound(0);
        axis.setUpperBound(Math.ceil((double) top / step) * step);
        axis.setTickUnit(step);
        axis.setMinorTickCount(0);
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number number) {
                return String.valueOf(number.intValue());
            }

            @Override
            public Number fromString(String string) {
                return Integer.parseInt(string);
            }
        });
    }

    public void back(ActionEvent actionEvent) {
        Main.getInstance().loadScene("main-view.fxml");
        Main.getInstance().getScreenSaver().setLastMove(System.currentTimeMillis());
    }
}
