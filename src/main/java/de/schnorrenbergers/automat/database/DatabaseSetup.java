package de.schnorrenbergers.automat.database;

import org.h2.tools.ChangeFileEncryption;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Einmalige Umstellung auf das Passwort vom Startbildschirm.
 * <p>
 * Früher stand in hibernate.cfg.xml ein festes Passwort ("X X", Nutzer "Test"),
 * und das per PIN eingegebene Passwort wurde unter einem Schlüssel gesetzt, den
 * Hibernate gar nicht liest. Jede Datenbank ist deshalb mit diesem festen, in
 * git stehenden Schlüssel verschlüsselt, und jede PIN "funktionierte". Diese
 * Klasse erkennt solche Datenbanken und verschlüsselt sie mit der neuen PIN um.
 */
public final class DatabaseSetup {

    /** Datenbanknutzer, unter dem die App arbeitet. */
    public static final String USER = "MiNt-ZeNtRuM";

    // Muss zu connection.url in hibernate.cfg.xml passen.
    private static final String DIRECTORY = ".";
    private static final String NAME = "MINTdatabase";
    private static final String URL = "jdbc:h2:" + DIRECTORY + "/" + NAME + ";CIPHER=AES;IFEXISTS=TRUE";

    private static final String LEGACY_USER = "Test";
    private static final String LEGACY_FILE_PASSWORD = "X";
    private static final String LEGACY_USER_PASSWORD = "X";

    public enum State {
        /** Es gibt noch keine Datenbank - sie wird mit dem neuen Passwort angelegt. */
        MISSING,
        /** Die Datenbank hat noch den alten, festen Schlüssel. */
        LEGACY_KEY,
        /** Die Datenbank ist mit einem eigenen Passwort geschützt. */
        PROTECTED
    }

    private DatabaseSetup() {
    }

    public static State detect() {
        if (!Files.exists(Path.of(DIRECTORY, NAME + ".mv.db"))) {
            return State.MISSING;
        }
        try (Connection ignored = open(LEGACY_USER, LEGACY_FILE_PASSWORD, LEGACY_USER_PASSWORD)) {
            return State.LEGACY_KEY;
        } catch (SQLException e) {
            return State.PROTECTED;
        }
    }

    /**
     * Verschlüsselt eine Datenbank mit altem Schlüssel neu und legt den
     * App-Nutzer mit dem neuen Passwort an. Die Datenbank darf dabei nirgends
     * geöffnet sein.
     */
    public static void rekey(String filePassword, String userPassword) throws SQLException {
        // Schreibt die Datei in eine temporäre Kopie und ersetzt sie erst danach.
        ChangeFileEncryption.execute(DIRECTORY, NAME, "AES",
                LEGACY_FILE_PASSWORD.toCharArray(), filePassword.toCharArray(), true);

        try (Connection connection = open(LEGACY_USER, filePassword, LEGACY_USER_PASSWORD);
             PreparedStatement create = connection.prepareStatement(
                     "CREATE USER IF NOT EXISTS \"" + USER + "\" PASSWORD ? ADMIN")) {
            create.setString(1, userPassword);
            create.execute();
        }
        // Den alten Nutzer kann nur ein anderer Nutzer löschen.
        try (Connection connection = open(USER, filePassword, userPassword)) {
            connection.createStatement().execute("DROP USER IF EXISTS \"" + LEGACY_USER + "\"");
        }
    }

    private static Connection open(String user, String filePassword, String userPassword) throws SQLException {
        // H2 erwartet bei CIPHER "Dateipasswort Nutzerpasswort" in einem Feld.
        return DriverManager.getConnection(URL, user, filePassword + " " + userPassword);
    }
}
