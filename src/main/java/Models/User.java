package Models;

public class User {
    private int userId;
    private String firstName;
    private String lastName;
    private String email;
    private String password;  // plain (dev)
    private Integer roleId;   // nullable
    private String status;    // "active" | "inactive"

    // convenience fields for table display (optional)
    private String roleName;

    public User() {}

    public User(int userId, String firstName, String lastName, String email,
                String password, Integer roleId, String status) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.roleId = roleId;
        this.status = status;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getFullName() {
        String fn = firstName == null ? "" : firstName;
        String ln = lastName == null ? "" : lastName;
        return (fn + " " + ln).trim();
    }

    @Override
    public String toString() {
        String name = getFullName();
        return name.isEmpty() ? ("User #" + userId) : name;
    }
}
