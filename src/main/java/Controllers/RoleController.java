package Controllers;

import Models.Role;
import Services.RoleService;
import Utils.UserSession;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class RoleController {

    // ================= FXML FIELDS =================
    @FXML private TextField txtRoleName;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private TextField txtDashboard;
    @FXML private TextArea txtDescription;
    @FXML private Label lblMessage;

    @FXML private TableView<Role> roleTable;
    @FXML private TableColumn<Role, Integer> colId;
    @FXML private TableColumn<Role, String> colName;
    @FXML private TableColumn<Role, String> colStatus;
    @FXML private TableColumn<Role, String> colDashboard;
    @FXML private TableColumn<Role, String> colDescription;

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnBack;

    // ================= SERVICES & DATA =================
    private final RoleService roleService = new RoleService();
    private final ObservableList<Role> roleList = FXCollections.observableArrayList();
    private MainShellController shell;

    // ================= INITIALIZATION =================
    @FXML
    private void initialize() {
        setupComboBoxes();
        setupTableColumns();
        setupTableListener();
        loadRoles();

        // Disable update/delete initially
        btnUpdate.setDisable(true);
        btnDelete.setDisable(true);
    }

    /**
     * Sets the shell reference (optional for back navigation or shell updates)
     */
    public void setShell(MainShellController shell) {
        this.shell = shell;
    }

    private void setupComboBoxes() {
        cmbStatus.setItems(FXCollections.observableArrayList("Active", "Inactive"));
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("roleId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDashboard.setCellValueFactory(new PropertyValueFactory<>("dashboard"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    private void setupTableListener() {
        roleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                fillFields(newSel);
                btnUpdate.setDisable(false);
                btnDelete.setDisable(false);
            } else {
                btnUpdate.setDisable(true);
                btnDelete.setDisable(true);
            }
        });
    }

    private void loadRoles() {
        try {
            roleList.clear();
            roleList.addAll(roleService.getAllRoles());
            roleTable.setItems(roleList);
        } catch (SQLException e) {
            showMessage("Error loading roles: " + e.getMessage(), true);
        }
    }

    private void fillFields(Role r) {
        txtRoleName.setText(r.getRoleName());
        cmbStatus.setValue(r.getStatus());
        txtDashboard.setText(r.getDashboard());
        txtDescription.setText(r.getDescription());
    }

    // ================= HANDLERS =================
    @FXML
    private void handleAdd() {
        if (!validateInputs()) return;

        for (Role role : roleList) {
            if (role.getRoleName().equalsIgnoreCase(txtRoleName.getText().trim())) {
                showMessage("Role name already exists.", true);
                return;
            }
        }

        Role r = new Role(
                0,
                txtRoleName.getText().trim(),
                cmbStatus.getValue(),
                txtDashboard.getText().trim(),
                txtDescription.getText().trim()
        );

        try {
            roleService.addRole(r);
            showMessage("Role added successfully.", false);
            loadRoles();
            handleClear();
        } catch (SQLException e) {
            showMessage("Error adding role: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleUpdate() {
        Role selected = roleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a role first.", true);
            return;
        }

        if (!validateInputs()) return;

        selected.setRoleName(txtRoleName.getText().trim());
        selected.setStatus(cmbStatus.getValue());
        selected.setDashboard(txtDashboard.getText().trim());
        selected.setDescription(txtDescription.getText().trim());

        try {
            roleService.updateRole(selected.getRoleId(), selected);
            showMessage("Role updated successfully.", false);
            loadRoles();
        } catch (SQLException e) {
            showMessage("Error updating role: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        Role selected = roleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a role first.", true);
            return;
        }

        var confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this role?", ButtonType.OK, ButtonType.CANCEL)
                .showAndWait();

        if (confirm.isEmpty() || confirm.get() != ButtonType.OK) return;

        try {
            roleService.deleteRole(selected.getRoleId());
            showMessage("Role deleted successfully.", false);
            loadRoles();
            handleClear();
        } catch (SQLException e) {
            showMessage("Error deleting role: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleClear() {
        txtRoleName.clear();
        cmbStatus.setValue(null);
        txtDashboard.clear();
        txtDescription.clear();
        roleTable.getSelectionModel().clearSelection();
        btnUpdate.setDisable(true);
        btnDelete.setDisable(true);
        lblMessage.setText("");
    }

    @FXML
    private void handleBack() {
        if (shell != null) {
            shell.backToPlans();
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainShell.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = (Stage) txtRoleName.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Dashboard");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ================= HELPERS =================
    private boolean validateInputs() {
        if (txtRoleName.getText() == null || txtRoleName.getText().trim().isEmpty()) {
            showMessage("Role name is required.", true);
            return false;
        }
        if (cmbStatus.getValue() == null) {
            showMessage("Status is required.", true);
            return false;
        }
        if (txtDashboard.getText() == null || txtDashboard.getText().trim().isEmpty()) {
            showMessage("Dashboard is required.", true);
            return false;
        }
        if (txtDescription.getText() == null || txtDescription.getText().trim().isEmpty()) {
            showMessage("Description is required.", true);
            return false;
        }
        return true;
    }

    private void showMessage(String message, boolean isError) {
        lblMessage.setText(message);
        lblMessage.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}