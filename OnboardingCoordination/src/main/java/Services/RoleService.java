package Services;

import Utils.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleService {

    private final Connection cnx;

    /**
     * Creates a new RoleService instance.
     */
    public RoleService() {
        cnx = MyDB.getInstance().getConnection();
    }

    /**
     * Returns the allroles value.
     */
    public List<Role> getAllRoles() throws SQLException {
        List<Role> list = new ArrayList<>();
        String sql = "SELECT role_id, name, status, default_dashboard, description FROM role";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Role r = new Role(
                    rs.getInt("role_id"),
                    rs.getString("name"),
                    rs.getString("status"),
                    rs.getString("default_dashboard"),
                    rs.getString("description")
            );
            list.add(r);
        }
        return list;
    }

    /**
     * Returns the roleidbyname value.
     */
    public Integer getRoleIdByName(String roleName) throws SQLException {
        String sql = "SELECT role_id FROM role WHERE LOWER(name) = LOWER(?) LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, roleName);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("role_id") : null;
    }

    /**
     * Creates a new record and updates the UI.
     */
    public int addRole(Role r) throws SQLException {
        String sql = "INSERT INTO role (name, status, default_dashboard, description) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, r.getRoleName());
        ps.setString(2, r.getStatus());
        ps.setString(3, r.getDashboard());
        ps.setString(4, r.getDescription());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) return rs.getInt(1);
        throw new SQLException("Failed to retrieve generated role_id");
    }

    /**
     * Updates the selected record and refreshes the UI.
     */
    public void updateRole(int roleId, Role r) throws SQLException {
        String sql = "UPDATE role SET name=?, status=?, default_dashboard=?, description=? WHERE role_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, r.getRoleName());
        ps.setString(2, r.getStatus());
        ps.setString(3, r.getDashboard());
        ps.setString(4, r.getDescription());
        ps.setInt(5, roleId);
        ps.executeUpdate();
    }

    /**
     * Deletes the selected record and refreshes the UI.
     */
    public void deleteRole(int roleId) throws SQLException {
        // if your FK is ON DELETE SET NULL, this is enough
        String sql = "DELETE FROM role WHERE role_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, roleId);
        ps.executeUpdate();
    }
}
