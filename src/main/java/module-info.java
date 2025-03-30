module de.schnorrenbergers.automat {
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires jdk.jsobject;
    requires org.json;
    requires org.apache.commons.lang3;
    requires atlantafx.base;
    requires jdk.httpserver;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;

    requires java.naming;

    opens de.schnorrenbergers.automat to javafx.fxml;
    exports de.schnorrenbergers.automat;
    exports de.schnorrenbergers.automat.controller;
    opens de.schnorrenbergers.automat.controller to javafx.fxml;
    opens de.schnorrenbergers.automat.database.types to org.hibernate.orm.core, jakarta.persistence, java.base;
    exports de.schnorrenbergers.automat.types to org.hibernate.orm.core, jakarta.persistence, java.base;
}