package Controllers;

import Utils.AvatarCropDialog;
import Utils.UserSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

public class UserProfileController {

    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;

    @FXML private TextField txtStatus;     // ✅ MISSING IN YOUR CODE
    @FXML private Label lblMsg;            // ✅ MISSING IN YOUR CODE

    @FXML private ImageView imgProfile;
    @FXML private Label lblProfileInitials;

    private String selectedProfilePicPath;

    private final UserService userService = new UserService();
    private MainShellController shell;

    @FXML
    /**
     * Initializes UI components and loads initial data.
     */
    private void initialize() {
        loadFromSession();
        applyCircleClip(imgProfile, 90);

    }

    /**
     * Sets the shell value.
     */
    public void setShell(MainShellController shell) {
        this.shell = shell;
    }
    /**
     * Executes this operation.
     */
    public static void applyCircleClip(ImageView iv, double size) {
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);

        Circle clip = new Circle(size / 2.0, size / 2.0, size / 2.0);
        iv.setClip(clip);

        // keep clip centered even if layout changes
        iv.layoutBoundsProperty().addListener((obs, oldB, b) -> {
            clip.setCenterX(b.getWidth() / 2.0);
            clip.setCenterY(b.getHeight() / 2.0);
            clip.setRadius(Math.min(b.getWidth(), b.getHeight()) / 2.0);
        });
    }

    /**
     * Loads and refreshes data displayed in the view.
     */
    private void loadFromSession() {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;

        txtFirstName.setText(u.getFirstName());
        txtLastName.setText(u.getLastName());
        txtEmail.setText(u.getEmail());
        txtPassword.setText(u.getPassword());
        txtStatus.setText(u.getStatus() == null ? "active" : u.getStatus());

        String fn = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String ln = u.getLastName() == null ? "" : u.getLastName().trim();
        String initials = (fn.isEmpty() ? "U" : fn.substring(0, 1).toUpperCase())
                + (ln.isEmpty() ? "" : ln.substring(0, 1).toUpperCase());
        lblProfileInitials.setText(initials.trim().isEmpty() ? "U" : initials);

        selectedProfilePicPath = u.getProfilePic();
        refreshProfileImagePreview(selectedProfilePicPath);
    }

    /**
     * Loads and refreshes data displayed in the view.
     */
    private void refreshProfileImagePreview(String path) {
        boolean hasPic = path != null && !path.trim().isEmpty() && new File(path).exists();
        if (hasPic) {
            imgProfile.setImage(new Image(new File(path).toURI().toString(), true));
            lblProfileInitials.setVisible(false);
        } else {
            imgProfile.setImage(null);
            lblProfileInitials.setVisible(true);
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleChoosePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Profile Picture");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File chosen = fc.showOpenDialog(txtEmail.getScene().getWindow());
        if (chosen == null) return;

        try {
            File dir = new File("userpics");
            if (!dir.exists()) dir.mkdirs();

            User u = UserSession.getInstance().getCurrentUser();
            File dest = new File(dir, "user_" + u.getUserId() + ".png");

            boolean saved = AvatarCropDialog.showAndSave(
                    txtEmail.getScene().getWindow(),
                    chosen,
                    dest,
                    256  // output size
            );

            if (!saved) return;

            selectedProfilePicPath = dest.getAbsolutePath();
            refreshProfileImagePreview(selectedProfilePicPath);
            lblMsg.setText("Photo updated. Click Save Changes to apply to your account.");

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Failed to crop photo: " + e.getMessage());
        }
    }


    @FXML
    /**
     * Handles the associated UI event.
     */
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
            User updated = new User(
                    u.getUserId(),
                    fn,
                    ln,
                    em,
                    pw,
                    u.getRoleId(),
                    u.getStatus()
            );
            updated.setProfilePic(selectedProfilePicPath);

            userService.updateUser(u.getUserId(), updated);

            // ✅ update session too (INCLUDING profile pic)
            u.setFirstName(fn);
            u.setLastName(ln);
            u.setEmail(em);
            u.setPassword(pw);
            u.setProfilePic(selectedProfilePicPath);

            lblMsg.setText("Profile updated.");
            if (shell != null) shell.refreshShellUserChip();

        } catch (SQLException e) {
            e.printStackTrace();
            lblMsg.setText("Update failed: " + e.getMessage());
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
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
            UserSession.getInstance().clear();
            if (shell != null) shell.handleLogout();

        } catch (SQLException e) {
            e.printStackTrace();
            lblMsg.setText("Deactivate failed: " + e.getMessage());
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
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
                if (shell != null) shell.handleLogout();

            } catch (SQLException e) {
                e.printStackTrace();
                lblMsg.setText("Delete failed: " + e.getMessage());
            }
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleBack() {
        if (shell != null) shell.backToPlans();
    }

    /**
     * Executes this operation.
     */
    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
