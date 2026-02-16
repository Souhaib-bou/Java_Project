package repo;

import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
}
