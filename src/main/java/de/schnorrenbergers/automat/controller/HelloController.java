package de.schnorrenbergers.automat.controller;

import de.schnorrenbergers.automat.Main;
import javafx.event.ActionEvent;

import java.io.IOException;

public class HelloController {

    public void reconnect(ActionEvent actionEvent) {
        try {
            Main.getInstance().start(Main.getInstance().getStage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}