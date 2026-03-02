package Controllers;

import Models.User;
import Services.AuthService;
<<<<<<< HEAD
import Utils.UserSession;
=======
import Services.GoogleAuthService;
import Utils.UserSession;
import javafx.application.Platform;
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

<<<<<<< HEAD
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMsg;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        try {
            // 1) AUTH ONLY
=======
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblMsg;

    private final AuthService       authService = new AuthService();
    private final GoogleAuthService googleAuth  = new GoogleAuthService();

    // ── Standard login ───────────────────────────────────────────────────────

    @FXML
    private void handleLogin() {
        String email    = txtEmail.getText();
        String password = txtPassword.getText();

        try {
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
            User u = authService.login(email, password);
            if (u == null) {
                lblMsg.setText("Invalid email/password or account inactive.");
                return;
            }
<<<<<<< HEAD

            UserSession.getInstance().setCurrentUser(u);

=======
            UserSession.getInstance().setCurrentUser(u);
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Authentication error: " + e.getMessage());
            return;
        }

<<<<<<< HEAD
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

=======
        loadMainShell();
    }

    // ── Signup ───────────────────────────────────────────────────────────────

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    @FXML
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
<<<<<<< HEAD
=======

    // ── Forgot password ──────────────────────────────────────────────────────

    @FXML
    private void handleForgotPassword() {
        try {
            Parent fpRoot = FXMLLoader.load(getClass().getResource("/ForgotPasswordView.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.getScene().setRoot(fpRoot);
            stage.setTitle("Hirely — Reset Password");
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Cannot open password reset: " + e.getMessage());
        }
    }

    // ── Google OAuth login ───────────────────────────────────────────────────

    @FXML
    private void handleGoogleLogin() {
        lblMsg.setText("Opening browser for Google sign-in…");
        new Thread(() -> {
            try {
                User u = googleAuth.authenticateWithGoogle();
                if (u == null) {
                    Platform.runLater(() -> lblMsg.setText("Google sign-in cancelled or failed."));
                    return;
                }
                UserSession.getInstance().setCurrentUser(u);
                Platform.runLater(this::loadMainShell);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblMsg.setText("Google sign-in error: " + e.getMessage()));
            }
        }, "google-auth-thread").start();
    }

    // ── Face login ───────────────────────────────────────────────────────────

    @FXML
    private void handleFaceLogin() {
        try {
            Parent faceRoot = FXMLLoader.load(getClass().getResource("/FaceLoginView.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.getScene().setRoot(faceRoot);
            stage.setTitle("Hirely — Face Login");
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Cannot open face login: " + e.getMessage());
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void loadMainShell() {
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
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
}
