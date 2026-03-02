package Models;

public class Role {
    private int roleId;
    private String roleName;
    private String status;
    private String dashboard;
    private String description;

    public Role() {}

    public Role(int roleId, String roleName, String status, String dashboard, String description) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.status = status;
        this.dashboard = dashboard;
        this.description = description;
    }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDashboard() { return dashboard; }
    public void setDashboard(String dashboard) { this.dashboard = dashboard; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return roleName;
    }
}
