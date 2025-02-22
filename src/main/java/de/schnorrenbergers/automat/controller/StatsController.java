package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class StatsController implements Initializable {
    public PieChart pie;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<PieChart.Data> data = pie.getData();
        data.clear();
        data.add(new PieChart.Data("Schoki", 10));
        data.add(new PieChart.Data("Gumibären", 20));
        data.add(new PieChart.Data("Menschenfleisch", 5));
        pie.setData(data);
    }

    public void back(ActionEvent actionEvent) {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
        Scene scene = null;
        try {
            scene = new Scene(fxmlLoader.load(), Main.getInstance().getDimension().getWidth(), Main.getInstance().getDimension().getHeight());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Main.getInstance().getStage().setScene(scene);
        Main.getInstance().getStage().show();
    }
}
