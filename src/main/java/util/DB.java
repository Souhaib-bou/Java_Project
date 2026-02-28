package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Central JDBC connection factory used by all repositories.
 */
public class DB {

    // Single source of truth for database connectivity used by repositories.
    private static final String URL = "jdbc:mysql://localhost:3306/hirely_db";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // put your password here
    private static volatile boolean schemaValidated = false;

    public static Connection getConnection() throws SQLException {
        // Driver comes from mysql-connector-j on the classpath.
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        ensureSchemaCompatibility(connection);
        return connection;
    }

    private static void ensureSchemaCompatibility(Connection connection) throws SQLException {
        if (schemaValidated) {
            return;
        }
        synchronized (DB.class) {
            if (schemaValidated) {
                return;
            }
            try {
                runSanityQuery(connection, "SELECT 1 FROM forum_interaction LIMIT 1", "forum_interaction");
                runSanityQuery(connection, "SELECT 1 FROM forum_notification LIMIT 1", "forum_notification");
                runSanityQuery(connection, "SELECT user_id FROM user LIMIT 1", "user.user_id");
                schemaValidated = true;
                DebugLog.info("DB", "Schema compatibility checks passed.");
            } catch (SQLException ex) {
                DebugLog.error("DB", "Schema compatibility check failed. Required tables/columns are missing.", ex);
                throw new SQLException(
                        "Schema mismatch: expected forum_interaction, forum_notification, and user.user_id",
                        ex);
            }
        }
    }

    private static void runSanityQuery(Connection connection, String sql, String expectedSchemaPart) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet ignored = ps.executeQuery()) {
            // Query execution itself is the schema check.
        } catch (SQLException ex) {
            throw new SQLException("Missing or incompatible schema part: " + expectedSchemaPart + " (query: " + sql + ")",
                    ex);
        }
    }
}
