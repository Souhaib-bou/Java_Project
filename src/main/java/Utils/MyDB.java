package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private static MyDB instance;
    private final Connection connection;

    // NOTE: add timeouts so it doesn't "hang"
    private static final String URL =
<<<<<<< HEAD
            "jdbc:mysql://localhost:3306/hirely"
=======
<<<<<<< HEAD
            "jdbc:mysql://localhost:3306/hirely"
=======
            "jdbc:mysql://localhost:3306/hirely?serverTimezone=UTC"
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=UTC"
                    + "&connectTimeout=5000"
                    + "&socketTimeout=5000";

    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

<<<<<<< HEAD
    /**
     * Creates a new MyDB instance.
     */
=======
<<<<<<< HEAD
    /**
     * Creates a new MyDB instance.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private MyDB() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("✅ Connected to DB");
        } catch (SQLException e) {
            // IMPORTANT: don't keep a null connection silently
            throw new RuntimeException("❌ Cannot connect to MySQL. Check server/URL/user/password. Root cause: " + e.getMessage(), e);
        }
    }

<<<<<<< HEAD
    /**
     * Returns the instance value.
     */
=======
<<<<<<< HEAD
    /**
     * Returns the instance value.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public static MyDB getInstance() {
        if (instance == null) instance = new MyDB();
        return instance;
    }
<<<<<<< HEAD
=======
<<<<<<< HEAD
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67

    /**
     * Returns the connection value.
     */
<<<<<<< HEAD
=======
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public Connection getConnection() {
        return connection;
    }
}
