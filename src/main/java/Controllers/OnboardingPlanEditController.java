package Controllers;

import Models.OnboardingPlan;
import Models.User;
import Services.PlanService;
import Services.UserService;
import Services.api.PlanApiService;
import Utils.UserSession;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class OnboardingPlanEditController implements Initializable {

    @FXML private ComboBox<User> cmbUser;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private DatePicker dpDeadline;

    // Keep JDBC for now for admin/recruiter full edit (we'll migrate later)
    private final PlanService planService = new PlanService();
    private final UserService userService = new UserService();

    // Use API for candidate status-only (enforces backend security)
    private final PlanApiService planApiService = new PlanApiService();

    private OnboardingPlan planToEdit;

    private boolean candidateMode = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // IMPORTANT: store enum values (DB/API), not pretty labels
        cmbStatus.getItems().setAll("pending", "in_progress", "completed", "on_hold");

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

    /** Call this before showing the window */
    public void setCandidateMode(boolean candidateMode) {
        this.candidateMode = candidateMode;

        // If FXML already loaded, apply immediately
        if (cmbUser != null) {
            cmbUser.setDisable(candidateMode);
            dpDeadline.setDisable(candidateMode);
        }
    }

    public void setPlan(OnboardingPlan plan) {
        this.planToEdit = plan;

        // Convert whatever is inside plan.status to enum-style
        String statusEnum = normalizeToEnum(plan.getStatus());
        cmbStatus.setValue(statusEnum);

        if (plan.getDeadline() != null) {
            LocalDate localDate = new java.sql.Date(plan.getDeadline().getTime()).toLocalDate();
            dpDeadline.setValue(localDate);
        }

        for (User u : cmbUser.getItems()) {
            if (u.getUserId() == plan.getUserId()) {
                cmbUser.setValue(u);
                break;
            }
        }

        // apply candidate mode after data is filled
        setCandidateMode(candidateMode);
    }

    @FXML
    private void handleSave() {
        if (planToEdit == null) return;

        if (cmbStatus.getValue() == null) {
            showWarning("Validation", "Select a status.");
            return;
        }

        // Candidate: status-only via backend
        if (candidateMode) {
            try {
                planApiService.updateStatus(planToEdit.getPlanId(), cmbStatus.getValue());
                closeWindow();
                return;
            } catch (RuntimeException ex) {
                showError("Not allowed", ex.getMessage());
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Error", ex.getMessage());
                return;
            }
        }

        // Admin/Recruiter: full edit (JDBC for now)
        if (cmbUser.getValue() == null || dpDeadline.getValue() == null) {
            showWarning("Validation", "Select user and deadline.");
            return;
        }

        try {
            int userId = cmbUser.getValue().getUserId();
            Date deadline = Date.from(dpDeadline.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());

            OnboardingPlan updated = new OnboardingPlan(
                    planToEdit.getPlanId(),
                    userId,
                    normalizeToEnum(cmbStatus.getValue()),
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

    // Accept both UI text ("In Progress") and enum ("in_progress")
    private String normalizeToEnum(String status) {
        if (status == null) return "pending";
        String s = status.trim().toLowerCase();

        return switch (s) {
            case "in progress", "in_progress" -> "in_progress";
            case "completed" -> "completed";
            case "on hold", "on_hold" -> "on_hold";
            default -> "pending";
        };
    }

    private void showError(String t, String c) {
        new Alert(Alert.AlertType.ERROR, c).showAndWait();
    }

    private void showWarning(String t, String c) {
        new Alert(Alert.AlertType.WARNING, c).showAndWait();
    }
}