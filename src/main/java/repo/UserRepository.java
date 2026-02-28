package repo;

import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Read-only helper repository for resolving user display names.
 */
public class UserRepository {

    // READ: lightweight lookup used by card metadata and profile headers.
    public String getDisplayNameById(long userId) {
        String sql = "SELECT first_name, last_name FROM user WHERE user_id = ?";

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("first_name") + " " + rs.getString("last_name");
                }
            }
        } catch (Exception ignored) {
            // UI should still work even if user lookup fails.
        }

        return "User #" + userId;
    }

    public long findUserIdByEmail(String email) {
        String sql = "SELECT user_id FROM user WHERE email = ? LIMIT 1";
        if (email == null || email.isBlank()) {
            return -1;
        }
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("user_id");
                }
            }
        } catch (Exception ignored) {
            // UI should still work if optional lookup fails.
        }
        return -1;
    }

    public long findOrCreateSystemUserId(String email, String firstName, String lastName) {
        long existing = findUserIdByEmail(email);
        if (existing > 0) {
            return existing;
        }
        if (email == null || email.isBlank()) {
            return -1;
        }

        String safeFirst = (firstName == null || firstName.isBlank()) ? "System" : firstName.trim();
        String safeLast = (lastName == null || lastName.isBlank()) ? "Bot" : lastName.trim();
        String sql = """
                INSERT INTO user (first_name, last_name, email, password_hash, role_id, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, safeFirst);
            ps.setString(2, safeLast);
            ps.setString(3, email.trim());
            ps.setString(4, "system_bot");
            ps.setInt(5, 1);
            ps.setString(6, "active");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (Exception ignored) {
            // Fallback: another process might have inserted concurrently.
        }

        return findUserIdByEmail(email);
    }
}
