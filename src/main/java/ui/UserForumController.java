package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.ForumPost;
import repo.ForumPostRepository;
import repo.UserRepository;
import util.Session;
import util.InputValidator;
import java.util.List;

import java.util.Comparator;

/**
 * Main user-facing forum feed controller.
 * Handles approved-post browsing, search/sort, and user post creation flow.
 */
public class UserForumController {

    @FXML
    private ImageView logoView;
    @FXML
    private Label currentUserLabel;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortBox;
    @FXML
    private Button newPostBtn;
    @FXML
    private Button profileBtn;
    @FXML
    private Button adminPanelBtn;
    @FXML
    private Button becomeAdminBtn;
    @FXML
    private ToggleButton themeToggle;

    @FXML
    private ListView<ForumPost> postListView;
    @FXML
    private Label hintLabel;
    @FXML
    private HBox appBar;
    @FXML
    private ComboBox<DevUser> devUserBox;

    private double xOffset = 0;
    private double yOffset = 0;

    private final ForumPostRepository postRepo = new ForumPostRepository();
    private final UserRepository userRepo = new UserRepository();

    private final ObservableList<ForumPost> allApproved = FXCollections.observableArrayList();
    private FilteredList<ForumPost> filtered;
    private SortedList<ForumPost> sorted;

    @FXML
    private void initialize() {
        // logo
        try {
            logoView.setImage(new Image(getClass().getResourceAsStream("/assets/logo.png")));
        } catch (Exception ignored) {
        }

        refreshSessionUI();
        syncThemeToggle();

        // Dev-only quick identity switcher for local testing.
        devUserBox.setItems(FXCollections.observableArrayList(
                new DevUser(1, "Ali (USER)", util.Session.Role.USER),
                new DevUser(2, "Mohammed (USER)", util.Session.Role.USER)));

        devUserBox.getSelectionModel().select(0); // Default Ali

        devUserBox.valueProperty().addListener((obs, oldV, v) -> {
            if (v == null)
                return;
            util.Session.set(v.id, v.role);
            refreshSessionUI();
            onRefresh();
        });

        // Window Dragging
        appBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        appBar.setOnMouseDragged(event -> {
            if (appBar.getScene() != null && appBar.getScene().getWindow() != null) {
                appBar.getScene().getWindow().setX(event.getScreenX() - xOffset);
                appBar.getScene().getWindow().setY(event.getScreenY() - yOffset);
            }
        });

        // Sorting options for feed chronology.
        sortBox.setItems(FXCollections.observableArrayList("New", "Old"));
        sortBox.getSelectionModel().select("New");

        // Post cards in user mode hide moderation status chip.
        postListView.setCellFactory(lv -> new ui.components.PostCardCell(userRepo, false));

        // Feed pipeline: source list -> filter by query -> sorted view.
        filtered = new FilteredList<>(allApproved, p -> true);
        sorted = new SortedList<>(filtered);
        sorted.setComparator(postComparator(sortBox.getValue()));
        postListView.setItems(sorted);

        sortBox.valueProperty().addListener((obs, o, v) -> sorted.setComparator(postComparator(v)));

        // Live search across title/content/category.
        searchField.textProperty().addListener((obs, o, v) -> {
            String q = (v == null ? "" : v.trim().toLowerCase());
            filtered.setPredicate(p -> {
                if (q.isEmpty())
                    return true;
                String t = safe(p.getTitle()).toLowerCase();
                String c = safe(p.getContent()).toLowerCase();
                String cat = safe(p.getCategory()).toLowerCase();
                return t.contains(q) || c.contains(q) || cat.contains(q);
            });
        });

        // Double click to open post in new window
        postListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                ForumPost p = postListView.getSelectionModel().getSelectedItem();
                if (p != null)
                    openPostDetails(p);
            }
        });

        onRefresh();
    }

    private Comparator<ForumPost> postComparator(String mode) {
        Comparator<ForumPost> pinnedFirst = Comparator.comparing(ForumPost::isPinned).reversed();

        Comparator<ForumPost> byCreatedAsc = Comparator.comparing(
                ForumPost::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder()));

        Comparator<ForumPost> byCreatedDesc = byCreatedAsc.reversed();

        Comparator<ForumPost> byDate = "Old".equalsIgnoreCase(mode) ? byCreatedAsc : byCreatedDesc;

        // pinned always stays on top, then apply New/Old sorting inside each group
        return pinnedFirst.thenComparing(byDate);
    }

    @FXML
    private void onRefresh() {
        try {
            // READ (posts): user feed only shows admin-approved posts.
            allApproved.setAll(postRepo.findApproved());
        } catch (Exception ex) {
            showError("Failed to load feed", ex);
        }
    }

    @FXML
    private void onOpenProfile() {
        try {
            // Navigation to user profile screen in a separate window.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/UserProfileView.fxml"));
            Scene scene = new Scene(loader.load(), 1150, 750);
            scene.getStylesheets().add(getClass().getResource(Session.getThemeStylesheetPath()).toExternalForm());

            Stage st = new Stage();
            st.initStyle(javafx.stage.StageStyle.UNDECORATED);
            st.setTitle("Hirely - My Profile");
            st.initModality(Modality.NONE);
            st.setScene(scene);
            st.sizeToScene();
            st.centerOnScreen();
            st.show();
        } catch (Exception ex) {
            showError("Failed to open profile", ex);
        }
    }

    @FXML
    private void onNewPost() {
        // CREATE (posts): open editor in "new post" mode.
        showPostEditor(null);
    }

    private void showPostEditor(ForumPost existing) {
        boolean editing = existing != null;
        Dialog<ForumPost> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Edit Post" : "New Post");

        dialog.getDialogPane().getStylesheets()
                .add(getClass().getResource(Session.getThemeStylesheetPath()).toExternalForm());
        dialog.getDialogPane().getStyleClass().add("root");

        ButtonType okType = new ButtonType(editing ? "Update" : "Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, okType);

        TextField title = new TextField(editing ? existing.getTitle() : "");
        TextArea content = new TextArea(editing ? existing.getContent() : "");
        content.setWrapText(true);

        TextField category = new TextField(editing ? safe(existing.getCategory()) : "");

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(12));
        gp.addRow(0, new Label("Title"), title);
        gp.addRow(1, new Label("Content"), content);
        gp.addRow(2, new Label("Category"), category);

        GridPane.setHgrow(title, Priority.ALWAYS);
        GridPane.setHgrow(category, Priority.ALWAYS);
        GridPane.setHgrow(content, Priority.ALWAYS);

        ScrollPane sp = new ScrollPane(gp);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setPrefViewportHeight(420);
        dialog.getDialogPane().setContent(sp);

        Node okBtn = dialog.getDialogPane().lookupButton(okType);

        // Block submit and show validation messages before closing dialog.
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            List<String> errors = InputValidator.validatePost(title.getText(), content.getText(), category.getText());
            if (!errors.isEmpty()) {
                ev.consume(); // prevent dialog from closing
                showValidation(errors);
            }
        });

        // CREATE/UPDATE (posts): collect and normalize user input.
        dialog.setResultConverter(btn -> {
            if (btn != okType)
                return null;

            ForumPost p = editing ? existing : new ForumPost();
            p.setAuthorId(Session.getCurrentUserId());

            String t = InputValidator.norm(title.getText());
            String c = InputValidator.norm(content.getText());
            String cat = InputValidator.normalizeNullable(category.getText());

            // Safe because we block with validation above
            p.setTitle(t);
            p.setContent(c);
            p.setCategory(cat);

            p.setStatus("PENDING");
            return p;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                if (editing) {
                    // UPDATE path for user's own post.
                    postRepo.update(p);
                    showInfo("Post updated (sent for approval)");
                } else {
                    // CREATE path for new post.
                    postRepo.insert(p);
                    showInfo("Post submitted for approval");
                }
                onRefresh();
            } catch (Exception ex) {
                showError("Failed to save post", ex);
            }
        });
    }

    private void openPostDetails(ForumPost p) {
        try {
            // Open details in a new stage to keep the feed context intact.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/PostDetailsView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 850);
            scene.getStylesheets().add(getClass().getResource(Session.getThemeStylesheetPath()).toExternalForm());

            PostDetailsController ctrl = loader.getController();
            ctrl.setPost(p);

            Stage st = new Stage();
            st.initStyle(javafx.stage.StageStyle.UNDECORATED);
            st.setTitle("Hirely - Post #" + p.getId());
            st.initModality(Modality.NONE);
            st.setScene(scene);
            st.sizeToScene();
            st.centerOnScreen();
            st.show();
        } catch (Exception ex) {
            showError("Failed to open post", ex);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(title);
        a.setContentText(ex.getMessage());
        a.showAndWait();
        ex.printStackTrace();
    }

    private void refreshSessionUI() {
        // Toggle role-gated actions and dev tooling visibility.
        long uid = Session.getCurrentUserId();
        String name = userRepo.getDisplayNameById(uid);
        currentUserLabel.setText("Logged in as " + (name == null || name.isBlank() ? ("User #" + uid) : name));

        boolean isAdmin = Session.isAdmin();
        boolean dev = Session.DEV_MODE;

        adminPanelBtn.setVisible(isAdmin);
        adminPanelBtn.setManaged(isAdmin);

        becomeAdminBtn.setVisible(dev && !isAdmin);
        becomeAdminBtn.setManaged(dev && !isAdmin);

        devUserBox.setVisible(dev);
        devUserBox.setManaged(dev);
    }

    private void showValidation(java.util.List<String> errors) {
        // Unified validation alert formatting for post editor errors.
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Validation");
        a.setHeaderText("Please fix the following:");
        a.setContentText(String.join("\n- ", prependBullet(errors)));
        a.showAndWait();
    }

    private java.util.List<String> prependBullet(java.util.List<String> errors) {
        if (errors.isEmpty())
            return errors;
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add("- " + errors.get(0));
        for (int i = 1; i < errors.size(); i++)
            out.add(errors.get(i));
        return out;
    }

    @FXML
    private void onOpenAdminPanel() {
        try {
            // Role-gated navigation to admin moderation screen.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/AdminForumView.fxml"));
            Scene scene = new Scene(loader.load(), 1250, 800);
            scene.getStylesheets().add(getClass().getResource(Session.getThemeStylesheetPath()).toExternalForm());

            Stage st = new Stage();
            st.initStyle(javafx.stage.StageStyle.UNDECORATED);
            st.setTitle("Hirely - Admin Panel");
            st.initModality(Modality.NONE);
            st.setScene(scene);
            st.sizeToScene();
            st.centerOnScreen();
            st.show();
        } catch (Exception ex) {
            showError("Failed to open Admin Panel", ex);
        }
    }

    @FXML
    private void onBecomeAdmin() {
        // Dev shortcut: elevate current session role to ADMIN.
        util.Session.set(Session.getCurrentUserId(), Session.Role.ADMIN);
        refreshSessionUI();
        onRefresh();
    }

    @FXML
    private void onToggleTheme() {
        Session.LIGHT_MODE = themeToggle.isSelected();
        syncThemeToggle();
        applyThemeToScene();
    }

    private void syncThemeToggle() {
        boolean light = Session.LIGHT_MODE;
        themeToggle.setSelected(light);
        themeToggle.setText(light ? "Dark" : "Light");
    }

    private void applyThemeToScene() {
        Scene scene = themeToggle.getScene();
        if (scene == null) {
            return;
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource(Session.getThemeStylesheetPath()).toExternalForm());
    }

    private static class DevUser {
        final long id;
        final String label;
        final util.Session.Role role;

        DevUser(long id, String label, util.Session.Role role) {
            this.id = id;
            this.label = label;
            this.role = role;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @FXML
    private void onMinimize() {
        // Custom app bar window controls (undecorated stage).
        if (appBar.getScene().getWindow() instanceof Stage s)
            s.setIconified(true);
    }

    @FXML
    private void onMaximize() {
        if (appBar.getScene().getWindow() instanceof Stage s) {
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            if (s.isMaximized()) {
                s.setMaximized(false);
            } else {
                s.setX(bounds.getMinX());
                s.setY(bounds.getMinY());
                s.setWidth(bounds.getWidth());
                s.setHeight(bounds.getHeight());
                s.setMaximized(true);
            }
        }
    }

    @FXML
    private void onClose() {
        // Main window close exits the application.
        System.exit(0);
    }
}
