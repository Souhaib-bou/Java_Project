package Services;

import Models.Role;
import Utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleService {

    private final Connection cnx;

    public RoleService() {
        cnx = MyDB.getInstance().getConnection();
    }

    public List<Role> getAllRoles() throws SQLException {
        List<Role> list = new ArrayList<>();
<<<<<<< HEAD
        String sql = "SELECT role_id, role_name, status, dashboard, description FROM role";
=======
        String sql = "SELECT role_id, name, status, default_dashboard, description FROM role";
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Role r = new Role(
                    rs.getInt("role_id"),
<<<<<<< HEAD
                    rs.getString("role_name"),
                    rs.getString("status"),
                    rs.getString("dashboard"),
=======
                    rs.getString("name"),
                    rs.getString("status"),
                    rs.getString("default_dashboard"),
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
                    rs.getString("description")
            );
            list.add(r);
        }
        return list;
    }

    public Integer getRoleIdByName(String roleName) throws SQLException {
<<<<<<< HEAD
        String sql = "SELECT role_id FROM role WHERE LOWER(role_name) = LOWER(?) LIMIT 1";
=======
        String sql = "SELECT role_id FROM role WHERE LOWER(name) = LOWER(?) LIMIT 1";
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, roleName);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("role_id") : null;
    }

    public int addRole(Role r) throws SQLException {
<<<<<<< HEAD
        String sql = "INSERT INTO role (role_name, status, dashboard, description) VALUES (?, ?, ?, ?)";
=======
        String sql = "INSERT INTO role (name, status, default_dashboard, description) VALUES (?, ?, ?, ?)";
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
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

    public void updateRole(int roleId, Role r) throws SQLException {
<<<<<<< HEAD
        String sql = "UPDATE role SET role_name=?, status=?, dashboard=?, description=? WHERE role_id=?";
=======
        String sql = "UPDATE role SET name=?, status=?, default_dashboard=?, description=? WHERE role_id=?";
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, r.getRoleName());
        ps.setString(2, r.getStatus());
        ps.setString(3, r.getDashboard());
        ps.setString(4, r.getDescription());
        ps.setInt(5, roleId);
        ps.executeUpdate();
    }

    public void deleteRole(int roleId) throws SQLException {
        // if your FK is ON DELETE SET NULL, this is enough
        String sql = "DELETE FROM role WHERE role_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, roleId);
        ps.executeUpdate();
    }
}
