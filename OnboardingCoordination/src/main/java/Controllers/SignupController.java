package Controllers;

import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SignupController {

    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirm;

    @FXML private ComboBox<String> cmbStatus;
    @FXML private ComboBox<Role> cmbRole;

    @FXML private Label lblMsg;

    private final UserService userService = new UserService();
    private final RoleService roleService = new RoleService();

    @FXML
    /**
     * Initializes UI components and loads initial data.
     */
    private void initialize() {

        // Status
        if (cmbStatus != null) {
            cmbStatus.getItems().setAll("active", "inactive");
            cmbStatus.setValue("active");
        }

        // Load roles from DB
        try {
            cmbRole.setItems(
                    FXCollections.observableArrayList(roleService.getAllRoles())
            );
        } catch (SQLException e) {
            e.printStackTrace();
            lblMsg.setText("Cannot load roles.");
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleSignup() {

        String fn = txtFirstName.getText() == null ? "" : txtFirstName.getText().trim();
        String ln = txtLastName.getText() == null ? "" : txtLastName.getText().trim();
        String em = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        String pw = txtPassword.getText() == null ? "" : txtPassword.getText();
        String cf = txtConfirm.getText() == null ? "" : txtConfirm.getText();

        // ✅ Validate role first
        Role selectedRole = cmbRole.getValue();
        if (selectedRole == null) {
            lblMsg.setText("Please select a role.");
            return;
        }

        // ✅ Validate fields
        if (fn.isEmpty() || ln.isEmpty() || em.isEmpty() || pw.isEmpty() || cf.isEmpty()) {
            lblMsg.setText("Please fill all fields.");
            return;
        }

        if (!em.contains("@") || !em.contains(".")) {
            lblMsg.setText("Please enter a valid email.");
            return;
        }

        if (!pw.equals(cf)) {
            lblMsg.setText("Passwords do not match.");
            return;
        }

        if (pw.length() < 4) {
            lblMsg.setText("Password is too short.");
            return;
        }

        String status = (cmbStatus == null || cmbStatus.getValue() == null)
                ? "active"
                : cmbStatus.getValue();

        try {

            if (userService.existsByEmail(em)) {
                lblMsg.setText("Email already exists. Use another one.");
                return;
            }

            // ✅ Get roleId dynamically
            int roleId = selectedRole.getRoleId();

            User newUser = new User(
                    0,
                    fn,
                    ln,
                    em,
                    pw,
                    roleId,   // 🔥 dynamic role
                    status
            );

            int id = userService.addUser(newUser);

            lblMsg.setText("Account created (ID: " + id + "). You can login now.");

            handleBackToLogin();

        } catch (SQLException e) {
            e.printStackTrace();
            lblMsg.setText("Signup failed: " + e.getMessage());
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleBackToLogin() {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/LoginView.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
            stage.setTitle("Hirely — Login");
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Cannot open Login: " + e.getMessage());
        }
    }
}
