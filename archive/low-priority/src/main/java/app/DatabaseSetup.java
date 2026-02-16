package app;

import util.DB;
import java.sql.*;

public class DatabaseSetup {
    public static void main(String[] args) {
        System.out.println("Starting Database Setup...");
        try (Connection conn = DB.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Insert Role
            System.out.println("Ensuring ADMIN role exists...");
            try (Statement stmt = conn.createStatement()) {
                // Try inserting with role_id=2, name='ADMIN'
                try {
                    stmt.executeUpdate("INSERT INTO `role` (`role_id`, `name`) VALUES (2, 'ADMIN')");
                    System.out.println("Inserted role (2, 'ADMIN') successfully.");
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1062) { // Duplicate entry
                        System.out.println("Role 'ADMIN' or ID 2 already exists.");
                    } else {
                        // Try alternative column names if it failed
                        try {
                            stmt.executeUpdate("INSERT INTO `role` (`role_name`) VALUES ('ADMIN')");
                            System.out.println("Inserted role 'ADMIN' successfully (auto-increment).");
                        } catch (SQLException e2) {
                            System.out.println("Could not insert role: " + e2.getMessage());
                        }
                    }
                }
            }

            // 2. Insert Admin User
            System.out.println("Ensuring Admin user exists...");
            try (Statement stmt = conn.createStatement()) {
                // Determine the correct role_id for ADMIN
                long adminRoleId = 2;
                try (ResultSet rs = stmt
                        .executeQuery("SELECT role_id FROM role WHERE name='ADMIN' OR role_name='ADMIN'")) {
                    if (rs.next()) {
                        adminRoleId = rs.getLong(1);
                    }
                }

                // Insert user
                try {
                    String userSql = "INSERT INTO `user` (`user_id`, `first_name`, `last_name`, `role_id`) VALUES (3, 'Admin', 'Hirely', ?)";
                    try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                        ps.setLong(1, adminRoleId);
                        ps.executeUpdate();
                        System.out.println("Inserted admin user (ID 3) successfully.");
                    }
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1062) {
                        System.out.println("Admin user ID 3 already exists.");
                    } else {
                        // Try without user_id if auto-increment
                        String userSql = "INSERT INTO `user` (`first_name`, `last_name`, `role_id`) VALUES ('Admin', 'Hirely', ?)";
                        try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                            ps.setLong(1, adminRoleId);
                            ps.executeUpdate();
                            System.out.println("Inserted admin user successfully (auto-increment).");
                        } catch (SQLException e2) {
                            System.out.println("Could not insert admin user: " + e2.getMessage());
                        }
                    }
                }
            }

            conn.commit();
            System.out.println("Database Setup Finished.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
