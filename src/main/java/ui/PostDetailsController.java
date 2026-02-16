package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.ForumComment;
import model.ForumPost;
import repo.ForumCommentRepository;
import repo.ForumPostRepository;
import repo.UserRepository;
import util.Session;
import util.InputValidator;
import java.util.List;

import java.time.format.DateTimeFormatter;

/**
 * Controller for the full post details window.
 * Shows post content, approved comments, and owner-level edit actions.
 */
public class PostDetailsController {

    @FXML
    private Label categoryChip, statusChip, pinnedChip, lockedChip;
    @FXML
    private Label titleLabel, metaLabel, contentLabel;

    @FXML
    private Button editPostBtn, deletePostBtn;

    @FXML
    private ListView<ForumComment> commentListView;
    @FXML
    private TextArea commentArea;
    @FXML
    private ComboBox<String> commentStatusBox;
    @FXML
    private Button addCommentBtn, updateCommentBtn, deleteCommentBtn;
    @FXML
    private Label lockHint;
    @FXML
    private HBox appBar;

    private double xOffset = 0;
    private double yOffset = 0;

    private final ForumCommentRepository commentRepo = new ForumCommentRepository();
    private final ForumPostRepository postRepo = new ForumPostRepository();
    private final UserRepository userRepo = new UserRepository();

    private final ObservableList<ForumComment> comments = FXCollections.observableArrayList();

    private ForumPost post;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");

    @FXML
    private void initialize() {
        // Status choices visible in the comment editor controls.
        commentStatusBox.setItems(FXCollections.observableArrayList("PENDING", "APPROVED", "REJECTED"));
        commentStatusBox.getSelectionModel().select("PENDING");

        // JavaFX observable list binding for on-screen comments.
        commentListView.setItems(comments);
        commentListView.setCellFactory(lv -> new CommentCardCell(userRepo));

        commentListView.getSelectionModel().selectedItemProperty().addListener((obs, o, c) -> {
            if (c == null) {
                updateCommentUiState();
                refreshCommentActionVisibility();
                return;
            }
            commentArea.setText(c.getContent());
            commentStatusBox.setValue(c.getStatus());
            updateCommentUiState();
            refreshCommentActionVisibility();
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
    }

    public void setPost(ForumPost p) {
        // Called by parent screens immediately after loading this view.
        this.post = p;
        render();
        loadComments();
        applyPermissions();
        updateCommentUiState();
        refreshCommentActionVisibility();
    }

    private void render() {
        if (post == null)
            return;

        titleLabel.setText(post.getTitle());
        contentLabel.setText(post.getContent() == null ? "" : post.getContent());

        String author = userRepo.getDisplayNameById(post.getAuthorId());
        String when = post.getCreatedAt() == null ? "" : DT.format(post.getCreatedAt());
        metaLabel.setText(
                (author == null ? ("User #" + post.getAuthorId()) : author) + (when.isBlank() ? "" : " - " + when));

        categoryChip
                .setText((post.getCategory() == null || post.getCategory().isBlank()) ? "General" : post.getCategory());
        statusChip.setText(post.getStatus());

        pinnedChip.setVisible(post.isPinned());
        pinnedChip.setManaged(post.isPinned());

        lockedChip.setVisible(post.isLocked());
        lockedChip.setManaged(post.isLocked());
    }

    private void applyPermissions() {
        if (post == null)
            return;

        long uid = Session.getCurrentUserId();
        boolean isOwner = post.getAuthorId() == uid;

        // USER flow: only post owner can edit/delete post content.
        editPostBtn.setVisible(isOwner);
        editPostBtn.setManaged(isOwner);
        deletePostBtn.setVisible(isOwner);
        deletePostBtn.setManaged(isOwner);
    }

    private boolean isCommentingBlocked() {
        // Comments are blocked when post is absent/locked or current session is admin.
        return post == null || post.isLocked() || Session.isAdmin();
    }

    private void updateCommentUiState() {
        boolean blocked = isCommentingBlocked();

        commentArea.setDisable(blocked);
        addCommentBtn.setDisable(blocked);

        boolean hasSelection = commentListView.getSelectionModel().getSelectedItem() != null;
        updateCommentBtn.setDisable(blocked || !hasSelection);
        deleteCommentBtn.setDisable(blocked || !hasSelection);

        if (post == null) {
            lockHint.setText("");
        } else if (post.isLocked()) {
            lockHint.setText("This post is locked. Comments are read-only.");
        } else if (Session.isAdmin()) {
            lockHint.setText("Admins can comment only from the Admin Panel.");
        } else {
            lockHint.setText("");
        }
    }

    private void refreshCommentActionVisibility() {
        if (post == null)
            return;

        boolean isAdmin = Session.isAdmin();
        boolean locked = post.isLocked();

        ForumComment sel = commentListView.getSelectionModel().getSelectedItem();
        boolean hasSel = sel != null;

        long currentUserId = Session.getCurrentUserId();
        boolean isOwnerOfSelected = hasSel && sel.getAuthorId() == currentUserId;

        // Add button: only users + not locked
        boolean canAdd = !isAdmin && !locked;
        addCommentBtn.setVisible(canAdd);
        addCommentBtn.setManaged(canAdd);

        // Update/Delete: only if selected comment is mine + not locked + not admin
        boolean canModifySelected = !isAdmin && !locked && hasSel && isOwnerOfSelected;

        updateCommentBtn.setVisible(canModifySelected);
        updateCommentBtn.setManaged(canModifySelected);

        deleteCommentBtn.setVisible(canModifySelected);
        deleteCommentBtn.setManaged(canModifySelected);
    }

    @FXML
    private void onRefreshComments() {
        loadComments();
        refreshCommentActionVisibility();
    }

    private void loadComments() {
        try {
            comments.clear();
            if (post == null)
                return;

            // READ (comments): user-facing details show approved comments only.
            comments.setAll(commentRepo.findApprovedByPostId(post.getId()));

            commentListView.getSelectionModel().clearSelection();
            commentArea.clear();
            commentStatusBox.setValue("PENDING");
        } catch (Exception ex) {
            showError("Failed to load comments", ex);
        }
    }

    @FXML
    private void onAddComment() {
        // CREATE (comments): users can submit new comments for moderation.
        if (Session.isAdmin()) {
            showWarning("Admins can comment only from the Admin Panel.");
            return;
        }
        if (post == null || post.isLocked()) {
            showWarning("This post is locked. You cannot modify comments.");
            return;
        }

        List<String> errors = InputValidator.validateComment(commentArea.getText());
        if (!errors.isEmpty()) {
            showWarning(errors.get(0));
            return;
        }

        ForumComment c = new ForumComment();
        c.setPostId(post.getId());
        c.setAuthorId(Session.getCurrentUserId());
        c.setContent(InputValidator.norm(commentArea.getText()));

        // User comments are submitted as pending moderation.
        c.setStatus("PENDING");

        try {
            commentRepo.insert(c);
            showInfo("Comment submitted for approval");
            loadComments();
            refreshCommentActionVisibility();
        } catch (Exception ex) {
            showError("Failed to add comment", ex);
        }
    }

    @FXML
    private void onUpdateComment() {
        // UPDATE (comments): only the current comment owner can edit.
        if (Session.isAdmin()) {
            showWarning("Admins can comment only from the Admin Panel.");
            return;
        }
        if (post == null || post.isLocked()) {
            showWarning("This post is locked. You cannot modify comments.");
            return;
        }

        ForumComment selected = commentListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        long uid = Session.getCurrentUserId();
        if (selected.getAuthorId() != uid) {
            showWarning("You can only edit your own comment.");
            return;
        }

        List<String> errors = InputValidator.validateComment(commentArea.getText());
        if (!errors.isEmpty()) {
            showWarning(errors.get(0));
            return;
        }

        selected.setContent(InputValidator.norm(commentArea.getText()));
        selected.setStatus("PENDING"); // resubmit after edit

        try {
            commentRepo.update(selected);
            showInfo("Comment updated (pending approval)");
            loadComments();
            refreshCommentActionVisibility();
        } catch (Exception ex) {
            showError("Failed to update comment", ex);
        }
    }

    @FXML
    private void onDeleteComment() {
        // DELETE (comments): only the current comment owner can delete.
        if (Session.isAdmin()) {
            showWarning("Admins can comment only from the Admin Panel.");
            return;
        }
        ForumComment selected = commentListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        long uid = Session.getCurrentUserId();
        if (selected.getAuthorId() != uid) {
            showWarning("You can only delete your own comment.");
            return;
        }

        try {
            commentRepo.delete(selected.getId());
            showInfo("Comment deleted");
            loadComments();
            refreshCommentActionVisibility();
        } catch (Exception ex) {
            showError("Failed to delete comment", ex);
        }
    }

    @FXML
    private void onEditPost() {
        // UPDATE (posts): owner edit flow with validation + resubmission.
        if (post == null)
            return;

        Dialog<ForumPost> dialog = new Dialog<>();
        dialog.setTitle("Edit Post");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/hirely.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("root");

        ButtonType okType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, okType);

        TextField title = new TextField(post.getTitle());
        TextArea content = new TextArea(post.getContent());
        content.setWrapText(true);
        TextField category = new TextField(post.getCategory() == null ? "" : post.getCategory());

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(12));
        gp.addRow(0, new Label("Title"), title);
        gp.addRow(1, new Label("Content"), content);
        gp.addRow(2, new Label("Category"), category);

        dialog.getDialogPane().setContent(gp);

        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            List<String> errors = InputValidator.validatePost(title.getText(), content.getText(), category.getText());
            if (!errors.isEmpty()) {
                ev.consume();
                showWarning("- " + String.join("\n- ", errors));
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != okType)
                return null;
            post.setTitle(InputValidator.norm(title.getText()));
            post.setContent(InputValidator.norm(content.getText()));
            post.setCategory(InputValidator.normalizeNullable(category.getText()));
            post.setStatus("PENDING");
            return post;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                // Edited user posts are re-submitted for admin approval.
                postRepo.update(p);
                showInfo("Post updated (resubmitted for approval)");
                render();
                applyPermissions();
            } catch (Exception ex) {
                showError("Failed to update post", ex);
            }
        });
    }

    @FXML
    private void onDeletePost() {
        // DELETE (posts): removes this post and closes details window.
        if (post == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete this post?");
        confirm.setContentText("This will also delete its comments.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;

        try {
            postRepo.delete(post.getId());
            showInfo("Post deleted");
            // close window:
            editPostBtn.getScene().getWindow().hide();
        } catch (Exception ex) {
            showError("Failed to delete post", ex);
        }
    }

    // Shared comment card
    private static class CommentCardCell extends ListCell<ForumComment> {
        private final UserRepository userRepo;
        private final VBox card = new VBox(6);
        private final Label content = new Label();
        private final Label meta = new Label();

        CommentCardCell(UserRepository userRepo) {
            this.userRepo = userRepo;
            card.getStyleClass().add("card");
            content.setWrapText(true);
            meta.getStyleClass().add("muted");
            card.getChildren().addAll(content, meta);
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

            setGraphic(card);
        }
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Validation");
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
        // Close only this details window.
        appBar.getScene().getWindow().hide();
    }
}
