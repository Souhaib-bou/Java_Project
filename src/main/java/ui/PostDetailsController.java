package ui;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.ForumComment;
import model.ForumPost;
import model.ModerationReport;
import org.kordamp.ikonli.javafx.FontIcon;
import repo.ForumCommentRepository;
import repo.ForumPostInteractionRepository;
import repo.ForumPostRepository;
import repo.NotificationRepository;
import repo.UserRepository;
import service.ModerationEngine;
import service.NotificationService;
import util.DebugLog;
import util.Session;
import util.InputValidator;
import ui.components.ModerationDialog;
import ui.components.NotificationsDialog;
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
    private Button likePostBtn, sharePostBtn;
    @FXML
    private Label likeCountLabel, shareHintLabel;
    @FXML
    private Button notificationsBtn;
    @FXML
    private Label notificationBadge;

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

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean customMaximized = false;
    private double restoreX = 0;
    private double restoreY = 0;
    private double restoreWidth = 0;
    private double restoreHeight = 0;

    private final ForumCommentRepository commentRepo = new ForumCommentRepository();
    private final ForumPostRepository postRepo = new ForumPostRepository();
    private final ForumPostInteractionRepository interactionRepo = new ForumPostInteractionRepository();
    private final NotificationRepository notificationRepo = new NotificationRepository();
    private final UserRepository userRepo = new UserRepository();
    private final ModerationEngine moderationEngine = new ModerationEngine();
    private final NotificationService notificationService = new NotificationService(notificationRepo, userRepo);
    private final long geminiUserId = userRepo.findUserIdByEmail("gemini@hirely.local");

    private final ObservableList<ForumComment> comments = FXCollections.observableArrayList();

    private ForumPost post;
    private boolean commentSubmissionBusy = false;
    private boolean postSubmissionBusy = false;
    private boolean postLikeBusy = false;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");

    @FXML
    private void initialize() {
        // Status choices visible in the comment editor controls.
        commentStatusBox.setItems(FXCollections.observableArrayList("PENDING", "APPROVED", "REJECTED"));
        commentStatusBox.getSelectionModel().select("PENDING");

        // JavaFX observable list binding for on-screen comments.
        commentListView.setItems(comments);
        commentListView.setCellFactory(lv -> new CommentCardCell(userRepo, geminiUserId));

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
        refreshNotificationBadge();
    }

    public void setPost(ForumPost p) {
        // Called by parent screens immediately after loading this view.
        this.post = p;
        refreshLikeStateFromDb();
        refreshShareStateFromDb();
        refreshNotificationBadge();
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
                .setText((post.getTag() == null || post.getTag().isBlank()) ? "General" : post.getTag());
        statusChip.setText(post.getStatus());

        pinnedChip.setVisible(post.isPinned());
        pinnedChip.setManaged(post.isPinned());

        lockedChip.setVisible(post.isLocked());
        lockedChip.setManaged(post.isLocked());
        refreshLikeUi();
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
        refreshLikeUi();
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
        TextField tag = new TextField(post.getTag() == null ? "" : post.getTag());

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(12));
        gp.addRow(0, new Label("Title"), title);
        gp.addRow(1, new Label("Content"), content);
        gp.addRow(2, new Label("Tag"), tag);

        dialog.getDialogPane().setContent(gp);

        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            List<String> errors = InputValidator.validatePost(title.getText(), content.getText(), tag.getText());
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
            draft.setTag(InputValidator.normalizeNullable(tag.getText()));
            return draft;
        });

        dialog.showAndWait().ifPresent(this::submitPostUpdateWithModeration);
    }

    private void submitCommentWithModeration(ForumComment commentDraft, boolean editing) {
        setCommentSubmissionBusy(true);

        Task<CommentSubmissionOutcome> task = new Task<>() {
            @Override
            protected CommentSubmissionOutcome call() throws Exception {
                ModerationReport report = moderationEngine
                        .analyzeAsync(ModerationEngine.ContentType.COMMENT, commentDraft.getContent())
                        .join();
                commentDraft.setStatus(report.getDecision());
                if (editing) {
                    commentRepo.update(commentDraft);
                } else {
                    long id = commentRepo.insert(commentDraft);
                    commentDraft.setId(id);
                }
                return new CommentSubmissionOutcome(report, editing);
            }
        };

        task.setOnSucceeded(evt -> {
            setCommentSubmissionBusy(false);
            CommentSubmissionOutcome out = task.getValue();

            if (!editing && post != null) {
                notificationService.notifyPostCommented(
                        commentDraft.getPostId(),
                        commentDraft.getId(),
                        commentDraft.getAuthorId(),
                        post.getAuthorId());
            }

            ModerationDialog.show(out.report);
            showInfo(messageForCommentStatus(out.report, out.editing));
            loadComments();
            refreshCommentActionVisibility();
            refreshNotificationBadge();
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
                ModerationReport report = moderationEngine
                        .analyzeAsync(ModerationEngine.ContentType.POST, buildPostModerationText(draft))
                        .join();
                draft.setStatus(report.getDecision());
                draft.setDuplicateScore(report.getDuplicateScore());
                draft.setDuplicateOfPostId(report.getDuplicateOfPostId());
                postRepo.update(draft);
                return new PostUpdateOutcome(report);
            }
        };

        task.setOnSucceeded(evt -> {
            setPostSubmissionBusy(false);
            PostUpdateOutcome out = task.getValue();
            ModerationDialog.show(out.report);
            applyPostDraft(draft);
            showInfo(messageForPostStatus(out.report));
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
        post.setTag(draft.getTag());
        post.setStatus(draft.getStatus());
        post.setDuplicateScore(draft.getDuplicateScore());
        post.setDuplicateOfPostId(draft.getDuplicateOfPostId());
        refreshLikeUi();
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
        if (draft.getTag() != null && !draft.getTag().isBlank()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(draft.getTag().trim());
        }
        return sb.toString();
    }

    private String messageForCommentStatus(ModerationReport report, boolean editing) {
        if (report == null) {
            return editing ? "Comment updated" : "Comment submitted";
        }
        if (report.isFallbackUsed()) {
            return editing ? "Comment updated and submitted for review (AI unavailable)"
                    : "Comment submitted for review (AI unavailable)";
        }
        String status = report.getDecision();
        if ("APPROVED".equals(status)) {
            return editing ? "Comment updated and auto-approved" : "Comment posted (auto-approved)";
        }
        if ("REJECTED".equals(status)) {
            return "Rejected by automated moderation";
        }
        return editing ? "Comment updated and submitted for review" : "Submitted for review";
    }

    private String messageForPostStatus(ModerationReport report) {
        if (report == null) {
            return "Post updated";
        }
        if (report.isFallbackUsed()) {
            return "Post updated and submitted for review (AI unavailable)";
        }
        String status = report.getDecision();
        if ("APPROVED".equals(status)) {
            return "Post updated and auto-approved";
        }
        if ("REJECTED".equals(status)) {
            return "Rejected by automated moderation";
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
    private void onToggleLikePost() {
        if (post == null || postLikeBusy || !isLikeAllowed()) {
            return;
        }

        boolean oldLiked = post.isLikedByCurrentUser();
        int oldCount = post.getLikeCount();

        boolean optimisticLiked = !oldLiked;
        int optimisticCount = Math.max(0, oldCount + (optimisticLiked ? 1 : -1));
        post.setLikedByCurrentUser(optimisticLiked);
        post.setLikeCount(optimisticCount);
        postLikeBusy = true;
        refreshLikeUi();

        Task<LikeToggleOutcome> task = new Task<>() {
            @Override
            protected LikeToggleOutcome call() throws Exception {
                long postId = post.getId();
                long userId = Session.getCurrentUserId();
                boolean nowLiked;
                if (oldLiked) {
                    interactionRepo.removeLike(postId, userId);
                    nowLiked = false;
                } else {
                    interactionRepo.addLike(postId, userId);
                    nowLiked = true;
                }
                int likes = interactionRepo.countLikes(postId);
                return new LikeToggleOutcome(nowLiked, likes);
            }
        };

        task.setOnSucceeded(evt -> {
            postLikeBusy = false;
            LikeToggleOutcome out = task.getValue();
            post.setLikedByCurrentUser(out.nowLiked);
            post.setLikeCount(out.likeCount);
            if (out.nowLiked) {
                notificationService.notifyPostLiked(post.getId(), Session.getCurrentUserId(), post.getAuthorId());
            }
            refreshLikeUi();
            refreshNotificationBadge();
        });

        task.setOnFailed(evt -> {
            postLikeBusy = false;
            post.setLikedByCurrentUser(oldLiked);
            post.setLikeCount(oldCount);
            refreshLikeUi();
            Throwable ex = task.getException();
            DebugLog.error("PostDetailsController", "Failed toggling like for post #" + post.getId(), ex);
            showError("Failed to update like", asException(ex));
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onSharePost() {
        if (post == null) {
            return;
        }
        if (!isShareAllowed()) {
            showWarning("Only approved posts can be shared.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(buildShareText(post));
        Clipboard.getSystemClipboard().setContent(content);

        String hint = "Copied";
        try {
            long postId = post.getId();
            long actorUserId = Session.getCurrentUserId();
            boolean firstShare = interactionRepo.addShare(postId, actorUserId);
            post.setShareCount(interactionRepo.countShares(postId));
            boolean shouldNotifyAuthor = firstShare && post.getAuthorId() != Session.getCurrentUserId();
            if (shouldNotifyAuthor) {
                notificationService.notifyPostShared(post.getId(), Session.getCurrentUserId(), post.getAuthorId());
                hint = "Copied + author notified";
                refreshNotificationBadge();
            }
        } catch (Exception ex) {
            DebugLog.error("PostDetailsController", "Failed recording share for post #" + post.getId(), ex);
        }

        refreshLikeUi();
        showShareHint(hint);
    }

    private void refreshLikeStateFromDb() {
        if (post == null) {
            return;
        }
        try {
            long postId = post.getId();
            long userId = Session.getCurrentUserId();
            post.setLikeCount(interactionRepo.countLikes(postId));
            post.setLikedByCurrentUser(interactionRepo.isLiked(postId, userId));
        } catch (Exception ex) {
            post.setLikeCount(Math.max(0, post.getLikeCount()));
            post.setLikedByCurrentUser(false);
            DebugLog.error("PostDetailsController", "Failed loading like state for post #" + post.getId(), ex);
        }
    }

    private void refreshShareStateFromDb() {
        if (post == null) {
            return;
        }
        try {
            long postId = post.getId();
            post.setShareCount(interactionRepo.countShares(postId));
        } catch (Exception ex) {
            post.setShareCount(Math.max(0, post.getShareCount()));
            DebugLog.error("PostDetailsController", "Failed loading share state for post #" + post.getId(), ex);
        }
    }

    private void refreshLikeUi() {
        if (likeCountLabel != null) {
            likeCountLabel.setText("Likes: " + (post == null ? 0 : Math.max(0, post.getLikeCount())));
        }
        if (likePostBtn != null) {
            likePostBtn.setText(post != null && post.isLikedByCurrentUser() ? "Unlike" : "Like");
            likePostBtn.setDisable(postLikeBusy || !isLikeAllowed());
        }
        if (sharePostBtn != null) {
            int shareCount = post == null ? 0 : Math.max(0, post.getShareCount());
            sharePostBtn.setText("Share (" + shareCount + ")");
            sharePostBtn.setDisable(!isShareAllowed());
        }
    }

    private boolean isLikeAllowed() {
        return post != null && "APPROVED".equalsIgnoreCase(post.getStatus());
    }

    private boolean isShareAllowed() {
        return post != null && "APPROVED".equalsIgnoreCase(post.getStatus());
    }

    private String buildShareText(ForumPost p) {
        String title = p.getTitle() == null ? "" : p.getTitle();
        String body = p.getContent() == null ? "" : p.getContent().replace('\n', ' ').trim();
        String snippet = body.length() <= 120 ? body : body.substring(0, 120) + "...";
        return "[" + title + "] (Post #" + p.getId() + ")\n" + snippet;
    }

    private void showShareHint(String text) {
        shareHintLabel.setText(text == null || text.isBlank() ? "Copied" : text);
        shareHintLabel.setVisible(true);
        shareHintLabel.setManaged(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(evt -> {
            shareHintLabel.setVisible(false);
            shareHintLabel.setManaged(false);
            shareHintLabel.setText("");
        });
        pause.playFromStart();
    }

    @FXML
    private void onOpenNotifications() {
        NotificationsDialog.show(
                Session.getCurrentUserId(),
                notificationRepo,
                this::refreshNotificationBadge,
                this::openPostFromNotification);
    }

    private void refreshNotificationBadge() {
        if (notificationBadge == null) {
            return;
        }
        try {
            int unread = notificationRepo.countUnread(Session.getCurrentUserId());
            notificationBadge.setText(Integer.toString(unread));
            boolean show = unread > 0;
            notificationBadge.setVisible(show);
            notificationBadge.setManaged(show);
        } catch (Exception ex) {
            notificationBadge.setVisible(false);
            notificationBadge.setManaged(false);
            DebugLog.error("PostDetailsController", "Failed loading notification badge", ex);
        }
    }

    private void openPostFromNotification(Long postId) {
        if (postId == null) {
            return;
        }
        try {
            ForumPost target = postRepo.findById(postId);
            if (target == null) {
                showWarning("Post #" + postId + " is no longer available.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/PostDetailsView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 850);
            scene.getStylesheets().add(getClass().getResource(Session.getThemeStylesheetPath()).toExternalForm());
            PostDetailsController ctrl = loader.getController();
            ctrl.setPost(target);

            Stage st = new Stage();
            st.initStyle(javafx.stage.StageStyle.UNDECORATED);
            st.setTitle("Hirely - Post #" + target.getId());
            st.setScene(scene);
            st.sizeToScene();
            st.centerOnScreen();
            st.show();
        } catch (Exception ex) {
            DebugLog.error("PostDetailsController", "Failed opening post from notification #" + postId, ex);
            showError("Failed to open post", ex);
        }
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
        private final ModerationReport report;
        private final boolean editing;

        private CommentSubmissionOutcome(ModerationReport report, boolean editing) {
            this.report = report;
            this.editing = editing;
        }
    }

    private static final class PostUpdateOutcome {
        private final ModerationReport report;

        private PostUpdateOutcome(ModerationReport report) {
            this.report = report;
        }
    }

    private static final class LikeToggleOutcome {
        private final boolean nowLiked;
        private final int likeCount;

        private LikeToggleOutcome(boolean nowLiked, int likeCount) {
            this.nowLiked = nowLiked;
            this.likeCount = likeCount;
        }
    }

    // Shared comment card
    private static class CommentCardCell extends ListCell<ForumComment> {
        private final UserRepository userRepo;
        private final long geminiUserId;
        private final VBox card = new VBox(8);
        private final HBox topRow = new HBox(8);
        private final Label author = new Label();
        private final Label badge = new Label("Gemini");
        private final Label when = new Label();
        private final Label content = new Label();

        CommentCardCell(UserRepository userRepo, long geminiUserId) {
            this.userRepo = userRepo;
            this.geminiUserId = geminiUserId;
            card.getStyleClass().add("comment-card");
            author.getStyleClass().add("comment-author");
            when.getStyleClass().add("post-meta");
            badge.getStyleClass().add("gemini-badge");
            badge.setVisible(false);
            badge.setManaged(false);
            content.setWrapText(true);

            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            topRow.getChildren().addAll(author, badge, spacer, when);
            card.getChildren().addAll(topRow, content);
        }

        @Override
        protected void updateItem(ForumComment c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) {
                setGraphic(null);
                return;
            }
            content.setText(c.getContent() == null ? "" : c.getContent());

            String displayName = userRepo.getDisplayNameById(c.getAuthorId());
            author.setText((displayName == null || displayName.isBlank()) ? ("User #" + c.getAuthorId()) : displayName);
            when.setText(c.getCreatedAt() == null ? "" : DT.format(c.getCreatedAt()));

            boolean isGemini = geminiUserId > 0 && c.getAuthorId() == geminiUserId;
            badge.setVisible(isGemini);
            badge.setManaged(isGemini);
            if (isGemini) {
                if (!card.getStyleClass().contains("comment-card-gemini")) {
                    card.getStyleClass().add("comment-card-gemini");
                }
            } else {
                card.getStyleClass().remove("comment-card-gemini");
            }

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
