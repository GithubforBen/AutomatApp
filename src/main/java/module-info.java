module de.schnorrenbergers.automat {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires jdk.jsobject;
    requires org.json;

    opens de.schnorrenbergers.automat to javafx.fxml;
    exports de.schnorrenbergers.automat;
    exports de.schnorrenbergers.automat.controller;
    opens de.schnorrenbergers.automat.controller to javafx.fxml;
}