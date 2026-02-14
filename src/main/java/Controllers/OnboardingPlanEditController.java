package Controllers;

import Models.OnboardingPlan;
import Models.User;
import Services.PlanService;
import Services.UserService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class OnboardingPlanEditController implements Initializable {

    @FXML private ComboBox<User> cmbUser;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private DatePicker dpDeadline;

    private final PlanService planService = new PlanService();
    private final UserService userService = new UserService();

    private OnboardingPlan planToEdit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        cmbStatus.getItems().addAll("Pending", "In Progress", "Completed", "On Hold");

        try {
            List<User> users = userService.getAllUsers();
            cmbUser.setItems(FXCollections.observableArrayList(users));

            cmbUser.setConverter(new StringConverter<>() {
                @Override
                public String toString(User u) {
                    return (u == null) ? "" : u.getFirstName() + " " + u.getLastName();
                }
                @Override
                public User fromString(String s) { return null; }
            });

        } catch (SQLException e) {
            showError("Error", "Cannot load users: " + e.getMessage());
        }
    }

    public void setPlan(OnboardingPlan plan) {
        this.planToEdit = plan;

        cmbStatus.setValue(plan.getStatus());

        // FIX: no toInstant() on java.sql.Date
        if (plan.getDeadline() != null) {
            LocalDate localDate = new java.sql.Date(plan.getDeadline().getTime()).toLocalDate();
            dpDeadline.setValue(localDate);
        }

        // select user in combobox
        for (User u : cmbUser.getItems()) {
            if (u.getUserId() == plan.getUserId()) {
                cmbUser.setValue(u);
                break;
            }
        }
    }

    @FXML
    private void handleSave() {

        if (planToEdit == null) return;

        if (cmbUser.getValue() == null || cmbStatus.getValue() == null || dpDeadline.getValue() == null) {
            showWarning("Validation", "Select user, status and deadline.");
            return;
        }

        try {
            int userId = cmbUser.getValue().getUserId();

            Date deadline = Date.from(
                    dpDeadline.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()
            );

            OnboardingPlan updated = new OnboardingPlan(
                    planToEdit.getPlanId(),
                    userId,
                    cmbStatus.getValue(),
                    deadline
            );

            planService.updateOnboardingPlan(planToEdit.getPlanId(), updated);

            closeWindow();

        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cmbUser.getScene().getWindow();
        stage.close();
    }

    private void showError(String t, String c) {
        new Alert(Alert.AlertType.ERROR, c).showAndWait();
    }

    private void showWarning(String t, String c) {
        new Alert(Alert.AlertType.WARNING, c).showAndWait();
    }
}
