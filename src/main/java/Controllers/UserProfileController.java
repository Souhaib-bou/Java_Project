package Controllers;

import Models.User;
import Services.UserService;
import Utils.UserSession;

<<<<<<< HEAD
import javafx.fxml.FXML;
import javafx.scene.control.*;
=======
import Utils.QRCodeGenerator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67

import java.sql.SQLException;

public class UserProfileController {

<<<<<<< HEAD
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;

    @FXML private Label lblMsg;
=======
    @FXML private TextField     txtFirstName;
    @FXML private TextField     txtLastName;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ImageView     qrCodeView;
    @FXML private Label         lblMsg;
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67

    private final UserService userService = new UserService();
    private MainShellController shell;

    @FXML
    private void initialize() {
        loadFromSession();
    }

    public void setShell(MainShellController shell) {
        this.shell = shell;
    }

    private void loadFromSession() {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;

        txtFirstName.setText(nullToEmpty(u.getFirstName()));
        txtLastName.setText(nullToEmpty(u.getLastName()));
        txtEmail.setText(nullToEmpty(u.getEmail()));
        txtPassword.setText(nullToEmpty(u.getPassword()));
<<<<<<< HEAD
=======

        generateQRCode(u);
    }

    private void generateQRCode(User u) {
        try {
            String vCard =
                    "BEGIN:VCARD\r\n" +
                    "VERSION:3.0\r\n" +
                    "FN:" + u.getFullName() + "\r\n" +
                    "N:" + nullToEmpty(u.getLastName()) + ";" + nullToEmpty(u.getFirstName()) + ";;;\r\n" +
                    "EMAIL:" + nullToEmpty(u.getEmail()) + "\r\n" +
                    "TITLE:" + nullToEmpty(u.getRoleName()) + "\r\n" +
                    "NOTE:Status: " + nullToEmpty(u.getStatus()) + "\r\n" +
                    "END:VCARD";
            qrCodeView.setImage(QRCodeGenerator.generate(vCard, 300));
        } catch (Exception e) {
            e.printStackTrace();
        }
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    }

    @FXML
    private void handleSave() {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;

        String fn = nullToEmpty(txtFirstName.getText()).trim();
        String ln = nullToEmpty(txtLastName.getText()).trim();
        String em = nullToEmpty(txtEmail.getText()).trim();
        String pw = nullToEmpty(txtPassword.getText());

        if (fn.isEmpty() || ln.isEmpty() || em.isEmpty() || pw.isEmpty()) {
            lblMsg.setText("First name, last name, email, and password are required.");
            return;
        }
        if (!em.contains("@") || !em.contains(".")) {
            lblMsg.setText("Please enter a valid email.");
            return;
        }

        try {
            // keep roleId + status unchanged
            User updated = new User(
                    u.getUserId(),
                    fn,
                    ln,
                    em,
                    pw,
                    u.getRoleId(),
                    u.getStatus()
            );

            userService.updateUser(u.getUserId(), updated);

            // update session too
            u.setFirstName(fn);
            u.setLastName(ln);
            u.setEmail(em);
            u.setPassword(pw);

            lblMsg.setText("Profile updated.");
            if (shell != null) shell.refreshShellUserChip();

        } catch (SQLException e) {
            e.printStackTrace();
            lblMsg.setText("Update failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeactivateAccount() {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;

        var res = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Deactivate your account?\n\nYou will be logged out immediately.",
                ButtonType.OK, ButtonType.CANCEL
        ).showAndWait();

        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            userService.setUserStatus(u.getUserId(), "inactive");

            // clear session + logout to login screen
            UserSession.getInstance().clear();
            if (shell != null) shell.handleLogout();

        } catch (SQLException e) {
            e.printStackTrace();
            lblMsg.setText("Deactivate failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteAccount() {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;

        var res = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete your account?\nThis action cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL).showAndWait();

        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                userService.deleteUser(u.getUserId());
                UserSession.getInstance().clear();

                // return to login
                if (shell != null) shell.handleLogout();

            } catch (SQLException e) {
                e.printStackTrace();
                lblMsg.setText("Delete failed: " + e.getMessage());
            }
        }
    }

    @FXML
<<<<<<< HEAD
=======
    private void handleSetupFaceId() {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FaceSetupView.fxml"));
            Parent root = loader.load();

            FaceSetupController ctrl = loader.getController();
            ctrl.setUserId(u.getUserId());

            Stage dialog = new Stage();
            dialog.setTitle("Hirely — Setup Face ID");
            dialog.setScene(new Scene(root, 700, 580));
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Cannot open Face ID setup: " + e.getMessage());
        }
    }

    @FXML
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private void handleBack() {
        if (shell != null) shell.backToPlans();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
