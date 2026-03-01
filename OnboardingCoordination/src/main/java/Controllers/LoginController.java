package Controllers;

import Models.User;
import Services.AuthService;
import Utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMsg;

    private final AuthService authService = new AuthService();

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleLogin() {
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        try {
            // 1) AUTH ONLY
            User u = authService.login(email, password);
            if (u == null) {
                lblMsg.setText("Invalid email/password or account inactive.");
                return;
            }

            UserSession.getInstance().setCurrentUser(u);

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Authentication error: " + e.getMessage());
            return;
        }

        // 2) UI LOAD ONLY (this is where it often fails)
        try {
            Parent shellRoot = FXMLLoader.load(getClass().getResource("/MainShell.fxml"));

            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.getScene().setRoot(shellRoot);
            stage.setTitle("Hirely — Onboarding Plans");

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Login OK, but cannot open MainShell.fxml: " + e.getMessage());
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleOpenSignup() {
        try {
            Parent signupRoot = FXMLLoader.load(getClass().getResource("/SignupView.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.getScene().setRoot(signupRoot);
            stage.setTitle("Hirely — Sign Up");
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Cannot open Sign Up: " + e.getMessage());
        }
    }
}
