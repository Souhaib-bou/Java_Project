package ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import model.ForumComment;
import model.ForumPost;
import model.ModerationReport;
import org.kordamp.ikonli.javafx.FontIcon;
import repo.ForumCommentRepository;
import repo.ForumPostRepository;
import repo.UserRepository;
import service.ModerationEngine;
import ui.components.AdminModerationDialog;
import util.DebugLog;
import util.Session;
import util.InputValidator;
import java.util.List;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin control panel controller.
 * Supports full post moderation plus comment management across all posts.
 */
public class AdminForumController {

    // Top bar
    @FXML
    private ImageView logoView;
    @FXML
    private HBox appBar;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortBox;
    @FXML
    private Button newPostBtn;
    @FXML
    private Label currentUserLabel;
    @FXML
    private ComboBox<DevUser> devUserBox;
    @FXML
    private Button userForumBtn;
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private FontIcon themeIcon;
    @FXML
    private Button btnAiAuditLast50;

    // Window controls
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean customMaximized = false;
    private double restoreX = 0;
    private double restoreY = 0;
    private double restoreWidth = 0;
    private double restoreHeight = 0;

    // Tabs
    @FXML
    private TabPane mainTabs;
    @FXML
    private Tab postTab;

    // Posts feed (Tab 1)
    @FXML
    private ListView<ForumPost> postListView;
    @FXML
    private Label postTitleLabel;
    @FXML
    private Label postMetaLabel;
    @FXML
    private Label postContentLabel;
    @FXML
    private Button editPostBtn;
    @FXML
    private Button deletePostBtn;
    @FXML
    private Button btnAiAnalyzePost;
    @FXML
    private Button btnAiReclassify;

    // Full Post View (Tab 2)
    @FXML
    private Label postTitleLabel2;
    @FXML
    private Label postMetaLabel2;
    @FXML
    private Label postContentLabel2;
    @FXML
    private Label categoryChip2;
    @FXML
    private Label statusChip2;
    @FXML
    private Label pinnedChip2;
    @FXML
    private Label lockedChip2;
    @FXML
    private ListView<ForumComment> commentListView2;
    @FXML
    private TextArea commentArea2;
    @FXML
    private ComboBox<String> commentStatusBox2;
    @FXML
    private Button addCommentBtn2;
    @FXML
    private Button updateCommentBtn2;
    @FXML
    private Button deleteCommentBtn2;
    @FXML
    private Button btnAiAnalyzeComment;

    private final ForumPostRepository postRepo = new ForumPostRepository();
    private final ForumCommentRepository commentRepo = new ForumCommentRepository();
    private final UserRepository userRepo = new UserRepository();
    private final ModerationEngine moderationEngine = new ModerationEngine();

    private final Map<Long, String> userNameCache = new HashMap<>();

    private final ObservableList<ForumPost> allPosts = FXCollections.observableArrayList();
    private final ObservableList<ForumComment> comments = FXCollections.observableArrayList();

    private FilteredList<ForumPost> filteredPosts;
    private SortedList<ForumPost> sortedPosts;

    private ForumPost selectedPost;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");
    private static final String AI_POST_BUTTON_TEXT = "Analyze Post (AI)";
    private static final String AI_COMMENT_BUTTON_TEXT = "Analyze Comment (AI)";
    private static final String AI_RECLASSIFY_BUTTON_TEXT = "AI Reclassify";
    private static final String AI_AUDIT_BUTTON_TEXT = "AI Audit (Last 50)";
    private static final String AI_ANALYZING_TEXT = "Analyzing...";
    private static final int AI_AUDIT_LIMIT = 50;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]{2,}");

    private boolean aiPostAnalysisInProgress = false;
    private boolean aiCommentAnalysisInProgress = false;
    private boolean aiReclassifyInProgress = false;
    private boolean aiAuditInProgress = false;

    @FXML
    private void initialize() {

        // Logo
        try {
            logoView.setImage(new Image(getClass().getResourceAsStream("/assets/logo.png")));
        } catch (Exception ignored) {
        }

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

        // Sort
        sortBox.setItems(FXCollections.observableArrayList("New", "Old"));
        sortBox.getSelectionModel().select("New");

        // Comment status
        commentStatusBox2.setItems(FXCollections.observableArrayList("PENDING", "APPROVED", "REJECTED"));
        commentStatusBox2.getSelectionModel().select("PENDING");

        refreshSessionUI();
        syncThemeToggle();

        // Dev-only identity switcher for quickly testing data ownership flows.
        devUserBox.setItems(FXCollections.observableArrayList(
                new DevUser(1, "Ali (USER)", util.Session.Role.USER),
                new DevUser(2, "Mohammed (USER)", util.Session.Role.USER)));

        devUserBox.getSelectionModel().select(0);

        devUserBox.valueProperty().addListener((obs, oldV, v) -> {
            if (v == null)
                return;
            util.Session.set(v.id, v.role);
            refreshSessionUI();
            onRefreshPosts();
        });

        // Current User
        currentUserLabel.setText("Logged in as Admin");

        // List Cells
        // Admin post cards display status chip for moderation visibility.
        postListView.setCellFactory(lv -> new ui.components.PostCardCell(userRepo, true));
        commentListView2.setCellFactory(lv -> new CommentCardCell());

        // Filter + sort pipeline for large post feeds.
        filteredPosts = new FilteredList<>(allPosts, p -> true);
        sortedPosts = new SortedList<>(filteredPosts);
        sortedPosts.setComparator(postComparator(sortBox.getValue()));
        postListView.setItems(sortedPosts);

        sortBox.valueProperty().addListener((obs, oldV, v) -> sortedPosts.setComparator(postComparator(v)));

        // Live search over title/tag/content.
        searchField.textProperty().addListener((obs, oldV, v) -> {
            String q = v == null ? "" : v.trim().toLowerCase();
            filteredPosts.setPredicate(p -> {
                if (q.isEmpty())
                    return true;
                return safe(p.getTitle()).toLowerCase().contains(q)
                        || safe(p.getTag()).toLowerCase().contains(q)
                        || safe(p.getContent()).toLowerCase().contains(q);
            });
        });

        // Selection: Post -> updates both views
        postListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, p) -> {
            selectedPost = p;
            renderSelectedPost();
            loadComments();
            refreshAiAnalyzeButtonState();
        });

        // Comment selection -> fill form
        commentListView2.getSelectionModel().selectedItemProperty().addListener((obs, oldV, c) -> {
            if (c == null) {
                refreshAdminCommentActionVisibility();
                refreshAiAnalyzeButtonState();
                return;
            }
            commentArea2.setText(c.getContent());
            commentStatusBox2.setValue(c.getStatus());
            refreshAdminCommentActionVisibility();
            refreshAiAnalyzeButtonState();
        });

        // Disable buttons depending on selection
        editPostBtn.disableProperty().bind(postListView.getSelectionModel().selectedItemProperty().isNull());
        deletePostBtn.disableProperty().bind(postListView.getSelectionModel().selectedItemProperty().isNull());

        BooleanBinding noPost = postListView.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding postLocked = Bindings.createBooleanBinding(() -> {
            ForumPost p = postListView.getSelectionModel().getSelectedItem();
            return p != null && p.isLocked();
        }, postListView.getSelectionModel().selectedItemProperty());

        BooleanBinding cannotComment = noPost.or(postLocked);

        commentArea2.disableProperty().bind(cannotComment);
        addCommentBtn2.disableProperty().bind(cannotComment);

        updateCommentBtn2.disableProperty().bind(commentListView2.getSelectionModel().selectedItemProperty().isNull());
        deleteCommentBtn2.disableProperty().bind(commentListView2.getSelectionModel().selectedItemProperty().isNull());

        refreshAiAnalyzeButtonState();

        // First load
        onRefreshPosts();
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

    private String displayName(long userId) {
        return userNameCache.computeIfAbsent(userId, userRepo::getDisplayNameById);
    }

    private String formatTime(LocalDateTime dt) {
        return dt == null ? "" : DT.format(dt);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    // =========================
    // Posts actions
    // =========================

    @FXML
    private void onRefreshPosts() {
        try {
            // READ (posts): admin gets full feed, including pending/rejected.
            allPosts.setAll(postRepo.findAll());
            annotateDuplicateScores(allPosts);
            if (!allPosts.isEmpty() && postListView.getSelectionModel().getSelectedItem() == null) {
                postListView.getSelectionModel().selectFirst();
            }
            refreshAiAnalyzeButtonState();
        } catch (Exception ex) {
            showError("Failed to load posts", ex);
        }
    }

    @FXML
    private void onViewFullPost() {
        if (selectedPost != null) {
            mainTabs.getSelectionModel().select(postTab);
        }
    }

    @FXML
    private void onNewPost() {
        showPostEditor(null);
    }

    @FXML
    private void onEditPost() {
        ForumPost p = postListView.getSelectionModel().getSelectedItem();
        if (p != null)
            showPostEditor(p);
    }

    @FXML
    private void onDeletePost() {
        // DELETE (posts): moderation delete flow with confirmation.
        ForumPost p = postListView.getSelectionModel().getSelectedItem();
        if (p == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete post #" + p.getId() + "?");
        confirm.setContentText("This will also delete its comments.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;

        try {
            postRepo.delete(p.getId());
            showInfo("Post deleted");
            selectedPost = null;
            onRefreshPosts();
            mainTabs.getSelectionModel().select(0); // back to feed
        } catch (Exception ex) {
            showError("Failed to delete post", ex);
        }
    }

    private void showPostEditor(ForumPost existing) {
        // CREATE/UPDATE (posts): admin editor includes status/pin/lock controls.
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
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("PENDING", "APPROVED", "REJECTED"));
        status.setValue(editing ? existing.getStatus() : "PENDING");

        CheckBox pinned = new CheckBox("Pinned");
        pinned.setSelected(editing && existing.isPinned());
        CheckBox locked = new CheckBox("Locked");
        locked.setSelected(editing && existing.isLocked());

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(12));
        gp.addRow(0, new Label("Title"), title);
        gp.addRow(1, new Label("Content"), content);
        gp.addRow(2, new Label("Tag"), tag);
        gp.addRow(3, new Label("Status"), status);
        gp.addRow(4, pinned, locked);

        GridPane.setHgrow(title, Priority.ALWAYS);
        GridPane.setHgrow(tag, Priority.ALWAYS);
        GridPane.setHgrow(content, Priority.ALWAYS);

        ScrollPane sp = new ScrollPane(gp);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setPrefViewportHeight(420);
        dialog.getDialogPane().setContent(sp);

        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        // Keep dialog open until all post validation errors are fixed.
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
            ForumPost p = editing ? existing : new ForumPost();
            p.setAuthorId(Session.getCurrentUserId());
            p.setTitle(InputValidator.norm(title.getText()));
            p.setContent(InputValidator.norm(content.getText()));
            p.setTag(InputValidator.normalizeNullable(tag.getText()));
            p.setStatus(status.getValue());
            p.setPinned(pinned.isSelected());
            p.setLocked(locked.isSelected());
            return p;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                if (editing) {
                    postRepo.update(p);
                } else {
                    long newId = postRepo.insert(p);
                    p.setId(newId);
                }
                showInfo("Post saved");
                onRefreshPosts();
                selectPostById(p.getId());
            } catch (Exception ex) {
                showError("Failed to save post", ex);
            }
        });
    }

    private void selectPostById(long id) {
        for (ForumPost p : sortedPosts) {
            if (p.getId() == id) {
                postListView.getSelectionModel().select(p);
                postListView.scrollTo(p);
                return;
            }
        }
    }

    private void renderSelectedPost() {
        // Keep both tab previews synchronized with current selection.
        if (selectedPost == null) {
            // Tab 1
            postTitleLabel.setText("Select a post");
            postMetaLabel.setText("");
            postContentLabel.setText("");
            // Tab 2
            postTitleLabel2.setText("Select a post");
            postMetaLabel2.setText("");
            postContentLabel2.setText("");
            categoryChip2.setText("");
            statusChip2.setText("");
            pinnedChip2.setVisible(false);
            pinnedChip2.setManaged(false);
            lockedChip2.setVisible(false);
            lockedChip2.setManaged(false);
            comments.clear();
            refreshAdminCommentActionVisibility();
            return;
        }

        ForumPost p = selectedPost;
        String author = displayName(p.getAuthorId());
        String when = formatTime(p.getCreatedAt());
        String metaText = author + (when.isBlank() ? "" : " - " + when);

        // Update Tab 1 (Preview)
        postTitleLabel.setText(p.getTitle());
        postContentLabel.setText(safe(p.getContent()));
        postMetaLabel.setText(metaText);

        // Update Tab 2 (Full View)
        postTitleLabel2.setText(p.getTitle());
        postContentLabel2.setText(safe(p.getContent()));
        postMetaLabel2.setText(metaText);
        categoryChip2.setText(safe(p.getTag()).isBlank() ? "General" : p.getTag());
        statusChip2.setText(safe(p.getStatus()));
        pinnedChip2.setVisible(p.isPinned());
        pinnedChip2.setManaged(p.isPinned());
        lockedChip2.setVisible(p.isLocked());
        lockedChip2.setManaged(p.isLocked());
        refreshAdminCommentActionVisibility();
    }

    private void refreshAdminCommentActionVisibility() {
        ForumPost p = selectedPost;
        boolean hasPost = p != null;
        boolean locked = hasPost && p.isLocked();

        ForumComment sel = commentListView2.getSelectionModel().getSelectedItem();
        boolean hasSel = sel != null;

        // Add comment: only when post exists and not locked
        boolean showAdd = hasPost && !locked;
        addCommentBtn2.setVisible(showAdd);
        addCommentBtn2.setManaged(showAdd);

        // Update/Delete: only when a comment is selected AND not locked
        boolean showModify = hasPost && !locked && hasSel;
        updateCommentBtn2.setVisible(showModify);
        updateCommentBtn2.setManaged(showModify);

        deleteCommentBtn2.setVisible(showModify);
        deleteCommentBtn2.setManaged(showModify);
        refreshAiAnalyzeButtonState();
    }

    // =========================
    // Comments actions
    // =========================

    @FXML
    private void onRefreshComments2() {
        loadComments();
    }

    private void loadComments() {
        try {
            // READ (comments): admin sees all comment statuses for selected post.
            comments.clear();
            if (selectedPost == null) {
                commentListView2.setItems(comments);
                refreshAiAnalyzeButtonState();
                return;
            }
            comments.setAll(commentRepo.findByPostId(selectedPost.getId()));
            commentListView2.setItems(comments);
            commentListView2.getSelectionModel().clearSelection();
            commentArea2.clear();
            commentStatusBox2.setValue("PENDING");
            refreshAdminCommentActionVisibility();
            refreshAiAnalyzeButtonState();
        } catch (Exception ex) {
            showError("Failed to load comments", ex);
        }
    }

    @FXML
    private void onAiAnalyzePost() {
        ForumPost post = postListView.getSelectionModel().getSelectedItem();
        if (post == null) {
            refreshAiAnalyzeButtonState();
            return;
        }

        aiPostAnalysisInProgress = true;
        btnAiAnalyzePost.setText(AI_ANALYZING_TEXT);
        refreshAiAnalyzeButtonState();

        String postText = safe(post.getTitle()) + "\n\n" + safe(post.getContent());
        moderationEngine.analyzePostAsync(postText)
                .whenComplete((report, error) -> Platform.runLater(() -> {
                    aiPostAnalysisInProgress = false;
                    btnAiAnalyzePost.setText(AI_POST_BUTTON_TEXT);
                    refreshAiAnalyzeButtonState();

                    if (error != null) {
                        showAiAnalysisError("post", error);
                        return;
                    }
                    if (report == null) {
                        showAiAnalysisError("post", new IllegalStateException("No moderation report returned"));
                        return;
                    }

                    String identifier = "Post #" + post.getId();
                    if (report.isFallbackUsed()) {
                        showAiFallbackWarning("post", report);
                    }
                    post.setDuplicateScore(report.getDuplicateScore());
                    post.setDuplicateOfPostId(report.getDuplicateOfPostId());
                    postListView.refresh();
                    AdminModerationDialog.show(
                            report,
                            "Post",
                            identifier,
                            () -> applyRecommendationToPost(post, "APPROVED"),
                            () -> applyRecommendationToPost(post, "REJECTED"));
                }));
    }

    @FXML
    private void onAiReclassify() {
        ForumPost post = postListView.getSelectionModel().getSelectedItem();
        if (post == null) {
            refreshAiAnalyzeButtonState();
            return;
        }

        aiReclassifyInProgress = true;
        btnAiReclassify.setText(AI_ANALYZING_TEXT);
        refreshAiAnalyzeButtonState();

        String postText = safe(post.getTitle()) + "\n\n" + safe(post.getContent());
        moderationEngine.analyzePostAsync(postText)
                .whenComplete((report, error) -> Platform.runLater(() -> {
                    aiReclassifyInProgress = false;
                    btnAiReclassify.setText(AI_RECLASSIFY_BUTTON_TEXT);
                    refreshAiAnalyzeButtonState();

                    if (error != null) {
                        DebugLog.error("AdminForumController",
                                "AI reclassify failed for post #" + post.getId() + ": " + shortErrorMessage(error), error);
                        showError("AI Reclassify Failed", asException(error));
                        return;
                    }
                    if (report == null) {
                        DebugLog.error("AdminForumController",
                                "AI reclassify returned no report for post #" + post.getId(), null);
                        showWarning("AI reclassify returned no result. Keeping current category.");
                        return;
                    }

                    String predictedCategory = resolvePredictedCategory(report);
                    if (report.isFallbackUsed()) {
                        if ("General".equalsIgnoreCase(predictedCategory)) {
                            DebugLog.error("AdminForumController",
                                    "AI service unavailable during reclassify for post #" + post.getId()
                                            + ". Falling back to category 'General'.",
                                    null);
                            showWarning("AI service is currently unavailable. Category was set to 'General'.");
                        } else {
                            DebugLog.error("AdminForumController",
                                    "AI service partially unavailable during reclassify for post #" + post.getId()
                                            + ". Using available category '" + predictedCategory + "'.",
                                    null);
                            showWarning("AI analysis completed with limited data. Category set to '" + predictedCategory
                                    + "'.");
                        }
                    }

                    try {
                        postRepo.updateTag(post.getId(), predictedCategory);
                        post.setTag(predictedCategory);
                        postListView.refresh();
                        renderSelectedPost();
                        showInfo("Post #" + post.getId() + " reclassified as '" + predictedCategory + "'.");
                    } catch (Exception ex) {
                        DebugLog.error("AdminForumController",
                                "Failed saving AI category for post #" + post.getId(), ex);
                        showError("Failed to update post tag", ex);
                    }
                }));
    }

    @FXML
    private void onAiAnalyzeComment() {
        ForumComment comment = commentListView2.getSelectionModel().getSelectedItem();
        if (comment == null) {
            refreshAiAnalyzeButtonState();
            return;
        }

        aiCommentAnalysisInProgress = true;
        btnAiAnalyzeComment.setText(AI_ANALYZING_TEXT);
        refreshAiAnalyzeButtonState();

        String commentText = safe(comment.getContent());
        moderationEngine.analyzeCommentAsync(commentText)
                .whenComplete((report, error) -> Platform.runLater(() -> {
                    aiCommentAnalysisInProgress = false;
                    btnAiAnalyzeComment.setText(AI_COMMENT_BUTTON_TEXT);
                    refreshAiAnalyzeButtonState();

                    if (error != null) {
                        showAiAnalysisError("comment", error);
                        return;
                    }
                    if (report == null) {
                        showAiAnalysisError("comment", new IllegalStateException("No moderation report returned"));
                        return;
                    }

                    String identifier = "Comment #" + comment.getId() + " on Post #" + comment.getPostId();
                    if (report.isFallbackUsed()) {
                        showAiFallbackWarning("comment", report);
                    }
                    AdminModerationDialog.show(
                            report,
                            "Comment",
                            identifier,
                            () -> applyRecommendationToComment(comment, "APPROVED"),
                            () -> applyRecommendationToComment(comment, "REJECTED"));
                }));
    }

    @FXML
    private void onAiAuditLast50() {
        if (aiAuditInProgress) {
            return;
        }
        openAiAuditDialog(AI_AUDIT_LIMIT);
    }

    private void openAiAuditDialog(int limit) {
        ObservableList<AuditRow> rows = FXCollections.observableArrayList();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("AI Audit");
        dialog.getDialogPane().setPrefWidth(920);
        dialog.getDialogPane().setPrefHeight(640);

        ButtonType applyType = new ButtonType("Apply Decisions", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(applyType, closeType);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        Label progressLabel = new Label("Preparing audit...");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("secondary");

        TableView<AuditRow> table = createAuditTable(rows);
        VBox root = new VBox(10);
        root.setPadding(new javafx.geometry.Insets(12));
        root.getChildren().addAll(
                new Label("Analyze the latest " + limit + " posts and latest " + limit + " comments."),
                new HBox(10, progressBar, cancelBtn),
                progressLabel,
                table);
        dialog.getDialogPane().setContent(root);

        Node applyNode = dialog.getDialogPane().lookupButton(applyType);
        if (applyNode instanceof Button applyButton) {
            applyButton.setDisable(true);
        }

        Task<Void> auditTask = createAiAuditTask(limit, rows);
        progressBar.progressProperty().bind(auditTask.progressProperty());
        progressLabel.textProperty().bind(auditTask.messageProperty());

        cancelBtn.setOnAction(evt -> {
            if (auditTask.isRunning()) {
                auditTask.cancel();
            }
        });

        aiAuditInProgress = true;
        if (btnAiAuditLast50 != null) {
            btnAiAuditLast50.setDisable(true);
            btnAiAuditLast50.setText(AI_ANALYZING_TEXT);
        }
        refreshAiAnalyzeButtonState();

        auditTask.setOnSucceeded(evt -> {
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            cancelBtn.setDisable(true);

            int actionable = countActionableAuditRows(rows);
            String doneMessage = "Audit completed. " + rows.size() + " items analyzed";
            if (actionable > 0) {
                doneMessage += " (" + actionable + " actionable).";
            } else {
                doneMessage += ". No REJECT/PENDING decisions to apply.";
            }
            progressLabel.setText(doneMessage);

            if (applyNode instanceof Button applyButton) {
                applyButton.setDisable(actionable == 0);
            }
            setAiAuditIdle();
        });

        auditTask.setOnCancelled(evt -> {
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            cancelBtn.setDisable(true);
            progressLabel.setText("Audit cancelled. " + rows.size() + " items analyzed so far.");
            if (applyNode instanceof Button applyButton) {
                applyButton.setDisable(countActionableAuditRows(rows) == 0);
            }
            DebugLog.info("AdminForumController", "AI audit cancelled by admin.");
            setAiAuditIdle();
        });

        auditTask.setOnFailed(evt -> {
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            cancelBtn.setDisable(true);

            Throwable error = auditTask.getException();
            String message = shortErrorMessage(error);
            progressLabel.setText("Audit stopped: " + message);
            if (applyNode instanceof Button applyButton) {
                applyButton.setDisable(true);
            }
            DebugLog.error("AdminForumController", "AI audit failed: " + message, error);
            showWarning("AI audit stopped: " + message);
            setAiAuditIdle();
        });

        dialog.setOnHidden(evt -> {
            if (auditTask.isRunning()) {
                auditTask.cancel();
            }
            setAiAuditIdle();
        });

        Thread worker = new Thread(auditTask, "admin-ai-audit");
        worker.setDaemon(true);
        worker.start();

        dialog.showAndWait().ifPresent(selected -> {
            if (selected == applyType) {
                applyAuditDecisions(rows);
            }
        });
    }

    private Task<Void> createAiAuditTask(int limit, ObservableList<AuditRow> outRows) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Loading latest posts/comments...");
                List<ForumPost> posts = postRepo.findLatest(limit);
                List<ForumComment> latestComments = commentRepo.findLatest(limit);

                int total = posts.size() + latestComments.size();
                if (total == 0) {
                    updateProgress(1, 1);
                    updateMessage("No posts/comments found for audit.");
                    return null;
                }
                updateProgress(0, total);

                int done = 0;
                for (ForumPost post : posts) {
                    if (isCancelled()) {
                        updateMessage("Cancelling...");
                        return null;
                    }
                    ModerationReport report = moderationEngine
                            .analyzePostAsync(buildAuditPostText(post))
                            .join();
                    ensureAuditAvailable(report);
                    AuditRow row = AuditRow.fromPost(post, report);
                    Platform.runLater(() -> outRows.add(row));
                    done++;
                    updateProgress(done, total);
                    updateMessage("Analyzed " + done + " / " + total);
                }

                for (ForumComment comment : latestComments) {
                    if (isCancelled()) {
                        updateMessage("Cancelling...");
                        return null;
                    }
                    ModerationReport report = moderationEngine
                            .analyzeCommentAsync(safe(comment.getContent()))
                            .join();
                    ensureAuditAvailable(report);
                    AuditRow row = AuditRow.fromComment(comment, report);
                    Platform.runLater(() -> outRows.add(row));
                    done++;
                    updateProgress(done, total);
                    updateMessage("Analyzed " + done + " / " + total);
                }
                updateMessage("Audit complete.");
                return null;
            }
        };
    }

    private void ensureAuditAvailable(ModerationReport report) {
        if (report == null) {
            throw new IllegalStateException("AI returned no report. Audit stopped.");
        }
        if (report.isFallbackUsed()) {
            throw new IllegalStateException("AI service unavailable (fallback detected). Audit stopped.");
        }
    }

    private void setAiAuditIdle() {
        aiAuditInProgress = false;
        if (btnAiAuditLast50 != null) {
            btnAiAuditLast50.setDisable(false);
            btnAiAuditLast50.setText(AI_AUDIT_BUTTON_TEXT);
        }
        refreshAiAnalyzeButtonState();
    }

    private TableView<AuditRow> createAuditTable(ObservableList<AuditRow> rows) {
        TableView<AuditRow> table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);

        TableColumn<AuditRow, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<AuditRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<AuditRow, Double> toxCol = new TableColumn<>("Toxicity");
        toxCol.setCellValueFactory(new PropertyValueFactory<>("toxicity"));
        toxCol.setCellFactory(col -> decimalCell());

        TableColumn<AuditRow, Double> qualityCol = new TableColumn<>("Quality");
        qualityCol.setCellValueFactory(new PropertyValueFactory<>("quality"));
        qualityCol.setCellFactory(col -> decimalCell());

        TableColumn<AuditRow, Double> duplicateCol = new TableColumn<>("Duplicate");
        duplicateCol.setCellValueFactory(new PropertyValueFactory<>("duplicate"));
        duplicateCol.setCellFactory(col -> decimalCell());

        TableColumn<AuditRow, String> decisionCol = new TableColumn<>("Decision");
        decisionCol.setCellValueFactory(new PropertyValueFactory<>("decision"));

        table.getColumns().addAll(idCol, typeCol, toxCol, qualityCol, duplicateCol, decisionCol);
        return table;
    }

    private TableCell<AuditRow, Double> decimalCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText("");
                    return;
                }
                setText(String.format("%.3f", value));
            }
        };
    }

    private String buildAuditPostText(ForumPost post) {
        return safe(post == null ? "" : post.getTitle()) + "\n\n" + safe(post == null ? "" : post.getContent());
    }

    private int countActionableAuditRows(List<AuditRow> rows) {
        int count = 0;
        for (AuditRow row : rows) {
            if (toStatusFromDecision(row.getDecision()) != null) {
                count++;
            }
        }
        return count;
    }

    private void applyAuditDecisions(List<AuditRow> rows) {
        if (rows == null || rows.isEmpty()) {
            showInfo("No audit results to apply.");
            return;
        }

        int updated = 0;
        int rejected = 0;
        int pending = 0;

        try {
            for (AuditRow row : rows) {
                String targetStatus = toStatusFromDecision(row.getDecision());
                if (targetStatus == null) {
                    continue;
                }
                if ("POST".equals(row.getType())) {
                    postRepo.updateStatus(row.getId(), targetStatus);
                } else {
                    commentRepo.updateStatus(row.getId(), targetStatus);
                }
                updated++;
                if ("REJECTED".equals(targetStatus)) {
                    rejected++;
                } else if ("PENDING".equals(targetStatus)) {
                    pending++;
                }
            }
        } catch (Exception ex) {
            DebugLog.error("AdminForumController", "Failed applying AI audit decisions", ex);
            showError("Failed to apply AI audit decisions", ex);
            return;
        }

        showInfo("Applied decisions to " + updated + " items (Rejected: " + rejected + ", Pending: " + pending + ").");
        onRefreshPosts();
        loadComments();
    }

    private String toStatusFromDecision(String decision) {
        if (decision == null || decision.isBlank()) {
            return null;
        }
        String normalized = decision.trim().toUpperCase(Locale.ROOT);
        if ("REJECT".equals(normalized) || "REJECTED".equals(normalized)) {
            return "REJECTED";
        }
        if ("PENDING".equals(normalized)) {
            return "PENDING";
        }
        return null;
    }

    private void refreshAiAnalyzeButtonState() {
        boolean hasPostSelection = postListView != null
                && postListView.getSelectionModel().getSelectedItem() != null;
        boolean hasCommentSelection = commentListView2 != null
                && commentListView2.getSelectionModel().getSelectedItem() != null;

        if (btnAiAnalyzePost != null) {
            btnAiAnalyzePost.setDisable(!hasPostSelection || aiPostAnalysisInProgress || aiReclassifyInProgress);
        }
        if (btnAiReclassify != null) {
            btnAiReclassify.setDisable(!hasPostSelection || aiReclassifyInProgress || aiPostAnalysisInProgress);
        }
        if (btnAiAnalyzeComment != null) {
            btnAiAnalyzeComment.setDisable(!hasCommentSelection || aiCommentAnalysisInProgress);
        }
        if (btnAiAuditLast50 != null) {
            btnAiAuditLast50.setDisable(aiAuditInProgress);
        }
    }

    private String resolvePredictedCategory(ModerationReport report) {
        if (report == null) {
            return "General";
        }
        String category = report.getPredictedCategory();
        if (category == null || category.isBlank()) {
            return "General";
        }
        return category.trim();
    }

    private void applyRecommendationToPost(ForumPost post, String recommendedStatus) {
        if (post == null || safe(recommendedStatus).isBlank()) {
            return;
        }
        try {
            post.setStatus(recommendedStatus);
            postRepo.update(post);
            DebugLog.info("AdminForumController",
                    "Applied AI recommendation to post #" + post.getId() + ": " + recommendedStatus);
            showInfo("Post #" + post.getId() + " marked as " + recommendedStatus);
            onRefreshPosts();
            selectPostById(post.getId());
        } catch (Exception ex) {
            DebugLog.error("AdminForumController",
                    "Failed applying AI recommendation to post #" + post.getId(), ex);
            showError("Failed to apply AI recommendation to post", ex);
        }
    }

    private void applyRecommendationToComment(ForumComment comment, String recommendedStatus) {
        if (comment == null || safe(recommendedStatus).isBlank()) {
            return;
        }
        try {
            comment.setStatus(recommendedStatus);
            commentRepo.update(comment);
            DebugLog.info("AdminForumController",
                    "Applied AI recommendation to comment #" + comment.getId() + ": " + recommendedStatus);
            showInfo("Comment #" + comment.getId() + " marked as " + recommendedStatus);
            loadComments();
            selectCommentById(comment.getId());
        } catch (Exception ex) {
            DebugLog.error("AdminForumController",
                    "Failed applying AI recommendation to comment #" + comment.getId(), ex);
            showError("Failed to apply AI recommendation to comment", ex);
        }
    }

    private void selectCommentById(long commentId) {
        for (ForumComment forumComment : commentListView2.getItems()) {
            if (forumComment.getId() == commentId) {
                commentListView2.getSelectionModel().select(forumComment);
                commentListView2.scrollTo(forumComment);
                return;
            }
        }
    }

    private void showAiFallbackWarning(String contentType, ModerationReport report) {
        DebugLog.error("AdminForumController",
                "AI service degraded while analyzing " + contentType + ". Fallback report generated. Reasons: "
                        + (report == null ? "n/a" : report.getReasons()),
                null);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("AI Service Limited");
        alert.setHeaderText("Analysis completed with limited AI data");
        alert.setContentText(
                "One or more AI services are currently unavailable. The item was marked for manual review and fallback scores are shown.");
        alert.showAndWait();
    }

    private void showAiAnalysisError(String contentType, Throwable error) {
        DebugLog.error("AdminForumController",
                "AI analysis failed for " + contentType + ": " + shortErrorMessage(error), error);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("AI Analysis Failed");
        alert.setHeaderText("Unable to analyze " + contentType);
        alert.setContentText(shortErrorMessage(error));
        alert.showAndWait();
    }

    private String shortErrorMessage(Throwable error) {
        Throwable root = error;
        while (root instanceof CompletionException && root.getCause() != null) {
            root = root.getCause();
        }
        if (root == null) {
            return "Unexpected error.";
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = root.getClass().getSimpleName();
        }
        message = message.replace('\n', ' ').replace('\r', ' ').trim();
        if (message.length() > 140) {
            message = message.substring(0, 140) + "...";
        }
        return message;
    }

    private Exception asException(Throwable ex) {
        return ex instanceof Exception e ? e : new Exception(ex == null ? "Unknown error" : ex.getMessage(), ex);
    }

    @FXML
    private void onAddComment2() {
        // CREATE (comments): admin can write comment with explicit moderation status.
        if (selectedPost == null || selectedPost.isLocked()) {
            showWarning("This post is locked. You cannot modify comments.");
            return;
        }
        List<String> errors = InputValidator.validateComment(commentArea2.getText());
        if (!errors.isEmpty()) {
            showWarning(errors.get(0));
            return;
        }

        ForumComment c = new ForumComment();
        c.setPostId(selectedPost.getId());
        c.setAuthorId(Session.getCurrentUserId());
        c.setContent(InputValidator.norm(commentArea2.getText()));
        c.setStatus(commentStatusBox2.getValue());

        try {
            commentRepo.insert(c);
            showInfo("Comment added");
            loadComments();
            refreshAdminCommentActionVisibility();
        } catch (Exception ex) {
            showError("Failed to add comment", ex);
        }
    }

    @FXML
    private void onUpdateComment2() {
        // UPDATE (comments): admin edit flow on selected comment.
        if (selectedPost == null || selectedPost.isLocked()) {
            showWarning("This post is locked. You cannot modify comments.");
            return;
        }

        ForumComment selected = commentListView2.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        List<String> errors = InputValidator.validateComment(commentArea2.getText());
        if (!errors.isEmpty()) {
            showWarning(errors.get(0));
            return;
        }

        selected.setContent(InputValidator.norm(commentArea2.getText()));
        selected.setStatus(commentStatusBox2.getValue());
        try {
            commentRepo.update(selected);
            showInfo("Comment updated");
            loadComments();
            refreshAdminCommentActionVisibility();
        } catch (Exception ex) {
            showError("Failed to update comment", ex);
        }
    }

    @FXML
    private void onDeleteComment2() {
        // DELETE (comments): admin delete flow on selected comment.
        ForumComment selected = commentListView2.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        try {
            commentRepo.delete(selected.getId());
            showInfo("Comment deleted");
            loadComments();
            refreshAdminCommentActionVisibility();
        } catch (Exception ex) {
            showError("Failed to delete comment", ex);
        }
    }

    // =========================
    private void refreshSessionUI() {
        // Admin panel always shows current identity; dev mode controls switcher visibility.
        long uid = Session.getCurrentUserId();
        String name = displayName(uid);
        currentUserLabel.setText("Logged in as " + (name == null || name.isBlank() ? ("User #" + uid) : name));

        boolean dev = Session.DEV_MODE;
        devUserBox.setVisible(dev);
        devUserBox.setManaged(dev);
    }

    @FXML
    private void onOpenUserForum() {
        try {
            // Navigation back to user-facing forum screen.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/UserForumView.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(getClass().getResource(Session.getThemeStylesheetPath()).toExternalForm());

            Stage st = new Stage();
            st.initStyle(javafx.stage.StageStyle.UNDECORATED);
            st.setTitle("Hirely - User Forum");
            st.initModality(Modality.NONE);
            st.setScene(scene);
            st.sizeToScene();
            st.centerOnScreen();
            st.show();
        } catch (Exception ex) {
            showError("Failed to open User Forum", ex);
        }
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

    // Modern Card Cells
    // =========================

    @FXML
    private void onMinimize() {
        // Custom app bar window controls (undecorated stage).
        if (logoView.getScene().getWindow() instanceof javafx.stage.Stage s)
            s.setIconified(true);
    }

    @FXML
    private void onMaximize() {
        if (logoView.getScene().getWindow() instanceof javafx.stage.Stage s) {
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
        // Admin window is treated as app-root; closing exits process.
        moderationEngine.shutdown();
        System.exit(0);
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
    }

    private void annotateDuplicateScores(java.util.List<ForumPost> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        java.util.List<Set<String>> tokenSets = new java.util.ArrayList<>(posts.size());
        for (ForumPost post : posts) {
            tokenSets.add(tokenizeForDuplicate(post));
        }

        for (int i = 0; i < posts.size(); i++) {
            ForumPost current = posts.get(i);
            if (current.getDuplicateScore() > 0.0) {
                continue;
            }
            Set<String> currentTokens = tokenSets.get(i);
            double bestScore = 0.0;
            Long bestPostId = null;
            for (int j = 0; j < posts.size(); j++) {
                if (i == j) {
                    continue;
                }
                double score = tokenOverlap(currentTokens, tokenSets.get(j));
                if (score > bestScore) {
                    bestScore = score;
                    bestPostId = posts.get(j).getId();
                }
            }
            current.setDuplicateScore(bestScore);
            current.setDuplicateOfPostId(bestPostId);
        }
    }

    private Set<String> tokenizeForDuplicate(ForumPost post) {
        String text = safe(post == null ? "" : post.getTitle()) + " " + safe(post == null ? "" : post.getContent());
        Set<String> tokens = new HashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(java.util.Locale.ROOT));
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private double tokenOverlap(Set<String> left, Set<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String token : left) {
            if (right.contains(token)) {
                intersection++;
            }
        }
        int union = left.size() + right.size() - intersection;
        if (union <= 0) {
            return 0.0;
        }
        return (double) intersection / union;
    }

    private static final class AuditRow {
        private final long id;
        private final String type;
        private final double toxicity;
        private final double quality;
        private final double duplicate;
        private final String decision;

        private AuditRow(long id, String type, double toxicity, double quality, double duplicate, String decision) {
            this.id = id;
            this.type = type;
            this.toxicity = toxicity;
            this.quality = quality;
            this.duplicate = duplicate;
            this.decision = decision;
        }

        private static AuditRow fromPost(ForumPost post, ModerationReport report) {
            return new AuditRow(
                    post.getId(),
                    "POST",
                    report.getToxicity(),
                    report.getQualityScore(),
                    report.getDuplicateScore(),
                    safeDecision(report.getDecision()));
        }

        private static AuditRow fromComment(ForumComment comment, ModerationReport report) {
            return new AuditRow(
                    comment.getId(),
                    "COMMENT",
                    report.getToxicity(),
                    report.getQualityScore(),
                    report.getDuplicateScore(),
                    safeDecision(report.getDecision()));
        }

        private static String safeDecision(String decision) {
            return decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        }

        public long getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public double getToxicity() {
            return toxicity;
        }

        public double getQuality() {
            return quality;
        }

        public double getDuplicate() {
            return duplicate;
        }

        public String getDecision() {
            return decision;
        }
    }

    private class CommentCardCell extends ListCell<ForumComment> {
        private final VBox card = new VBox(6);
        private final Label content = new Label();
        private final Label meta = new Label();

        CommentCardCell() {
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
            content.setText(safe(c.getContent()));
            String author = displayName(c.getAuthorId());
            String when = formatTime(c.getCreatedAt());
            meta.setText(author + (when.isBlank() ? "" : " - " + when) + " - " + safe(c.getStatus()));
            setGraphic(card);
        }
    }
}
