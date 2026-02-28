package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
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
import model.InteractionType;
import model.ModerationReport;
import model.TargetType;
import org.kordamp.ikonli.javafx.FontIcon;
import repo.ForumPostRepository;
import repo.InteractionRepository;
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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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
    private FontIcon themeIcon;
    @FXML
    private Button notificationsBtn;
    @FXML
    private Label notificationBadge;

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
    private boolean customMaximized = false;
    private double restoreX = 0;
    private double restoreY = 0;
    private double restoreWidth = 0;
    private double restoreHeight = 0;

    private final ForumPostRepository postRepo = new ForumPostRepository();
    private final InteractionRepository interactionRepo = new InteractionRepository();
    private final NotificationRepository notificationRepo = new NotificationRepository();
    private final UserRepository userRepo = new UserRepository();
    private final ModerationEngine moderationEngine = new ModerationEngine();
    private final NotificationService notificationService = new NotificationService(notificationRepo, userRepo);

    private final ObservableList<ForumPost> allApproved = FXCollections.observableArrayList();
    private final Set<Long> likeToggleInProgress = new HashSet<>();
    private FilteredList<ForumPost> filtered;
    private SortedList<ForumPost> sorted;
    private boolean postSubmissionBusy = false;
    private static final String DEFAULT_HINT = "Tip: Double-click a post to open it.";
    private static final double DUPLICATE_PENDING_THRESHOLD = 0.80;

    @FXML
    private void initialize() {
        // logo
        try {
            logoView.setImage(new Image(getClass().getResourceAsStream("/assets/logo.png")));
        } catch (Exception ignored) {
        }
        hintLabel.setText(DEFAULT_HINT);

        refreshSessionUI();
        syncThemeToggle();
        refreshNotificationBadge();

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
        sortBox.setItems(FXCollections.observableArrayList("New", "Old", "Most liked"));
        sortBox.getSelectionModel().select("New");

        // Post cards in user mode hide moderation status chip.
        postListView.setCellFactory(
                lv -> new ui.components.PostCardCell(userRepo, false, this::onToggleLikeFromFeed, this::onShareFromFeed));

        // Feed pipeline: source list -> filter by query -> sorted view.
        filtered = new FilteredList<>(allApproved, p -> true);
        sorted = new SortedList<>(filtered);
        sorted.setComparator(postComparator(sortBox.getValue()));
        postListView.setItems(sorted);

        sortBox.valueProperty().addListener((obs, o, v) -> sorted.setComparator(postComparator(v)));

        // Live search across title/content/tag.
        searchField.textProperty().addListener((obs, o, v) -> {
            String q = (v == null ? "" : v.trim().toLowerCase());
            filtered.setPredicate(p -> {
                if (q.isEmpty())
                    return true;
                String t = safe(p.getTitle()).toLowerCase();
                String c = safe(p.getContent()).toLowerCase();
                String tag = safe(p.getTag()).toLowerCase();
                return t.contains(q) || c.contains(q) || tag.contains(q);
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
        Comparator<ForumPost> byLikes = Comparator.comparingInt(ForumPost::getLikeCount).reversed()
                .thenComparing(byCreatedDesc);

        Comparator<ForumPost> byDateOrLikes = "Most liked".equalsIgnoreCase(mode)
                ? byLikes
                : ("Old".equalsIgnoreCase(mode) ? byCreatedAsc : byCreatedDesc);

        // pinned always stays on top, then apply New/Old sorting inside each group
        return pinnedFirst.thenComparing(byDateOrLikes);
    }

    @FXML
    private void onRefresh() {
        try {
            // READ (posts): user feed only shows admin-approved posts.
            java.util.List<ForumPost> approved = postRepo.findApproved();
            hydrateLikeState(approved);
            allApproved.setAll(approved);
            refreshSortOrder();
            refreshNotificationBadge();
        } catch (Exception ex) {
            showError("Failed to load feed", ex);
        }
    }

    private void hydrateLikeState(java.util.List<ForumPost> posts) {
        long uid = Session.getCurrentUserId();
        for (ForumPost post : posts) {
            try {
                post.setLikedByCurrentUser(interactionRepo.hasInteraction(
                        TargetType.POST,
                        post.getId(),
                        uid,
                        InteractionType.LIKE));
            } catch (Exception ex) {
                post.setLikedByCurrentUser(false);
                DebugLog.error("UserForumController", "Failed loading like state for post #" + post.getId(), ex);
            }
        }
    }

    private void onToggleLikeFromFeed(ForumPost post) {
        if (post == null) {
            return;
        }
        long postId = post.getId();
        if (likeToggleInProgress.contains(postId)) {
            return;
        }

        boolean oldLiked = post.isLikedByCurrentUser();
        int oldCount = post.getLikeCount();

        boolean optimisticLiked = !oldLiked;
        int optimisticCount = Math.max(0, oldCount + (optimisticLiked ? 1 : -1));
        post.setLikedByCurrentUser(optimisticLiked);
        post.setLikeCount(optimisticCount);
        likeToggleInProgress.add(postId);
        postListView.refresh();
        refreshSortOrder();

        Task<LikeToggleOutcome> task = new Task<>() {
            @Override
            protected LikeToggleOutcome call() throws Exception {
                long uid = Session.getCurrentUserId();
                boolean nowLiked = interactionRepo.toggleInteraction(TargetType.POST, postId, uid, InteractionType.LIKE);
                int latestCount = interactionRepo.countInteractions(TargetType.POST, postId, InteractionType.LIKE);
                return new LikeToggleOutcome(nowLiked, latestCount);
            }
        };

        task.setOnSucceeded(evt -> {
            likeToggleInProgress.remove(postId);
            LikeToggleOutcome out = task.getValue();
            post.setLikedByCurrentUser(out.nowLiked);
            post.setLikeCount(out.likeCount);

            if (out.nowLiked) {
                notificationService.notifyPostLiked(post.getId(), Session.getCurrentUserId(), post.getAuthorId());
            }

            postListView.refresh();
            refreshSortOrder();
        });

        task.setOnFailed(evt -> {
            likeToggleInProgress.remove(postId);
            post.setLikedByCurrentUser(oldLiked);
            post.setLikeCount(oldCount);
            postListView.refresh();
            refreshSortOrder();
            Throwable err = task.getException();
            DebugLog.error("UserForumController", "Failed toggling like for post #" + postId, err);
            showError("Failed to update like", asException(err));
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private String onShareFromFeed(ForumPost post) {
        if (!isShareAllowed(post)) {
            return "Copied";
        }
        try {
            long postId = post.getId();
            long actorUserId = Session.getCurrentUserId();
            boolean firstShare = false;
            if (!interactionRepo.hasInteraction(TargetType.POST, postId, actorUserId, InteractionType.SHARE)) {
                firstShare = interactionRepo.toggleInteraction(TargetType.POST, postId, actorUserId, InteractionType.SHARE);
            }
            post.setShareCount(interactionRepo.countInteractions(TargetType.POST, postId, InteractionType.SHARE));
            postListView.refresh();

            boolean shouldNotifyAuthor = firstShare && post.getAuthorId() != Session.getCurrentUserId();
            if (shouldNotifyAuthor) {
                notificationService.notifyPostShared(post.getId(), Session.getCurrentUserId(), post.getAuthorId());
                refreshNotificationBadge();
                return "Copied + author notified";
            }
            return "Copied";
        } catch (Exception ex) {
            DebugLog.error("UserForumController", "Failed recording share for post #" + post.getId(), ex);
            return "Copied";
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
        if (postSubmissionBusy) {
            return;
        }
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

        TextField tag = new TextField(editing ? safe(existing.getTag()) : "");

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(12));
        gp.addRow(0, new Label("Title"), title);
        gp.addRow(1, new Label("Content"), content);
        gp.addRow(2, new Label("Tag"), tag);

        GridPane.setHgrow(title, Priority.ALWAYS);
        GridPane.setHgrow(tag, Priority.ALWAYS);
        GridPane.setHgrow(content, Priority.ALWAYS);

        ScrollPane sp = new ScrollPane(gp);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setPrefViewportHeight(420);
        dialog.getDialogPane().setContent(sp);

        Node okBtn = dialog.getDialogPane().lookupButton(okType);

        // Block submit and show validation messages before closing dialog.
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            List<String> errors = InputValidator.validatePost(title.getText(), content.getText(), tag.getText());
            if (!errors.isEmpty()) {
                ev.consume(); // prevent dialog from closing
                showValidation(errors);
            }
        });

        // CREATE/UPDATE (posts): collect and normalize user input.
        dialog.setResultConverter(btn -> {
            if (btn != okType)
                return null;

            ForumPost p = new ForumPost();
            if (editing && existing != null) {
                p.setId(existing.getId());
                p.setAuthorId(existing.getAuthorId());
                p.setPinned(existing.isPinned());
                p.setLocked(existing.isLocked());
                p.setCreatedAt(existing.getCreatedAt());
            } else {
                p.setAuthorId(Session.getCurrentUserId());
            }

            String t = InputValidator.norm(title.getText());
            String c = InputValidator.norm(content.getText());
            String normalizedTag = InputValidator.normalizeSingleTag(tag.getText());

            // Safe because we block with validation above
            p.setTitle(t);
            p.setContent(c);
            p.setTag(normalizedTag);
            return p;
        });

        dialog.showAndWait().ifPresent(p -> submitPostWithModeration(p, editing));
    }

    private void submitPostWithModeration(ForumPost postDraft, boolean editing) {
        setPostSubmissionBusy(true);

        Task<PostSubmissionOutcome> task = new Task<>() {
            @Override
            protected PostSubmissionOutcome call() throws Exception {
                ModerationReport report = moderationEngine
                        .analyzePostAsync(buildModerationText(postDraft))
                        .join();
                String userTag = InputValidator.normalizeSingleTag(postDraft.getTag());
                String predictedTag = InputValidator.normalizeSingleTag(resolvePredictedCategory(report));
                postDraft.setTag(userTag != null ? userTag : (predictedTag != null ? predictedTag : "#General"));
                postDraft.setDuplicateScore(report.getDuplicateScore());
                postDraft.setDuplicateOfPostId(report.getDuplicateOfPostId());

                boolean forcedDuplicatePending = report.getDuplicateScore() >= DUPLICATE_PENDING_THRESHOLD;
                if (forcedDuplicatePending) {
                    postDraft.setStatus("PENDING");
                    report.setDecision("PENDING");
                    report.getReasons().add(String.format("Possible duplicate (score %.2f)", report.getDuplicateScore()));
                } else {
                    postDraft.setStatus(report.getDecision());
                }

                if (editing) {
                    postRepo.update(postDraft);
                } else {
                    long newId = postRepo.insert(postDraft);
                    postDraft.setId(newId);
                }
                return new PostSubmissionOutcome(report, editing, forcedDuplicatePending);
            }
        };

        task.setOnSucceeded(evt -> {
            setPostSubmissionBusy(false);
            PostSubmissionOutcome out = task.getValue();
            ModerationDialog.show(out.report);
            showInfo(messageForPostStatus(out.report, out.editing, out.forcedDuplicatePending));
            onRefresh();
        });

        task.setOnFailed(evt -> {
            setPostSubmissionBusy(false);
            showError("Failed to save post", asException(task.getException()));
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private String buildModerationText(ForumPost p) {
        StringBuilder sb = new StringBuilder();
        if (p.getTitle() != null && !p.getTitle().isBlank()) {
            sb.append(p.getTitle().trim());
        }
        if (p.getContent() != null && !p.getContent().isBlank()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(p.getContent().trim());
        }
        return sb.toString();
    }

    private String resolvePredictedCategory(ModerationReport report) {
        if (report == null) {
            return "General";
        }
        String predicted = report.getPredictedCategory();
        if (predicted == null || predicted.isBlank()) {
            return "General";
        }
        return predicted.trim();
    }

    private String messageForPostStatus(ModerationReport report, boolean editing, boolean forcedDuplicatePending) {
        if (forcedDuplicatePending) {
            return editing ? "Possible duplicate: post updated and sent for manual review"
                    : "Possible duplicate: post submitted for manual review";
        }
        if (report == null) {
            return editing ? "Post updated" : "Post submitted";
        }
        if (report.isFallbackUsed()) {
            return editing ? "Post updated and submitted for review (AI unavailable)"
                    : "Post submitted for review (AI unavailable)";
        }
        String status = report.getDecision();
        if ("APPROVED".equals(status)) {
            return editing ? "Post updated and auto-approved" : "Post published (auto-approved)";
        }
        if ("REJECTED".equals(status)) {
            return "Rejected by automated moderation";
        }
        return editing ? "Post updated and submitted for review" : "Post submitted for review";
    }

    private void setPostSubmissionBusy(boolean busy) {
        postSubmissionBusy = busy;
        newPostBtn.setDisable(busy);
        hintLabel.setText(busy ? "Checking content..." : DEFAULT_HINT);
    }

    private Exception asException(Throwable ex) {
        return ex instanceof Exception e ? e : new Exception(ex == null ? "Unknown error" : ex.getMessage(), ex);
    }

    private void refreshSortOrder() {
        if (sorted == null) {
            return;
        }
        String mode = sortBox == null ? "New" : sortBox.getValue();
        sorted.setComparator(postComparator(mode));
    }

    private boolean isShareAllowed(ForumPost post) {
        return post != null && "APPROVED".equalsIgnoreCase(safe(post.getStatus()));
    }

    private static final class PostSubmissionOutcome {
        private final ModerationReport report;
        private final boolean editing;
        private final boolean forcedDuplicatePending;

        private PostSubmissionOutcome(ModerationReport report, boolean editing, boolean forcedDuplicatePending) {
            this.report = report;
            this.editing = editing;
            this.forcedDuplicatePending = forcedDuplicatePending;
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
        refreshNotificationBadge();
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
    private void onOpenNotifications() {
        NotificationsDialog.show(
                Session.getCurrentUserId(),
                notificationRepo,
                this::refreshNotificationBadge,
                this::openPostFromNotification);
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
            DebugLog.error("UserForumController", "Failed loading notification badge", ex);
        }
    }

    private void openPostFromNotification(Long postId) {
        if (postId == null) {
            return;
        }
        try {
            ForumPost post = postRepo.findById(postId);
            if (post == null) {
                showInfo("Post #" + postId + " is no longer available.");
                return;
            }
            openPostDetails(post);
        } catch (Exception ex) {
            DebugLog.error("UserForumController", "Failed opening post from notification #" + postId, ex);
            showError("Failed to open post from notification", ex);
        }
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
        // Main window close exits the application.
        System.exit(0);
    }
}
