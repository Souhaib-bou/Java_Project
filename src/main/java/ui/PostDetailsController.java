package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.ForumComment;
import model.ForumPost;
import org.kordamp.ikonli.javafx.FontIcon;
import repo.ForumCommentRepository;
import repo.ForumPostRepository;
import repo.UserRepository;
import util.ModerationService;
import util.WikipediaClient;
import util.Session;
import util.InputValidator;
import java.awt.Desktop;
import java.net.URI;
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
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private FontIcon themeIcon;

    @FXML
    private TextField wikiQueryField;
    @FXML
    private Button wikiLoadBtn;
    @FXML
    private ProgressIndicator wikiLoading;
    @FXML
    private ScrollPane wikiScroll;
    @FXML
    private Label wikiSummaryLabel;
    @FXML
    private Hyperlink wikiLink;

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean customMaximized = false;
    private double restoreX = 0;
    private double restoreY = 0;
    private double restoreWidth = 0;
    private double restoreHeight = 0;

    private final ForumCommentRepository commentRepo = new ForumCommentRepository();
    private final ForumPostRepository postRepo = new ForumPostRepository();
    private final UserRepository userRepo = new UserRepository();
    private final ModerationService moderationService = new ModerationService();

    private final ObservableList<ForumComment> comments = FXCollections.observableArrayList();

    private ForumPost post;
    private String wikiUrl;
    private boolean commentSubmissionBusy = false;
    private boolean postSubmissionBusy = false;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");
    private static final double WIKI_MIN = 90;
    private static final double WIKI_MAX = 240;

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

        syncThemeToggle();
    }

    public void setPost(ForumPost p) {
        // Called by parent screens immediately after loading this view.
        this.post = p;
        render();
        loadComments();
        applyPermissions();
        updateCommentUiState();
        refreshCommentActionVisibility();

        String title = (post == null || post.getTitle() == null) ? "" : post.getTitle().trim();
        if (wikiQueryField != null) {
            wikiQueryField.setText(title);
        }
        if (wikiSummaryLabel != null) {
            wikiSummaryLabel.setText("");
        }
        if (wikiLink != null) {
            wikiLink.setVisible(false);
            wikiLink.setManaged(false);
        }
        if (wikiScroll != null) {
            wikiScroll.setPrefHeight(WIKI_MIN);
        }
        wikiUrl = null;
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
        editPostBtn.setDisable(postSubmissionBusy);
        deletePostBtn.setVisible(isOwner);
        deletePostBtn.setManaged(isOwner);
        deletePostBtn.setDisable(postSubmissionBusy);
    }

    private boolean isCommentingBlocked() {
        // Comments are blocked when post is absent/locked or current session is admin.
        return post == null || post.isLocked() || Session.isAdmin();
    }

    private void updateCommentUiState() {
        boolean blocked = isCommentingBlocked() || commentSubmissionBusy;

        commentArea.setDisable(blocked);
        addCommentBtn.setDisable(blocked);

        boolean hasSelection = commentListView.getSelectionModel().getSelectedItem() != null;
        updateCommentBtn.setDisable(blocked || !hasSelection);
        deleteCommentBtn.setDisable(blocked || !hasSelection);

        if (post == null) {
            lockHint.setText("");
        } else if (commentSubmissionBusy) {
            lockHint.setText("Checking comment...");
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
        boolean canAdd = !isAdmin && !locked && !commentSubmissionBusy;
        addCommentBtn.setVisible(canAdd);
        addCommentBtn.setManaged(canAdd);

        // Update/Delete: only if selected comment is mine + not locked + not admin
        boolean canModifySelected = !isAdmin && !locked && hasSel && isOwnerOfSelected && !commentSubmissionBusy;

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
        submitCommentWithModeration(c, false);
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

        ForumComment draft = new ForumComment();
        draft.setId(selected.getId());
        draft.setPostId(selected.getPostId());
        draft.setAuthorId(selected.getAuthorId());
        draft.setCreatedAt(selected.getCreatedAt());
        draft.setContent(InputValidator.norm(commentArea.getText()));
        submitCommentWithModeration(draft, true);
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
        dialog.getDialogPane().getStylesheets()
                .add(getClass().getResource(Session.getThemeStylesheetPath()).toExternalForm());
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

            ForumPost draft = new ForumPost();
            draft.setId(post.getId());
            draft.setAuthorId(post.getAuthorId());
            draft.setPinned(post.isPinned());
            draft.setLocked(post.isLocked());
            draft.setCreatedAt(post.getCreatedAt());
            draft.setTitle(InputValidator.norm(title.getText()));
            draft.setContent(InputValidator.norm(content.getText()));
            draft.setCategory(InputValidator.normalizeNullable(category.getText()));
            return draft;
        });

        dialog.showAndWait().ifPresent(this::submitPostUpdateWithModeration);
    }

    private void submitCommentWithModeration(ForumComment commentDraft, boolean editing) {
        setCommentSubmissionBusy(true);

        Task<CommentSubmissionOutcome> task = new Task<>() {
            @Override
            protected CommentSubmissionOutcome call() throws Exception {
                ModerationService.ModerationResult moderation =
                        moderationService.decideStatus(commentDraft.getContent());

                if (ModerationService.STATUS_REJECTED.equals(moderation.getStatus())) {
                    return CommentSubmissionOutcome.rejected(moderation);
                }

                commentDraft.setStatus(moderation.getStatus());
                if (editing) {
                    commentRepo.update(commentDraft);
                } else {
                    long id = commentRepo.insert(commentDraft);
                    commentDraft.setId(id);
                }
                return CommentSubmissionOutcome.saved(moderation, editing);
            }
        };

        task.setOnSucceeded(evt -> {
            setCommentSubmissionBusy(false);
            CommentSubmissionOutcome out = task.getValue();
            if (out.rejected) {
                showWarning("Rejected by automated moderation");
                return;
            }

            showInfo(messageForCommentStatus(out.status, out.usedFallback, out.editing));
            loadComments();
            refreshCommentActionVisibility();
        });

        task.setOnFailed(evt -> {
            setCommentSubmissionBusy(false);
            showError("Failed to save comment", asException(task.getException()));
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void submitPostUpdateWithModeration(ForumPost draft) {
        setPostSubmissionBusy(true);

        Task<PostUpdateOutcome> task = new Task<>() {
            @Override
            protected PostUpdateOutcome call() throws Exception {
                ModerationService.ModerationResult moderation =
                        moderationService.decideStatus(buildPostModerationText(draft));

                if (ModerationService.STATUS_REJECTED.equals(moderation.getStatus())) {
                    return PostUpdateOutcome.rejected(moderation);
                }

                draft.setStatus(moderation.getStatus());
                postRepo.update(draft);
                return PostUpdateOutcome.saved(moderation);
            }
        };

        task.setOnSucceeded(evt -> {
            setPostSubmissionBusy(false);
            PostUpdateOutcome out = task.getValue();
            if (out.rejected) {
                showWarning("Rejected by automated moderation");
                return;
            }

            applyPostDraft(draft);
            showInfo(messageForPostStatus(out.status, out.usedFallback));
            render();
            applyPermissions();
        });

        task.setOnFailed(evt -> {
            setPostSubmissionBusy(false);
            showError("Failed to update post", asException(task.getException()));
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void applyPostDraft(ForumPost draft) {
        if (post == null) {
            post = draft;
            return;
        }
        post.setTitle(draft.getTitle());
        post.setContent(draft.getContent());
        post.setCategory(draft.getCategory());
        post.setStatus(draft.getStatus());
    }

    private String buildPostModerationText(ForumPost draft) {
        StringBuilder sb = new StringBuilder();
        if (draft.getTitle() != null && !draft.getTitle().isBlank()) {
            sb.append(draft.getTitle().trim());
        }
        if (draft.getContent() != null && !draft.getContent().isBlank()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(draft.getContent().trim());
        }
        if (draft.getCategory() != null && !draft.getCategory().isBlank()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(draft.getCategory().trim());
        }
        return sb.toString();
    }

    private String messageForCommentStatus(String status, boolean usedFallback, boolean editing) {
        if (usedFallback) {
            return editing ? "Comment updated and submitted for review (moderation service unavailable)"
                    : "Comment submitted for review (moderation service unavailable)";
        }
        if (ModerationService.STATUS_APPROVED.equals(status)) {
            return editing ? "Comment updated and auto-approved" : "Comment posted (auto-approved)";
        }
        return editing ? "Comment updated and submitted for review" : "Submitted for review";
    }

    private String messageForPostStatus(String status, boolean usedFallback) {
        if (usedFallback) {
            return "Post updated and submitted for review (moderation service unavailable)";
        }
        if (ModerationService.STATUS_APPROVED.equals(status)) {
            return "Post updated and auto-approved";
        }
        return "Post updated and submitted for review";
    }

    private void setCommentSubmissionBusy(boolean busy) {
        commentSubmissionBusy = busy;
        updateCommentUiState();
        refreshCommentActionVisibility();
    }

    private void setPostSubmissionBusy(boolean busy) {
        postSubmissionBusy = busy;
        editPostBtn.setDisable(busy);
        deletePostBtn.setDisable(busy);
    }

    private Exception asException(Throwable ex) {
        return ex instanceof Exception e ? e : new Exception(ex == null ? "Unknown error" : ex.getMessage(), ex);
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

    private static final class CommentSubmissionOutcome {
        private final String status;
        private final boolean usedFallback;
        private final boolean rejected;
        private final boolean editing;

        private CommentSubmissionOutcome(String status, boolean usedFallback, boolean rejected, boolean editing) {
            this.status = status;
            this.usedFallback = usedFallback;
            this.rejected = rejected;
            this.editing = editing;
        }

        private static CommentSubmissionOutcome saved(ModerationService.ModerationResult result, boolean editing) {
            return new CommentSubmissionOutcome(result.getStatus(), result.isUsedFallback(), false, editing);
        }

        private static CommentSubmissionOutcome rejected(ModerationService.ModerationResult result) {
            return new CommentSubmissionOutcome(result.getStatus(), result.isUsedFallback(), true, false);
        }
    }

    private static final class PostUpdateOutcome {
        private final String status;
        private final boolean usedFallback;
        private final boolean rejected;

        private PostUpdateOutcome(String status, boolean usedFallback, boolean rejected) {
            this.status = status;
            this.usedFallback = usedFallback;
            this.rejected = rejected;
        }

        private static PostUpdateOutcome saved(ModerationService.ModerationResult result) {
            return new PostUpdateOutcome(result.getStatus(), result.isUsedFallback(), false);
        }

        private static PostUpdateOutcome rejected(ModerationService.ModerationResult result) {
            return new PostUpdateOutcome(result.getStatus(), result.isUsedFallback(), true);
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
    private void onLoadWikipedia() {
        String query = wikiQueryField == null ? "" : wikiQueryField.getText();
        query = query == null ? "" : query.trim();

        if (query.isBlank()) {
            String fallback = post == null ? "" : post.getTitle();
            fallback = fallback == null ? "" : fallback.trim();
            if (fallback.isBlank()) {
                showWarning("Type something to search.");
                return;
            }
            query = fallback;
            wikiQueryField.setText(query);
        }

        setWikiLoading(true);
        if (wikiSummaryLabel != null) {
            wikiSummaryLabel.setText("");
        }
        wikiLink.setVisible(false);
        wikiLink.setManaged(false);
        wikiUrl = null;

        String finalQuery = query;
        Task<WikipediaClient.WikiSummary> task = new Task<>() {
            @Override
            protected WikipediaClient.WikiSummary call() throws Exception {
                return WikipediaClient.fetchSummary(finalQuery);
            }
        };

        task.setOnSucceeded(evt -> {
            WikipediaClient.WikiSummary summary = task.getValue();
            wikiSummaryLabel.setText(summary.extract);
            wikiUrl = summary.url;
            if (wikiUrl != null && !wikiUrl.isBlank()) {
                wikiLink.setText("Open on Wikipedia");
                wikiLink.setVisible(true);
                wikiLink.setManaged(true);
            }
            Platform.runLater(this::updateWikiBoxHeight);
            setWikiLoading(false);
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            wikiSummaryLabel.setText(getWikiErrorMessage(ex));
            wikiUrl = null;
            wikiLink.setVisible(false);
            wikiLink.setManaged(false);
            Platform.runLater(this::updateWikiBoxHeight);
            setWikiLoading(false);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onOpenWiki() {
        if (wikiUrl == null || wikiUrl.isBlank()) {
            showWarning("No Wikipedia link available.");
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            showWarning("Opening links is not supported on this system.");
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(wikiUrl));
        } catch (Exception ex) {
            showError("Failed to open link", ex);
        }
    }

    private void setWikiLoading(boolean loading) {
        wikiLoadBtn.setDisable(loading);
        wikiLoading.setVisible(loading);
        wikiLoading.setManaged(loading);
    }

    private void updateWikiBoxHeight() {
        if (wikiSummaryLabel == null || wikiScroll == null) {
            return;
        }
        wikiSummaryLabel.applyCss();
        wikiSummaryLabel.layout();
        double width = wikiScroll.getWidth();
        if (width <= 0) {
            width = 600;
        }
        double textHeight = wikiSummaryLabel.prefHeight(width - 20);
        double target = Math.min(WIKI_MAX, Math.max(WIKI_MIN, textHeight + 20));
        wikiScroll.setPrefHeight(target);
    }

    private String getWikiErrorMessage(Throwable ex) {
        if (ex instanceof WikipediaClient.NotFoundException) {
            return "No Wikipedia summary found.";
        }
        if (isNetworkError(ex)) {
            return "Couldn't reach Wikipedia (offline?).";
        }
        return "Failed to load Wikipedia summary.";
    }

    private boolean isNetworkError(Throwable ex) {
        if (ex == null) {
            return false;
        }
        if (ex instanceof java.net.http.HttpTimeoutException) {
            return true;
        }
        if (ex instanceof java.net.UnknownHostException) {
            return true;
        }
        if (ex instanceof java.net.ConnectException) {
            return true;
        }
        if (ex instanceof java.io.IOException) {
            return true;
        }
        return isNetworkError(ex.getCause());
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
        // Close only this details window.
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
