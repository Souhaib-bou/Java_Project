package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.ForumComment;
import model.ForumPost;
import org.kordamp.ikonli.javafx.FontIcon;
import repo.ForumCommentRepository;
import repo.ForumPostInteractionRepository;
import repo.ForumPostRepository;
import repo.UserRepository;
import util.DebugLog;
import util.Session;

/**
 * Profile screen controller.
 * Shows current user's posts/comments and allows opening post details.
 */
public class UserProfileController {

    @FXML
    private Label profileTitle;
    @FXML
    private ListView<ForumPost> myPostsList;
    @FXML
    private ListView<ForumComment> myCommentsList;
    @FXML
    private HBox appBar;
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private FontIcon themeIcon;

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean customMaximized = false;
    private double restoreX = 0;
    private double restoreY = 0;
    private double restoreWidth = 0;
    private double restoreHeight = 0;

    private final ForumPostRepository postRepo = new ForumPostRepository();
    private final ForumPostInteractionRepository interactionRepo = new ForumPostInteractionRepository();
    private final ForumCommentRepository commentRepo = new ForumCommentRepository();
    private final UserRepository userRepo = new UserRepository();

    private final ObservableList<ForumPost> myPosts = FXCollections.observableArrayList();
    private final ObservableList<ForumComment> myComments = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Resolve current user once at screen load and update header text.
        long uid = Session.getCurrentUserId();
        String name = userRepo.getDisplayNameById(uid);
        profileTitle.setText("My Profile - " + (name == null || name.isBlank() ? ("User #" + uid) : name));

        // JavaFX observable lists keep ListView content reactive.
        myPostsList.setItems(myPosts);
        myCommentsList.setItems(myComments);

        // Reuse shared post card style + compact custom comment card.
        myPostsList.setCellFactory(lv -> new ui.components.PostCardCell(userRepo, false));
        myCommentsList.setCellFactory(lv -> new CommentMiniCell(userRepo));

        // UX shortcut: open full post details on double-click.
        myPostsList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                ForumPost p = myPostsList.getSelectionModel().getSelectedItem();
                if (p != null)
                    openPostDetails(p);
            }
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

        syncThemeToggle();
        onRefresh();
    }

    @FXML
    private void onRefresh() {
        try {
            // Keep profile data scoped to the logged-in user.
            // Includes READ queries for both authored posts and comments.
            long uid = Session.getCurrentUserId();
            java.util.List<ForumPost> posts = postRepo.findByAuthorId(uid);
            hydrateLikeState(posts, uid);
            myPosts.setAll(posts);
            myComments.setAll(commentRepo.findByAuthorId(uid));
        } catch (Exception ex) {
            showError("Failed to load profile", ex);
        }
    }

    private void hydrateLikeState(java.util.List<ForumPost> posts, long userId) {
        for (ForumPost post : posts) {
            try {
                post.setLikedByCurrentUser(interactionRepo.isLiked(post.getId(), userId));
            } catch (Exception ex) {
                post.setLikedByCurrentUser(false);
                DebugLog.error("UserProfileController", "Failed loading like state for post #" + post.getId(), ex);
            }
        }
    }

    private void openPostDetails(ForumPost p) {
        try {
            // Open post details in a separate window so profile stays open.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/PostDetailsView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 860);
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

    private static class CommentMiniCell extends ListCell<ForumComment> {
        private final UserRepository userRepo;
        private final VBox box = new VBox(4);
        private final Label content = new Label();
        private final Label meta = new Label();

        CommentMiniCell(UserRepository userRepo) {
            this.userRepo = userRepo;
            // Compact card design for profile comment history.
            box.getStyleClass().add("card");
            content.setWrapText(true);
            meta.getStyleClass().add("muted");
            box.getChildren().addAll(content, meta);
        }

        @Override
        protected void updateItem(ForumComment c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) {
                setGraphic(null);
                return;
            }
            content.setText(c.getContent() == null ? "" : c.getContent());
            String author = userRepo.getDisplayNameById(c.getAuthorId());
            meta.setText((author == null ? ("User #" + c.getAuthorId()) : author) + " - " + c.getStatus());
            setGraphic(box);
        }
    }

    private void showError(String title, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(title);
        a.setContentText(ex.getMessage());
        a.showAndWait();
        ex.printStackTrace();
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
            if (customMaximized) {
                s.setMaximized(false);
                s.setX(restoreX);
                s.setY(restoreY);
                s.setWidth(restoreWidth);
                s.setHeight(restoreHeight);
                customMaximized = false;
            } else {
                restoreX = s.getX();
                restoreY = s.getY();
                restoreWidth = s.getWidth();
                restoreHeight = s.getHeight();
                s.setMaximized(false);
                s.setX(bounds.getMinX());
                s.setY(bounds.getMinY());
                s.setWidth(bounds.getWidth());
                s.setHeight(bounds.getHeight());
                customMaximized = true;
            }
        }
    }

    @FXML
    private void onClose() {
        // Close only this window; keep other app windows alive.
        appBar.getScene().getWindow().hide();
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
        themeToggle.setText("");
        if (themeIcon != null) {
            themeIcon.setIconLiteral(light ? "fas-sun" : "fas-moon");
        }
        themeToggle.getStyleClass().remove("icon-accent");
        if (light) {
            themeToggle.getStyleClass().add("icon-accent");
        }
    }

    private void applyThemeToScene() {
        Scene scene = themeToggle.getScene();
        if (scene == null) {
            return;
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource(Session.getThemeStylesheetPath()).toExternalForm());
    }
}
