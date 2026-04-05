package Controllers;

import Models.Role;
import Services.RoleService;
<<<<<<< HEAD
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
=======
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;

public class RoleController {

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
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

<<<<<<< HEAD
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
=======
    private final RoleService roleService = new RoleService();
    private final ObservableList<Role> roleList = FXCollections.observableArrayList();

<<<<<<< HEAD
    private MainShellController shell;

=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
    @FXML
    public void initialize() {

        cmbStatus.setItems(FXCollections.observableArrayList("Active", "Inactive"));

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        colId.setCellValueFactory(new PropertyValueFactory<>("roleId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDashboard.setCellValueFactory(new PropertyValueFactory<>("dashboard"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
<<<<<<< HEAD
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
=======
        addFieldValidation();
        loadRoles();

        roleTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        fillFields(newSelection);
                    }
                }
        );
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    }

    private void loadRoles() {
        try {
            roleList.clear();
            roleList.addAll(roleService.getAllRoles());
            roleTable.setItems(roleList);
        } catch (SQLException e) {
<<<<<<< HEAD
            showMessage("Error loading roles: " + e.getMessage(), true);
=======
            lblMessage.setText("Error loading roles.");
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        }
    }

    private void fillFields(Role r) {
        txtRoleName.setText(r.getRoleName());
        cmbStatus.setValue(r.getStatus());
        txtDashboard.setText(r.getDashboard());
        txtDescription.setText(r.getDescription());
    }

<<<<<<< HEAD
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

=======
    @FXML
    private void handleAdd() {
        if (!validateInputs()) return;
        try {
            Role r = new Role(
                    0,
                    txtRoleName.getText(),
                    cmbStatus.getValue(),
                    txtDashboard.getText(),
                    txtDescription.getText()
            );

            roleService.addRole(r);
            lblMessage.setText("Role added successfully.");
            loadRoles();
            handleClear();

        } catch (SQLException e) {
            lblMessage.setText("Error adding role.");
        }
    }

    private void addFieldValidation() {

        txtRoleName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                txtRoleName.setStyle("-fx-border-color: red;");
            } else {
                txtRoleName.setStyle(null);
            }
        });
    }
<<<<<<< HEAD
    public void setShell(MainShellController shell) {
        this.shell = shell;
    }
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    @FXML
    private void handleUpdate() {
        Role selected = roleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
<<<<<<< HEAD
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
=======
            lblMessage.setText("Select a role first.");
            return;
        }
        if (!validateInputs()) return;

        try {
            selected.setRoleName(txtRoleName.getText());
            selected.setStatus(cmbStatus.getValue());
            selected.setDashboard(txtDashboard.getText());
            selected.setDescription(txtDescription.getText());

            roleService.updateRole(selected.getRoleId(), selected);
            lblMessage.setText("Role updated.");
            loadRoles();

        } catch (SQLException e) {
            lblMessage.setText("Error updating role.");
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        }
    }

    @FXML
    private void handleDelete() {
        Role selected = roleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
<<<<<<< HEAD
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
=======
            lblMessage.setText("Select a role first.");
            return;
        }

        try {
            roleService.deleteRole(selected.getRoleId());
            lblMessage.setText("Role deleted.");
            loadRoles();
            handleClear();

        } catch (SQLException e) {
            lblMessage.setText("Error deleting role.");
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        }
    }

    @FXML
    private void handleClear() {
        txtRoleName.clear();
        cmbStatus.setValue(null);
        txtDashboard.clear();
        txtDescription.clear();
        roleTable.getSelectionModel().clearSelection();
<<<<<<< HEAD
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
=======
    }

    private boolean validateInputs() {

        if (txtRoleName.getText() == null || txtRoleName.getText().trim().isEmpty()) {
            lblMessage.setText("Role name is required.");
            return false;
        }

        if (cmbStatus.getValue() == null || cmbStatus.getValue().trim().isEmpty()) {
            lblMessage.setText("Status is required.");
            return false;
        }

        if (txtDashboard.getText() == null || txtDashboard.getText().trim().isEmpty()) {
            lblMessage.setText("Dashboard is required.");
            return false;
        }

        if (txtDescription.getText() == null || txtDescription.getText().trim().isEmpty()) {
            lblMessage.setText("Description is required.");
            return false;
        }

        return true;
    }
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
}