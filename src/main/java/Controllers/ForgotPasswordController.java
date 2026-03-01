package Controllers;

import Services.PasswordResetService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ForgotPasswordController {

    @FXML private VBox           paneEmail;
    @FXML private VBox           paneOtp;
    @FXML private VBox           paneNewPassword;

    @FXML private TextField      txtEmail;
    @FXML private TextField      txtOtp;
    @FXML private PasswordField  txtNewPassword;
    @FXML private PasswordField  txtConfirmPassword;
    @FXML private Label          lblMsg;

    private final PasswordResetService resetService = new PasswordResetService();

    private String verifiedEmail;

    // ── Step 1: send OTP ────────────────────────────────────────────────────

    @FXML
    private void handleSendOtp() {
        String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        if (email.isEmpty()) {
            lblMsg.setText("Please enter your email.");
            return;
        }
        try {
            boolean sent = resetService.generateAndSendOtp(email);
            if (!sent) {
                lblMsg.setText("No account found with that email.");
                return;
            }
            verifiedEmail = email;
            showPane(paneOtp);
            lblMsg.setText("Code sent! Check your inbox.");
        } catch (IllegalStateException e) {
            // A code was already sent — go to OTP step so user can enter it
            verifiedEmail = email;
            showPane(paneOtp);
            lblMsg.setText("A code was already sent to your inbox. Enter it below.");
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Failed to send code: " + e.getMessage());
        }
    }

    // ── Step 2: verify OTP ──────────────────────────────────────────────────

    @FXML
    private void handleVerifyOtp() {
        String otp = txtOtp.getText() == null ? "" : txtOtp.getText().trim();
        if (otp.isEmpty()) {
            lblMsg.setText("Please enter the code.");
            return;
        }
        try {
            if (!resetService.verifyOtp(verifiedEmail, otp)) {
                lblMsg.setText("Invalid or expired code. Try again.");
                return;
            }
            showPane(paneNewPassword);
            lblMsg.setText("Code verified. Enter your new password.");
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Error: " + e.getMessage());
        }
    }

    // ── Step 3: reset password ──────────────────────────────────────────────

    @FXML
    private void handleResetPassword() {
        String pw  = txtNewPassword.getText()     == null ? "" : txtNewPassword.getText();
        String cpw = txtConfirmPassword.getText() == null ? "" : txtConfirmPassword.getText();

        if (pw.isEmpty()) {
            lblMsg.setText("Please enter a new password.");
            return;
        }
        if (pw.length() < 4) {
            lblMsg.setText("Password is too short (min 4 characters).");
            return;
        }
        if (!pw.equals(cpw)) {
            lblMsg.setText("Passwords do not match.");
            return;
        }

        String otp = txtOtp.getText() == null ? "" : txtOtp.getText().trim();
        try {
            boolean ok = resetService.resetPassword(verifiedEmail, otp, pw);
            if (!ok) {
                lblMsg.setText("Reset failed. The code may have expired.");
                return;
            }
            lblMsg.setText("Password reset! You can now log in.");
            handleBackToLogin();
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Error: " + e.getMessage());
        }
    }

    // ── Navigation ──────────────────────────────────────────────────────────

    @FXML
    private void handleBackToLogin() {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/LoginView.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
            stage.setTitle("Hirely — Login");
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Cannot open login: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToEmail() {
        showPane(paneEmail);
        lblMsg.setText("");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void showPane(VBox target) {
        for (VBox p : new VBox[]{paneEmail, paneOtp, paneNewPassword}) {
            p.setVisible(false);
            p.setManaged(false);
        }
        target.setVisible(true);
        target.setManaged(true);
        lblMsg.setText("");
    }
}
