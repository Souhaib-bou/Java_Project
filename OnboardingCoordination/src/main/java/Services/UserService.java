package Services;

import Utils.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Connection cnx;

    /**
     * Creates a new UserService instance.
     */
    public UserService() {
        cnx = MyDB.getInstance().getConnection();
    }

    /**
     * Executes this operation.
     */
    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    /**
     * Sets the userstatus value.
     */
    public void setUserStatus(int userId, String status) throws SQLException {
        String sql = "UPDATE users SET status=? WHERE user_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, status);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    // ✅ UPDATED: includes profile_pic (optional)
    /**
     * Creates a new record and updates the UI.
     */
    public int addUser(User u) throws SQLException {
        String sql =
                "INSERT INTO users (first_name, last_name, email, password, role_id, status, profile_pic) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, u.getFirstName());
        ps.setString(2, u.getLastName());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getPassword());

        if (u.getRoleId() == null) ps.setNull(5, Types.INTEGER);
        else ps.setInt(5, u.getRoleId());

        ps.setString(6, u.getStatus() == null ? "active" : u.getStatus());

        if (u.getProfilePic() == null || u.getProfilePic().trim().isEmpty())
            ps.setNull(7, Types.VARCHAR);
        else
            ps.setString(7, u.getProfilePic().trim());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) return rs.getInt(1);
        throw new SQLException("Failed to retrieve generated user_id.");
    }

    // ✅ already good (kept)
    /**
     * Updates the selected record and refreshes the UI.
     */
    public void updateUser(int userId, User u) throws SQLException {
        String sql = "UPDATE users SET first_name=?, last_name=?, email=?, password=?, role_id=?, status=?, profile_pic=? WHERE user_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, u.getFirstName());
        ps.setString(2, u.getLastName());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getPassword());

        if (u.getRoleId() == null) ps.setNull(5, Types.INTEGER);
        else ps.setInt(5, u.getRoleId());

        ps.setString(6, u.getStatus() == null ? "active" : u.getStatus());

        if (u.getProfilePic() == null || u.getProfilePic().trim().isEmpty())
            ps.setNull(7, Types.VARCHAR);
        else
            ps.setString(7, u.getProfilePic().trim());

        ps.setInt(8, userId);
        ps.executeUpdate();
    }

    /**
     * Deletes the selected record and refreshes the UI.
     */
    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    /**
     * Returns users with joined role name for display.
     */
    /**
     * Returns the allusers value.
     */
    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql =
                "SELECT u.user_id, u.first_name, u.last_name, u.email, u.password, u.role_id, u.status, u.profile_pic, " +
                        "       r.name AS role_name " +
                        "FROM users u " +
                        "LEFT JOIN role r ON r.role_id = u.role_id " +
                        "ORDER BY u.user_id DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            User u = new User(
                    rs.getInt("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    (Integer) rs.getObject("role_id"),
                    rs.getString("status")
            );
            u.setRoleName(rs.getString("role_name"));
            u.setProfilePic(rs.getString("profile_pic"));
            list.add(u);
        }
        return list;
    }

    /**
     * Returns the userbyid value.
     */
    public User getUserById(int userId) throws SQLException {
        String sql =
                "SELECT user_id, first_name, last_name, email, password, role_id, status, profile_pic " +
                        "FROM users WHERE user_id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            User u = new User(
                    rs.getInt("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    (Integer) rs.getObject("role_id"),
                    rs.getString("status")
            );
            u.setProfilePic(rs.getString("profile_pic"));
            return u;
        }
        return null;
    }

    /**
     * Executes this operation.
     */
    public User findByEmail(String email) throws SQLException {
        String sql =
                "SELECT u.user_id, u.first_name, u.last_name, u.email, u.password, u.role_id, u.status, u.profile_pic, " +
                        "       r.name AS role_name " +
                        "FROM users u " +
                        "LEFT JOIN role r ON r.role_id = u.role_id " +
                        "WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(?)) " +
                        "LIMIT 1";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email == null ? "" : email.trim());
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            User u = new User(
                    rs.getInt("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    (Integer) rs.getObject("role_id"),
                    rs.getString("status")
            );
            u.setRoleName(rs.getString("role_name"));
            u.setProfilePic(rs.getString("profile_pic"));
            return u;
        }
        return null;
    }
}
