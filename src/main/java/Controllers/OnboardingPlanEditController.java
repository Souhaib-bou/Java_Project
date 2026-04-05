package Controllers;

import Models.OnboardingPlan;
import Models.User;
import Services.PlanService;
<<<<<<< HEAD
import Services.UserService;
import Services.api.PlanApiService;
import Utils.UserSession;
=======

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
<<<<<<< HEAD
=======

import Services.UserService;
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
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

<<<<<<< HEAD
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
=======
    private final PlanService planService = new PlanService();
    private final UserService userService = new UserService();

    private OnboardingPlan planToEdit;

    @Override
    /**
     * Initializes UI components and loads initial data.
     */
    public void initialize(URL location, ResourceBundle resources) {

        cmbStatus.getItems().addAll("Pending", "In Progress", "Completed", "On Hold");
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67

        try {
            List<User> users = userService.getAllUsers();
            cmbUser.setItems(FXCollections.observableArrayList(users));

            cmbUser.setConverter(new StringConverter<>() {
                @Override
<<<<<<< HEAD
=======
                /**
                 * Executes this operation.
                 */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
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

<<<<<<< HEAD
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

=======
    /**
     * Sets the plan value.
     */
    public void setPlan(OnboardingPlan plan) {
        this.planToEdit = plan;

        cmbStatus.setValue(plan.getStatus());

        // FIX: no toInstant() on java.sql.Date
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        if (plan.getDeadline() != null) {
            LocalDate localDate = new java.sql.Date(plan.getDeadline().getTime()).toLocalDate();
            dpDeadline.setValue(localDate);
        }

<<<<<<< HEAD
=======
        // select user in combobox
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        for (User u : cmbUser.getItems()) {
            if (u.getUserId() == plan.getUserId()) {
                cmbUser.setValue(u);
                break;
            }
        }
<<<<<<< HEAD

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
=======
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleSave() {

        if (planToEdit == null) return;

        if (cmbUser.getValue() == null || cmbStatus.getValue() == null || dpDeadline.getValue() == null) {
            showWarning("Validation", "Select user, status and deadline.");
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
            return;
        }

        try {
            int userId = cmbUser.getValue().getUserId();
<<<<<<< HEAD
            Date deadline = Date.from(dpDeadline.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
=======

            Date deadline = Date.from(
                    dpDeadline.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67

            OnboardingPlan updated = new OnboardingPlan(
                    planToEdit.getPlanId(),
                    userId,
<<<<<<< HEAD
                    normalizeToEnum(cmbStatus.getValue()),
=======
                    cmbStatus.getValue(),
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
                    deadline
            );

            planService.updateOnboardingPlan(planToEdit.getPlanId(), updated);
<<<<<<< HEAD
=======

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
            closeWindow();

        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
        }
    }

    @FXML
<<<<<<< HEAD
=======
    /**
     * Handles the associated UI event.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private void handleCancel() {
        closeWindow();
    }

<<<<<<< HEAD
=======
    /**
     * Executes this operation.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private void closeWindow() {
        Stage stage = (Stage) cmbUser.getScene().getWindow();
        stage.close();
    }

<<<<<<< HEAD
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

=======
    /**
     * Executes this operation.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private void showError(String t, String c) {
        new Alert(Alert.AlertType.ERROR, c).showAndWait();
    }

<<<<<<< HEAD
    private void showWarning(String t, String c) {
        new Alert(Alert.AlertType.WARNING, c).showAndWait();
    }
}
=======
    /**
     * Executes this operation.
     */
    private void showWarning(String t, String c) {
        new Alert(Alert.AlertType.WARNING, c).showAndWait();
    }
}
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
