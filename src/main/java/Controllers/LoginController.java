package Controllers;

import Models.User;
import Utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import Utils.api.ApiClient;

public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMsg;

    @FXML
    private void handleLogin() {

        String email = txtEmail.getText();
        String password = txtPassword.getText();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            lblMsg.setText("Please enter email and password.");
            return;
        }

        try {
            // 1) call Spring Boot backend login
            String json = "{ \"email\": \"" + escapeJson(email) + "\", \"password\": \"" + escapeJson(password) + "\" }";
            String response = ApiClient.post("/api/auth/login", json);

            // 2) parse response: {"token":"...","userId":1,"roleId":1}
            String token = extractString(response, "token");
            int userId = extractInt(response, "userId");
            int roleId = extractInt(response, "roleId");

            // 3) store session
            User u = new User();
            u.setUserId(userId);
            u.setRoleId(roleId);

            UserSession.getInstance().setCurrentUser(u);
            UserSession.getInstance().setToken(token);
            Utils.api.ApiSession.setToken(token);

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Login failed: " + e.getMessage());
            return;
        }

        // 4) navigate to main shell
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

    // ---------- small helpers (no extra libraries needed) ----------

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractString(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int i = json.indexOf(needle);
        if (i < 0) throw new RuntimeException("Missing field: " + key);
        int start = i + needle.length();
        int end = json.indexOf("\"", start);
        if (end < 0) throw new RuntimeException("Bad JSON for field: " + key);
        return json.substring(start, end);
    }

    private static int extractInt(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) throw new RuntimeException("Missing field: " + key);
        int start = i + needle.length();

        // read digits until non-digit
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;

        return Integer.parseInt(json.substring(start, end));
    }
}