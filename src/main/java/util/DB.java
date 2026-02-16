package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central JDBC connection factory used by all repositories.
 */
public class DB {

    // Single source of truth for database connectivity used by repositories.
    private static final String URL = "jdbc:mysql://localhost:3306/hirely_db";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // put your password here

    public static Connection getConnection() throws SQLException {
        // Driver comes from mysql-connector-j on the classpath.
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
