package Controllers;

import Models.User;
import Services.UserService;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class UserModuleController implements Initializable {

    // ===== USERS form =====
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<Role> cmbRole;
    @FXML private ComboBox<String> cmbUserStatus;

    // ===== USERS table =====
    @FXML private TableView<User> tvUsers;
    @FXML private TableColumn<User, Integer> colUserId;
    @FXML private TableColumn<User, String> colUserName;
    @FXML private TableColumn<User, String> colUserEmail;
    @FXML private TableColumn<User, String> colUserRole;
    @FXML private TableColumn<User, String> colUserStatus;

    @FXML private Label lblUserMsg;

    // ===== ROLES form =====
    @FXML private TextField txtRoleName;
    @FXML private ComboBox<String> cmbRoleStatus;
    @FXML private TextField txtRoleDashboard;
    @FXML private TextArea txtRoleDesc;

    // ===== ROLES table =====
    @FXML private TableView<Role> tvRoles;
    @FXML private TableColumn<Role, Integer> colRoleId;
    @FXML private TableColumn<Role, String> colRoleName;
    @FXML private TableColumn<Role, String> colRoleStatus;
    @FXML private TableColumn<Role, String> colRoleDashboard;
    @FXML private TableColumn<Role, String> colRoleDesc;

    @FXML private Label lblRoleMsg;

    private final UserService userService = new UserService();
    private final RoleService roleService = new RoleService();

    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    private final ObservableList<Role> rolesList = FXCollections.observableArrayList();

    private User selectedUser;
    private Role selectedRole;

    @Override
    /**
     * Initializes UI components and loads initial data.
     */
    public void initialize(URL location, ResourceBundle resources) {

        // combos
        cmbUserStatus.setItems(FXCollections.observableArrayList("active", "inactive"));
        cmbUserStatus.setValue("active");

        cmbRoleStatus.setItems(FXCollections.observableArrayList("active", "inactive"));
        cmbRoleStatus.setValue("active");

        // users table
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUserName.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().getFullName()));
        colUserEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserRole.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(
                cell.getValue().getRoleName() == null ? "-" : cell.getValue().getRoleName()
        ));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tvUsers.setItems(usersList);

        // roles table
        colRoleId.setCellValueFactory(new PropertyValueFactory<>("roleId"));
        colRoleName.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        colRoleStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colRoleDashboard.setCellValueFactory(new PropertyValueFactory<>("dashboard"));
        colRoleDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        tvRoles.setItems(rolesList);


        // selection listeners
        tvUsers.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selectedUser = n;
            if (n != null) fillUserForm(n);
        });

        tvRoles.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selectedRole = n;
            if (n != null) fillRoleForm(n);
        });

        // load data
        reloadRoles();
        reloadUsers();
    }

    /**
     * Executes this operation.
     */
    private void reloadUsers() {
        try {
            List<User> users = userService.getAllUsers();
            usersList.setAll(users);
            lblUserMsg.setText("Loaded " + users.size() + " users");
        } catch (SQLException e) {
            lblUserMsg.setText("Failed to load users");
            showError("DB Error", e.getMessage());
        }
    }

    /**
     * Executes this operation.
     */
    private void reloadRoles() {
        try {
            List<Role> roles = roleService.getAllRoles();
            rolesList.setAll(roles);
            cmbRole.setItems(rolesList);
            lblRoleMsg.setText("Loaded " + roles.size() + " roles");
        } catch (SQLException e) {
            lblRoleMsg.setText("Failed to load roles");
            showError("DB Error", e.getMessage());
        }
    }

    // ========= USERS =========

    /**
     * Executes this operation.
     */
    private void fillUserForm(User u) {
        txtFirstName.setText(u.getFirstName());
        txtLastName.setText(u.getLastName());
        txtEmail.setText(u.getEmail());
        txtPassword.setText(u.getPassword());
        cmbUserStatus.setValue(u.getStatus() == null ? "active" : u.getStatus());

        // select role in combo
        if (u.getRoleId() == null) {
            cmbRole.setValue(null);
        } else {
            for (Role r : rolesList) {
                if (r.getRoleId() == u.getRoleId()) {
                    cmbRole.setValue(r);
                    break;
                }
            }
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleClearUser() {
        txtFirstName.clear();
        txtLastName.clear();
        txtEmail.clear();
        txtPassword.clear();
        cmbRole.setValue(null);
        cmbUserStatus.setValue("active");
        selectedUser = null;
        tvUsers.getSelectionModel().clearSelection();
        lblUserMsg.setText("Ready");
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleAddUser() {
        if (!validateUserForm()) return;

        try {
            Integer roleId = (cmbRole.getValue() == null) ? null : cmbRole.getValue().getRoleId();

            User u = new User(
                    0,
                    txtFirstName.getText().trim(),
                    txtLastName.getText().trim(),
                    txtEmail.getText().trim(),
                    txtPassword.getText(),
                    roleId,
                    cmbUserStatus.getValue()
            );

            int id = userService.addUser(u);
            showInfo("Success", "User added (ID: " + id + ")");
            reloadUsers();
            handleClearUser();

        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleUpdateUser() {
        if (selectedUser == null) {
            showWarning("No selection", "Select a user first.");
            return;
        }
        if (!validateUserForm()) return;

        try {
            Integer roleId = (cmbRole.getValue() == null) ? null : cmbRole.getValue().getRoleId();

            User updated = new User(
                    selectedUser.getUserId(),
                    txtFirstName.getText().trim(),
                    txtLastName.getText().trim(),
                    txtEmail.getText().trim(),
                    txtPassword.getText(),
                    roleId,
                    cmbUserStatus.getValue()
            );

            userService.updateUser(selectedUser.getUserId(), updated);
            showInfo("Success", "User updated.");
            reloadUsers();

        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleDeleteUser() {
        if (selectedUser == null) {
            showWarning("No selection", "Select a user first.");
            return;
        }

        var res = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user " + selectedUser.getFullName() + " ?",
                ButtonType.OK, ButtonType.CANCEL).showAndWait();

        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                userService.deleteUser(selectedUser.getUserId());
                showInfo("Success", "User deleted.");
                reloadUsers();
                handleClearUser();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        }
    }

    /**
     * Executes this operation.
     */
    private boolean validateUserForm() {
        String fn = txtFirstName.getText() == null ? "" : txtFirstName.getText().trim();
        String ln = txtLastName.getText() == null ? "" : txtLastName.getText().trim();
        String em = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        String pw = txtPassword.getText() == null ? "" : txtPassword.getText();

        if (fn.isEmpty() || ln.isEmpty() || em.isEmpty() || pw.isEmpty()) {
            showWarning("Validation", "First name, last name, email and password are required.");
            return false;
        }
        if (!em.contains("@") || !em.contains(".")) {
            showWarning("Validation", "Please enter a valid email.");
            return false;
        }
        return true;
    }

    // ========= ROLES =========

    /**
     * Executes this operation.
     */
    private void fillRoleForm(Role r) {
        txtRoleName.setText(r.getRoleName());
        cmbRoleStatus.setValue(r.getStatus() == null ? "active" : r.getStatus());
        txtRoleDashboard.setText(r.getDashboard());
        txtRoleDesc.setText(r.getDescription());
    }


    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleClearRole() {
        txtRoleName.clear();
        cmbRoleStatus.setValue("active");
        txtRoleDashboard.clear();
        txtRoleDesc.clear();
        selectedRole = null;
        tvRoles.getSelectionModel().clearSelection();
        lblRoleMsg.setText("Ready");
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleAddRole() {
        String name = txtRoleName.getText() == null ? "" : txtRoleName.getText().trim();
        if (name.isEmpty()) {
            showWarning("Validation", "Role name is required.");
            return;
        }

        try {
            Role r = new Role(
                    0,
                    name,
                    cmbRoleStatus.getValue(),
                    txtRoleDashboard.getText(),
                    txtRoleDesc.getText()
            );

            int id = roleService.addRole(r);
            showInfo("Success", "Role added (ID: " + id + ")");
            reloadRoles();
            handleClearRole();

        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }
    }


    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleUpdateRole() {
        if (selectedRole == null) {
            showWarning("No selection", "Select a role first.");
            return;
        }

        String name = txtRoleName.getText() == null ? "" : txtRoleName.getText().trim();
        if (name.isEmpty()) {
            showWarning("Validation", "Role name is required.");
            return;
        }

        try {
            Role updated = new Role(
                    selectedRole.getRoleId(),
                    name,
                    cmbRoleStatus.getValue(),
                    txtRoleDashboard.getText(),
                    txtRoleDesc.getText()
            );

            roleService.updateRole(selectedRole.getRoleId(), updated);
            showInfo("Success", "Role updated.");
            reloadRoles();

        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }
    }


    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleDeleteRole() {
        if (selectedRole == null) {
            showWarning("No selection", "Select a role first.");
            return;
        }

        var res = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete role " + selectedRole.getRoleName() + " ?\nUsers will have role_id set to NULL.",
                ButtonType.OK, ButtonType.CANCEL).showAndWait();

        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                roleService.deleteRole(selectedRole.getRoleId());
                showInfo("Success", "Role deleted.");
                reloadRoles();
                reloadUsers();
                handleClearRole();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        }
    }


    // ========= dialogs =========

    /**
     * Executes this operation.
     */
    private void showInfo(String t, String c) {
        new Alert(Alert.AlertType.INFORMATION, c).showAndWait();
    }

    /**
     * Executes this operation.
     */
    private void showWarning(String t, String c) {
        new Alert(Alert.AlertType.WARNING, c).showAndWait();
    }

    /**
     * Executes this operation.
     */
    private void showError(String t, String c) {
        new Alert(Alert.AlertType.ERROR, c).showAndWait();
    }
}
