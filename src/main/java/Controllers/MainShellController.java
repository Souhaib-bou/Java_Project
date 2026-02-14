package Controllers;

import Models.User;
import Utils.UserSession;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class MainShellController implements Initializable {

    @FXML private StackPane contentHost;

    @FXML private Label lblHeaderUserName;
    @FXML private Label lblHeaderInitials;

    @FXML private Label lblPageTitle;
    @FXML private Label lblPageSubtitle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshHeader();

        // IMPORTANT: during initialize(), Scene/Window can be null.
        // So: build content now, set window title later.
        openHome();

        // If you prefer onboarding first, replace openHome() with openPlans()
        // openPlans();
    }

    private void refreshHeader() {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;

        String full = u.getFullName();
        lblHeaderUserName.setText(full == null || full.trim().isEmpty() ? "User" : full);

        String fn = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String ln = u.getLastName() == null ? "" : u.getLastName().trim();

        String i1 = fn.isEmpty() ? "U" : fn.substring(0, 1).toUpperCase();
        String i2 = ln.isEmpty() ? "" : ln.substring(0, 1).toUpperCase();
        String initials = (i1 + i2).trim();

        lblHeaderInitials.setText(initials.isEmpty() ? "U" : initials);
    }

    private void setPageMeta(String title, String subtitle) {
        lblPageTitle.setText(title);
        lblPageSubtitle.setText(subtitle);

        // ✅ set title safely AFTER Scene exists
        Platform.runLater(() -> {
            if (contentHost != null && contentHost.getScene() != null) {
                Stage stage = (Stage) contentHost.getScene().getWindow();
                if (stage != null) stage.setTitle("Hirely — " + title);
            }
        });
    }

    // ===== HOME =====

    private void openHome() {
        VBox home = new VBox(10);
        home.getStyleClass().add("card");

        Label t = new Label("Home");
        t.getStyleClass().add("section-title");

        Label s = new Label("Use the Onboarding button to manage plans and tasks.");
        s.getStyleClass().add("footer-label");

        home.getChildren().addAll(t, s);

        contentHost.getChildren().setAll(home);
        setPageMeta("Home", "Welcome");
    }

    @FXML
    private void handleOpenHome() {
        openHome();
    }

    // ===== ONBOARDING =====

    public void openPlans() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/OnboardingPlanView.fxml"));
            Parent root = loader.load();

            OnboardingPlanController controller = loader.getController();
            controller.setShell(this); // ✅ give it navigation ability

            contentHost.getChildren().setAll(root);
            setPageMeta("Onboarding Plans", "Assign plans and track progress");

            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.setTitle("Hirely — Onboarding Plans");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void openTasks(int planId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/OnboardingTaskView.fxml"));
            Parent root = loader.load();

            OnboardingTaskController taskController = loader.getController();
            taskController.setPlanContext(planId);

            // allow "Back"
            taskController.setOnBack(this::openPlans);

            contentHost.getChildren().setAll(root);
            setPageMeta("Tasks", "Manage tasks for plan #" + planId);

            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.setTitle("Hirely — Tasks (Plan #" + planId + ")");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Cannot load tasks view. Check console.").showAndWait();
        }
    }
    @FXML
    private void handleOpenJobOffers() {
        openJobOffers();
    }

    public void openJobOffers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JobOfferView.fxml"));
            Parent root = loader.load();

            JobOfferController controller = loader.getController();
            controller.setShell(this); // ✅ give navigation ability

            contentHost.getChildren().setAll(root);
            setPageMeta("Job Offers", "Create and manage job offers");

            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.setTitle("Hirely — Job Offers");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Cannot load JobOfferView.fxml. Check console.").showAndWait();
        }
    }

    public void openApplications() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ApplicationView.fxml"));
            Parent root = loader.load();

            ApplicationController controller = loader.getController();
            controller.setShell(this);

            contentHost.getChildren().setAll(root);
            setPageMeta("Applications", "Manage applications");

            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.setTitle("Hirely — Applications");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Cannot load ApplicationView.fxml. Check console.").showAndWait();
        }
    }


    @FXML
    private void handleOpenOnboarding() {
        openPlans();
    }

    // ===== PROFILE =====

    @FXML
    private void handleOpenProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserProfileView.fxml"));
            Parent root = loader.load();

            UserProfileController controller = loader.getController();
            controller.setShell(this);

            contentHost.getChildren().setAll(root);
            setPageMeta("My Profile", "Manage your account information");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void backToPlans() {
        refreshHeader();
        openPlans();
    }

    public void refreshShellUserChip() {
        refreshHeader();
    }

    // ===== LOGOUT =====

    @FXML
    public void handleLogout() {
        try {
            UserSession.getInstance().clear();

            Parent loginRoot = FXMLLoader.load(getClass().getResource("/LoginView.fxml"));

            // here we are already inside a shown Scene, safe to access window
            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
            stage.setTitle("Hirely — Login");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
