/**
 * The module definition for the application "de.schnorrenbergers.automat".<p>
 * <p>
 * This module defines the necessary dependencies and accessibility for the components of<p>
 * the application, including third-party libraries and internal packages.<p>
 * <p>
 * Required modules:<p>
 * - javafx.fxml: Provides JavaFX functionalities, specifically FXML support for UI definitions.<p>
 * - org.kordamp.bootstrapfx.core: Enables BootstrapFX for styling JavaFX applications.<p>
 * - jdk.jsobject: Used for Java binding for JavaScript objects.<p>
 * - org.json: Supports JSON manipulation and parsing.<p>
 * - org.apache.commons.lang3: Extends Java utility functionalities commonly used in applications.<p>
 * - atlantafx.base: Provides additional JavaFX component styles and resources.<p>
 * - jdk.httpserver: Used for creating lightweight HTTP servers.<p>
 * - org.hibernate.orm.core: Facilitates object-relational mapping (ORM) for database operations.<p>
 * - jakarta.persistence: Provides the Jakarta Persistence API for database interactions.<p>
 * - java.naming: Supports Java Naming and Directory Interface (JNDI).<p>
 * <p>
 * Module exports:<p>
 * - de.schnorrenbergers.automat: The main package of the application.<p>
 * - de.schnorrenbergers.automat.controller: Contains controller classes used in the application.<p>
 * - de.schnorrenbergers.automat.utils.types: Exposes types required by Hibernate ORM and JPA.<p>
 * <p>
 * Module opens directives:<p>
 * - de.schnorrenbergers.automat: Opened to javafx.fxml for accessing FXML configurations.<p>
 * - de.schnorrenbergers.automat.controller: Opened to javafx.fxml for controller initialization via FXML.<p>
 * - de.schnorrenbergers.automat.database.types: Opened to Hibernate ORM, Jakarta Persistence, and Java base modules for database entity management.<p>
 * - de.schnorrenbergers.automat.database.types.types: Opened to the relevant ORM and persistence frameworks for advanced type management.<p>
 */

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
    requires org.yaml.snakeyaml;
    requires bcrypt;
    requires java.desktop;

    opens de.schnorrenbergers.automat to javafx.fxml;
    exports de.schnorrenbergers.automat;
    exports de.schnorrenbergers.automat.controller;
    opens de.schnorrenbergers.automat.controller to javafx.fxml;
    opens de.schnorrenbergers.automat.database.types to org.hibernate.orm.core, jakarta.persistence, java.base;
    exports de.schnorrenbergers.automat.utils.types to org.hibernate.orm.core, jakarta.persistence, java.base;
    opens de.schnorrenbergers.automat.database.types.types to jakarta.persistence, java.base, org.hibernate.orm.core;
    exports de.schnorrenbergers.automat.utils to jakarta.persistence, java.base, org.hibernate.orm.core;
}