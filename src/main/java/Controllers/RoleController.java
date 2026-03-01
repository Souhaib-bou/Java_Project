package Controllers;

import Models.Role;
import Services.RoleService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;

public class RoleController {

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

    private final RoleService roleService = new RoleService();
    private final ObservableList<Role> roleList = FXCollections.observableArrayList();

<<<<<<< HEAD
    private MainShellController shell;

=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
    @FXML
    public void initialize() {

        cmbStatus.setItems(FXCollections.observableArrayList("Active", "Inactive"));

        colId.setCellValueFactory(new PropertyValueFactory<>("roleId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDashboard.setCellValueFactory(new PropertyValueFactory<>("dashboard"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        addFieldValidation();
        loadRoles();

        roleTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        fillFields(newSelection);
                    }
                }
        );
    }

    private void loadRoles() {
        try {
            roleList.clear();
            roleList.addAll(roleService.getAllRoles());
            roleTable.setItems(roleList);
        } catch (SQLException e) {
            lblMessage.setText("Error loading roles.");
        }
    }

    private void fillFields(Role r) {
        txtRoleName.setText(r.getRoleName());
        cmbStatus.setValue(r.getStatus());
        txtDashboard.setText(r.getDashboard());
        txtDescription.setText(r.getDescription());
    }

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
    @FXML
    private void handleUpdate() {
        Role selected = roleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
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
        }
    }

    @FXML
    private void handleDelete() {
        Role selected = roleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
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
        }
    }

    @FXML
    private void handleClear() {
        txtRoleName.clear();
        cmbStatus.setValue(null);
        txtDashboard.clear();
        txtDescription.clear();
        roleTable.getSelectionModel().clearSelection();
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
}