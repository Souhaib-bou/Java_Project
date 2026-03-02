package Services;

import Models.User;
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
<<<<<<< HEAD
    public User findById(int id) throws SQLException {

        String sql = "SELECT * FROM Users WHERE user_id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {

            User u = new User();

            u.setUserId(rs.getInt("user_id"));
            u.setFirstName(rs.getString("first_name"));
            u.setLastName(rs.getString("last_name"));
            u.setEmail(rs.getString("email"));
            u.setPassword(rs.getString("password"));
            u.setRoleId(rs.getInt("role_id"));
            u.setStatus(rs.getString("status"));

            return u;
        }

        return null;
    }
=======
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67

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
                "SELECT u.user_id, u.first_name, u.last_name, u.email, u.password, " +
                        "       u.role_id, u.status, u.profile_pic, " +
                        "       r.name AS role_name " +
                        "FROM users u " +
                        "LEFT JOIN role r ON r.role_id = u.role_id " +
                        "WHERE u.user_id = ?";

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
            u.setRoleName(rs.getString("role_name")); // ✅ VERY IMPORTANT

            return u;
        }

        return null;
    }

    /**
     * Executes this operation.
     */
<<<<<<< HEAD
=======
    public User findByGoogleId(String googleId) throws SQLException {
        String sql =
                "SELECT u.user_id, u.first_name, u.last_name, u.email, u.password, " +
                "       u.role_id, u.status, u.profile_pic, u.google_id, u.face_data, " +
                "       r.name AS role_name " +
                "FROM users u " +
                "LEFT JOIN role r ON r.role_id = u.role_id " +
                "WHERE u.google_id = ? LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, googleId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            User u = new User(
                    rs.getInt("user_id"), rs.getString("first_name"),
                    rs.getString("last_name"), rs.getString("email"),
                    rs.getString("password"), (Integer) rs.getObject("role_id"),
                    rs.getString("status")
            );
            u.setProfilePic(rs.getString("profile_pic"));
            u.setGoogleId(rs.getString("google_id"));
            u.setFaceData(rs.getString("face_data"));
            u.setRoleName(rs.getString("role_name"));
            return u;
        }
        return null;
    }

    public int addGoogleUser(String googleId, String email,
                             String firstName, String lastName) throws SQLException {
        String sql =
                "INSERT INTO users (first_name, last_name, email, password, google_id, status) " +
                "VALUES (?, ?, ?, '', ?, 'active')";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, firstName);
        ps.setString(2, lastName);
        ps.setString(3, email);
        ps.setString(4, googleId);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) return rs.getInt(1);
        throw new SQLException("Failed to retrieve generated user_id for Google user.");
    }

    public void saveFaceData(int userId, String faceData) throws SQLException {
        String sql = "UPDATE users SET face_data = ? WHERE user_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, faceData);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    public List<User> getAllUsersWithFaceData() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql =
                "SELECT user_id, first_name, last_name, email, password, role_id, status, " +
                "       profile_pic, face_data " +
                "FROM users WHERE face_data IS NOT NULL AND face_data != ''";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            User u = new User(
                    rs.getInt("user_id"), rs.getString("first_name"),
                    rs.getString("last_name"), rs.getString("email"),
                    rs.getString("password"), (Integer) rs.getObject("role_id"),
                    rs.getString("status")
            );
            u.setFaceData(rs.getString("face_data"));
            list.add(u);
        }
        return list;
    }

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
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
