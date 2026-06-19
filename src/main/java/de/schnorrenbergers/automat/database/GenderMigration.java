package de.schnorrenbergers.automat.database;

import de.schnorrenbergers.automat.database.types.types.Gender;
import org.hibernate.SessionFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The {@code Gender} enum has been trimmed down over time, but rows persisted while it still
 * held a removed constant (e.g. {@code DUAL_GENDER}, {@code OTHER}) are still sitting in the
 * "user" table. Hibernate's {@code EnumType.STRING} throws {@code IllegalArgumentException} the
 * moment such a row is hydrated, which happens during {@code Main#initialise()}'s seed-data check
 * and would otherwise stop the application from starting. This runs as plain JDBC (no entity
 * hydration) right after the {@link SessionFactory} is built, so it never trips that conversion.
 */
final class GenderMigration {

    // Removed constants with an evidenced, semantically close replacement still in the enum.
    private static final Map<String, Gender> KNOWN_REMAPPING = Map.of(
            "DUAL_GENDER", Gender.BIGENDER,
            "OTHER", Gender.NON_BINARY
    );

    // Used for any removed constant without a specific mapping above. Gender no longer has an
    // "OTHER" catch-all, so NON_BINARY is the closest neutral default.
    private static final Gender FALLBACK = Gender.NON_BINARY;

    private GenderMigration() {
    }

    static void run(SessionFactory sessionFactory) {
        Set<String> valid = new HashSet<>();
        for (Gender gender : Gender.values()) valid.add(gender.name());

        try (var session = sessionFactory.openSession()) {
            session.doWork(connection -> {
                Set<String> stale = new HashSet<>();
                try (Statement statement = connection.createStatement();
                     ResultSet rs = statement.executeQuery("SELECT DISTINCT \"gender\" FROM \"user\"")) {
                    while (rs.next()) {
                        String value = rs.getString(1);
                        if (value != null && !valid.contains(value)) stale.add(value);
                    }
                }

                for (String value : stale) {
                    Gender replacement = KNOWN_REMAPPING.getOrDefault(value, FALLBACK);
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE \"user\" SET \"gender\" = ? WHERE \"gender\" = ?")) {
                        update.setString(1, replacement.name());
                        update.setString(2, value);
                        int updated = update.executeUpdate();
                        System.out.println("Gender migration: remapped " + updated + " row(s) from \"" + value + "\" to \"" + replacement.name() + "\"");
                    }
                }
            });
        }
    }
}
