package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private static MyDB instance;
    private final Connection connection;

    // NOTE: add timeouts so it doesn't "hang"
    private static final String URL =
            "jdbc:mysql://localhost:3306/hirely?serverTimezone=UTC"
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=UTC"
                    + "&connectTimeout=5000"
                    + "&socketTimeout=5000";

    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private MyDB() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("✅ Connected to DB");
        } catch (SQLException e) {
            // IMPORTANT: don't keep a null connection silently
            throw new RuntimeException("❌ Cannot connect to MySQL. Check server/URL/user/password. Root cause: " + e.getMessage(), e);
        }
    }

    public static MyDB getInstance() {
        if (instance == null) instance = new MyDB();
        return instance;
    }
    public Connection getConnection() {
        return connection;
    }
}
