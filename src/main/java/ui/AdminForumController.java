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
import util.GeminiClient;
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
import java.util.concurrent.CompletableFuture;
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
    private ComboBox<String> statusFilterBox;
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
    @FXML
    private TableView<FeedbackRow> feedbackTable;
    @FXML
    private TableColumn<FeedbackRow, String> feedbackTimeCol;
    @FXML
    private TableColumn<FeedbackRow, String> feedbackTypeCol;
    @FXML
    private TableColumn<FeedbackRow, String> feedbackTargetCol;
    @FXML
    private TableColumn<FeedbackRow, String> feedbackDecisionCol;
    @FXML
    private TableColumn<FeedbackRow, String> feedbackCategoryCol;
    @FXML
    private TableColumn<FeedbackRow, String> feedbackToxicityCol;
    @FXML
    private TableColumn<FeedbackRow, String> feedbackQualityCol;
    @FXML
    private TableColumn<FeedbackRow, String> feedbackDuplicateCol;
    @FXML
    private TableColumn<FeedbackRow, String> feedbackFallbackCol;
    @FXML
    private TableColumn<FeedbackRow, String> feedbackSourceCol;
    @FXML
    private Label feedbackTotalLabel;
    @FXML
    private Label feedbackPostLabel;
    @FXML
    private Label feedbackCommentLabel;
    @FXML
    private Label feedbackFallbackLabel;
    @FXML
    private Label feedbackLastUpdatedLabel;

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
    private ComboBox<String> commentStatusFilterBox2;
    @FXML
    private ComboBox<String> commentSortBox2;
    @FXML
    private Button addCommentBtn2;
    @FXML
    private Button updateCommentBtn2;
    @FXML
    private Button deleteCommentBtn2;
    @FXML
    private Button seeCommentPostBtn2;
    @FXML
    private Button btnAiAnalyzeComment;

    private final ForumPostRepository postRepo = new ForumPostRepository();
    private final ForumCommentRepository commentRepo = new ForumCommentRepository();
    private final UserRepository userRepo = new UserRepository();
    private final ModerationEngine moderationEngine = new ModerationEngine();
    private final GeminiClient geminiClient = new GeminiClient();
    private final long geminiUserId = userRepo.findOrCreateSystemUserId("gemini@hirely.local", "Gemini", "Assistant");

    private final Map<Long, String> userNameCache = new HashMap<>();

    private final ObservableList<ForumPost> allPosts = FXCollections.observableArrayList();
    private final ObservableList<ForumComment> comments = FXCollections.observableArrayList();
    private final ObservableList<FeedbackRow> feedbackRows = FXCollections.observableArrayList();
    private final java.util.List<ForumComment> commentBuffer = new java.util.ArrayList<>();

    private FilteredList<ForumPost> filteredPosts;
    private SortedList<ForumPost> sortedPosts;

    private ForumPost selectedPost;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");
    private static final DateTimeFormatter FEEDBACK_TS = DateTimeFormatter.ofPattern("dd MMM HH:mm:ss");
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
        statusFilterBox.setItems(FXCollections.observableArrayList("All", "PENDING", "REJECTED", "APPROVED"));
        statusFilterBox.getSelectionModel().select("All");

        // Keep top-bar controls readable when window width is tight.
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setMaxWidth(Double.MAX_VALUE);
        statusFilterBox.setMinWidth(Region.USE_PREF_SIZE);
        newPostBtn.setMinWidth(Region.USE_PREF_SIZE);
        btnAiAuditLast50.setMinWidth(Region.USE_PREF_SIZE);
        userForumBtn.setMinWidth(Region.USE_PREF_SIZE);

        // Comment status
        commentStatusBox2.setItems(FXCollections.observableArrayList("PENDING", "APPROVED", "REJECTED"));
        commentStatusBox2.getSelectionModel().select("PENDING");
        if (commentStatusFilterBox2 != null) {
            commentStatusFilterBox2.setItems(FXCollections.observableArrayList("All", "PENDING", "APPROVED", "REJECTED"));
            commentStatusFilterBox2.getSelectionModel().select("All");
            commentStatusFilterBox2.valueProperty().addListener((obs, oldV, v) -> applyCommentFiltersAndSort());
        }
        if (commentSortBox2 != null) {
            commentSortBox2.setItems(FXCollections.observableArrayList("Newest", "Oldest", "Status"));
            commentSortBox2.getSelectionModel().select("Newest");
            commentSortBox2.valueProperty().addListener((obs, oldV, v) -> applyCommentFiltersAndSort());
        }

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
        initFeedbackTable();

        // Filter + sort pipeline for large post feeds.
        filteredPosts = new FilteredList<>(allPosts, p -> true);
        sortedPosts = new SortedList<>(filteredPosts);
        sortedPosts.setComparator(postComparator(sortBox.getValue()));
        postListView.setItems(sortedPosts);

        sortBox.valueProperty().addListener((obs, oldV, v) -> sortedPosts.setComparator(postComparator(v)));
        statusFilterBox.valueProperty().addListener((obs, oldV, v) -> applyPostFilters());

        // Live search over title/tag/content.
        searchField.textProperty().addListener((obs, oldV, v) -> applyPostFilters());

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
        editPostBtn.setMinWidth(Region.USE_PREF_SIZE);
        deletePostBtn.setMinWidth(Region.USE_PREF_SIZE);
        btnAiReclassify.setMinWidth(Region.USE_PREF_SIZE);
        btnAiAnalyzePost.setMinWidth(Region.USE_PREF_SIZE);

        BooleanBinding noPost = postListView.getSelectionModel().selectedItemProperty().isNull();
        // Admins can always moderate comments, even if a post is locked.
        BooleanBinding cannotComment = noPost;

        commentArea2.disableProperty().bind(cannotComment);
        addCommentBtn2.disableProperty().bind(cannotComment);

        updateCommentBtn2.disableProperty().bind(commentListView2.getSelectionModel().selectedItemProperty().isNull());
        deleteCommentBtn2.disableProperty().bind(commentListView2.getSelectionModel().selectedItemProperty().isNull());
        if (seeCommentPostBtn2 != null) {
            seeCommentPostBtn2.disableProperty().bind(commentListView2.getSelectionModel().selectedItemProperty().isNull());
        }

        refreshAiAnalyzeButtonState();

        // First load
        onRefreshPosts();
    }

    private void applyPostFilters() {
        if (filteredPosts == null) {
            return;
        }
        String q = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String statusFilter = statusFilterBox == null ? "All" : safe(statusFilterBox.getValue()).trim().toUpperCase(Locale.ROOT);
        filteredPosts.setPredicate(p -> {
            if (p == null) {
                return false;
            }
            boolean matchesText = q.isEmpty()
                    || safe(p.getTitle()).toLowerCase().contains(q)
                    || safe(p.getTag()).toLowerCase().contains(q)
                    || safe(p.getContent()).toLowerCase().contains(q);
            if (!matchesText) {
                return false;
            }
            if ("ALL".equals(statusFilter) || statusFilter.isBlank()) {
                return true;
            }
            return safe(p.getStatus()).trim().equalsIgnoreCase(statusFilter);
        });
    }

    private void initFeedbackTable() {
        if (feedbackTable == null) {
            return;
        }
        feedbackTable.setStyle("-fx-font-size: 13px;");
        feedbackTimeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        feedbackTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        feedbackTargetCol.setCellValueFactory(new PropertyValueFactory<>("target"));
        feedbackDecisionCol.setCellValueFactory(new PropertyValueFactory<>("decision"));
        feedbackCategoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        feedbackToxicityCol.setCellValueFactory(new PropertyValueFactory<>("toxicity"));
        feedbackQualityCol.setCellValueFactory(new PropertyValueFactory<>("quality"));
        feedbackDuplicateCol.setCellValueFactory(new PropertyValueFactory<>("duplicate"));
        feedbackFallbackCol.setCellValueFactory(new PropertyValueFactory<>("fallback"));
        feedbackSourceCol.setCellValueFactory(new PropertyValueFactory<>("source"));
        feedbackTable.setItems(feedbackRows);
        refreshFeedbackSummary();
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
            applyPostFilters();
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
            p.setTag(InputValidator.normalizeSingleTag(tag.getText()));
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
        ForumComment sel = commentListView2.getSelectionModel().getSelectedItem();
        boolean hasSel = sel != null;

        // Admin can add comments whenever a post is selected.
        boolean showAdd = hasPost;
        addCommentBtn2.setVisible(showAdd);
        addCommentBtn2.setManaged(showAdd);

        // Update/Delete: only when a comment is selected.
        boolean showModify = hasPost && hasSel;
        updateCommentBtn2.setVisible(showModify);
        updateCommentBtn2.setManaged(showModify);

        deleteCommentBtn2.setVisible(showModify);
        deleteCommentBtn2.setManaged(showModify);
        if (seeCommentPostBtn2 != null) {
            seeCommentPostBtn2.setVisible(hasSel);
            seeCommentPostBtn2.setManaged(hasSel);
        }
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
            commentBuffer.clear();
            if (selectedPost == null) {
                commentListView2.setItems(comments);
                refreshAiAnalyzeButtonState();
                return;
            }
            commentBuffer.addAll(commentRepo.findByPostId(selectedPost.getId()));
            applyCommentFiltersAndSort();
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

    private void applyCommentFiltersAndSort() {
        java.util.List<ForumComment> filtered = new java.util.ArrayList<>(commentBuffer);
        String statusFilter = commentStatusFilterBox2 == null ? "All"
                : safe(commentStatusFilterBox2.getValue()).trim().toUpperCase(Locale.ROOT);
        if (!statusFilter.isBlank() && !"ALL".equals(statusFilter)) {
            filtered.removeIf(c -> !statusFilter.equals(safe(c.getStatus()).trim().toUpperCase(Locale.ROOT)));
        }

        String sortMode = commentSortBox2 == null ? "Newest" : safe(commentSortBox2.getValue());
        Comparator<ForumComment> byCreatedAsc = Comparator.comparing(
                ForumComment::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<ForumComment> comparator;
        if ("Oldest".equalsIgnoreCase(sortMode)) {
            comparator = byCreatedAsc;
        } else if ("Status".equalsIgnoreCase(sortMode)) {
            comparator = Comparator
                    .comparingInt((ForumComment c) -> commentStatusRank(c.getStatus()))
                    .thenComparing(byCreatedAsc.reversed());
        } else {
            comparator = byCreatedAsc.reversed();
        }
        filtered.sort(comparator);
        comments.setAll(filtered);
    }

    private int commentStatusRank(String status) {
        String value = safe(status).trim().toUpperCase(Locale.ROOT);
        if ("PENDING".equals(value)) {
            return 0;
        }
        if ("REJECTED".equals(value)) {
            return 1;
        }
        if ("APPROVED".equals(value)) {
            return 2;
        }
        return 3;
    }

    @FXML
    private void onSeeCommentPost2() {
        ForumComment selected = commentListView2.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        long postId = selected.getPostId();
        if (searchField != null) {
            searchField.clear();
        }
        if (statusFilterBox != null) {
            statusFilterBox.setValue("All");
        }
        applyPostFilters();
        selectPostById(postId);
        if (postListView.getSelectionModel().getSelectedItem() == null
                || postListView.getSelectionModel().getSelectedItem().getId() != postId) {
            try {
                ForumPost post = postRepo.findById(postId);
                if (post != null) {
                    boolean exists = allPosts.stream().anyMatch(p -> p.getId() == postId);
                    if (!exists) {
                        allPosts.add(post);
                    }
                    applyPostFilters();
                    selectPostById(postId);
                }
            } catch (Exception ex) {
                showError("Failed to open related post", ex);
                return;
            }
        }
        mainTabs.getSelectionModel().select(0);
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
        moderationEngine.analyzePostAsync(postText, "post:" + post.getId())
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
                    recordFeedback("POST", "Post #" + post.getId(), report, "Analyze");
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
        moderationEngine.analyzePostAsync(postText, "post:" + post.getId())
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
                        recordFeedback("POST", "Post #" + post.getId(), report, "Reclassify");
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
        moderationEngine.analyzeCommentAsync(commentText, "comment:" + comment.getId())
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
                    recordFeedback("COMMENT", "Comment #" + comment.getId(), report, "Analyze");
                }));
    }

    @FXML
    private void onAiAuditLast50() {
        if (aiAuditInProgress) {
            return;
        }
        feedbackRows.clear();
        refreshFeedbackSummary();
        aiAuditInProgress = true;
        if (btnAiAuditLast50 != null) {
            btnAiAuditLast50.setDisable(true);
            btnAiAuditLast50.setText(AI_ANALYZING_TEXT);
        }
        refreshAiAnalyzeButtonState();

        Task<Void> auditTask = createAiAuditTask(AI_AUDIT_LIMIT);
        auditTask.setOnSucceeded(evt -> {
            setAiAuditIdle();
            showInfo("AI audit completed. Results are available in the Feedback tab.");
        });
        auditTask.setOnCancelled(evt -> {
            setAiAuditIdle();
            showWarning("AI audit cancelled.");
            DebugLog.info("AdminForumController", "AI audit cancelled by admin.");
        });
        auditTask.setOnFailed(evt -> {
            setAiAuditIdle();
            Throwable error = auditTask.getException();
            String message = shortErrorMessage(error);
            DebugLog.error("AdminForumController", "AI audit failed: " + message, error);
            showWarning("AI audit stopped: " + message);
        });

        Thread worker = new Thread(auditTask, "admin-ai-audit");
        worker.setDaemon(true);
        worker.start();
    }

    private Task<Void> createAiAuditTask(int limit) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Loading latest pending/rejected posts and latest comments...");
                List<ForumPost> posts = postRepo.findLatest(limit);
                posts.removeIf(post -> {
                    String status = safe(post == null ? "" : post.getStatus()).trim().toUpperCase(Locale.ROOT);
                    return "APPROVED".equals(status);
                });
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
                            .analyzePostAsync(buildAuditPostText(post), "post:" + post.getId())
                            .join();
                    ensureAuditAvailable(report);
                    Platform.runLater(() -> recordFeedback("POST", "Post #" + post.getId(), report, "Audit"));
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
                            .analyzeCommentAsync(safe(comment.getContent()), "comment:" + comment.getId())
                            .join();
                    ensureAuditAvailable(report);
                    Platform.runLater(
                            () -> recordFeedback("COMMENT", "Comment #" + comment.getId(), report, "Audit"));
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

    private String buildAuditPostText(ForumPost post) {
        return safe(post == null ? "" : post.getTitle()) + "\n\n" + safe(post == null ? "" : post.getContent());
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

    private void recordFeedback(String type, String target, ModerationReport report, String source) {
        if (report == null) {
            return;
        }
        FeedbackRow row = new FeedbackRow(
                FEEDBACK_TS.format(LocalDateTime.now()),
                safe(type),
                safe(target),
                safe(report.getDecision()),
                safe(resolvePredictedCategory(report)),
                String.format(Locale.ROOT, "%.3f", report.getToxicity()),
                String.format(Locale.ROOT, "%.3f", report.getQualityScore()),
                String.format(Locale.ROOT, "%.3f", report.getDuplicateScore()),
                report.isFallbackUsed() ? "Yes" : "No",
                safe(source));

        feedbackRows.add(0, row);
        if (feedbackRows.size() > 250) {
            feedbackRows.remove(feedbackRows.size() - 1);
        }
        refreshFeedbackSummary();
    }

    private void refreshFeedbackSummary() {
        if (feedbackTotalLabel == null) {
            return;
        }
        int total = feedbackRows.size();
        int posts = 0;
        int commentsCount = 0;
        int fallback = 0;
        for (FeedbackRow row : feedbackRows) {
            if ("POST".equals(row.getType())) {
                posts++;
            } else if ("COMMENT".equals(row.getType())) {
                commentsCount++;
            }
            if ("Yes".equals(row.getFallback())) {
                fallback++;
            }
        }
        feedbackTotalLabel.setText(Integer.toString(total));
        feedbackPostLabel.setText(Integer.toString(posts));
        feedbackCommentLabel.setText(Integer.toString(commentsCount));
        feedbackFallbackLabel.setText(Integer.toString(fallback));
        feedbackLastUpdatedLabel.setText(total == 0 ? "No AI analysis yet" : "Updated " + feedbackRows.get(0).getTime());
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
        if (selectedPost == null) {
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
            if (containsGeminiTrigger(c.getContent())) {
                requestGeminiReplyAsync(selectedPost, c.getContent());
            }
        } catch (Exception ex) {
            showError("Failed to add comment", ex);
        }
    }

    private boolean containsGeminiTrigger(String commentText) {
        return commentText != null && commentText.toLowerCase(Locale.ROOT).contains("@gemini");
    }

    private void requestGeminiReplyAsync(ForumPost targetPost, String userCommentText) {
        if (targetPost == null) {
            return;
        }
        if (geminiUserId <= 0) {
            DebugLog.error("AdminForumController",
                    "Gemini bot user not found (expected email gemini@hirely.local)", null);
            showWarning("Gemini bot user is missing. Create user with email gemini@hirely.local.");
            return;
        }
        long postId = targetPost.getId();
        String cleanedUserComment = GeminiClient.cleanTriggerToken(userCommentText);
        CompletableFuture.supplyAsync(() -> geminiClient.generateReply(
                targetPost.getTitle(),
                targetPost.getContent(),
                cleanedUserComment,
                targetPost.getTag()))
                .whenComplete((replyText, ex) -> Platform.runLater(() -> {
                    String text = replyText;
                    if (ex != null) {
                        DebugLog.error("AdminForumController", "Gemini async generation failed", ex);
                        text = "Gemini is unavailable right now (missing key or service error). Please try again later.";
                    }
                    insertGeminiComment(postId, text);
                }));
    }

    private void insertGeminiComment(long postId, String content) {
        try {
            ForumComment bot = new ForumComment();
            bot.setPostId(postId);
            bot.setAuthorId(geminiUserId);
            bot.setStatus("APPROVED");
            bot.setContent(trimToCommentLimit(content));
            commentRepo.insert(bot);
            if (selectedPost != null && selectedPost.getId() == postId) {
                loadComments();
            }
        } catch (Exception ex) {
            DebugLog.error("AdminForumController", "Failed inserting Gemini reply for post #" + postId, ex);
        }
    }

    private String trimToCommentLimit(String text) {
        String normalized = (text == null || text.isBlank())
                ? "Gemini is unavailable right now (missing key or service error). Please try again later."
                : text.trim();
        int max = InputValidator.COMMENT_MAX;
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max - 3) + "...";
    }

    @FXML
    private void onUpdateComment2() {
        // UPDATE (comments): admin edit flow on selected comment.
        if (selectedPost == null) {
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

    public static final class FeedbackRow {
        private final String time;
        private final String type;
        private final String target;
        private final String decision;
        private final String category;
        private final String toxicity;
        private final String quality;
        private final String duplicate;
        private final String fallback;
        private final String source;

        private FeedbackRow(String time, String type, String target, String decision, String category,
                String toxicity, String quality, String duplicate, String fallback, String source) {
            this.time = time;
            this.type = type;
            this.target = target;
            this.decision = decision;
            this.category = category;
            this.toxicity = toxicity;
            this.quality = quality;
            this.duplicate = duplicate;
            this.fallback = fallback;
            this.source = source;
        }

        public String getTime() { return time; }
        public String getType() { return type; }
        public String getTarget() { return target; }
        public String getDecision() { return decision; }
        public String getCategory() { return category; }
        public String getToxicity() { return toxicity; }
        public String getQuality() { return quality; }
        public String getDuplicate() { return duplicate; }
        public String getFallback() { return fallback; }
        public String getSource() { return source; }
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
