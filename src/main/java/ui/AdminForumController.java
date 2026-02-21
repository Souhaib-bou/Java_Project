package ui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import model.ForumComment;
import model.ForumPost;
import repo.ForumCommentRepository;
import repo.ForumPostRepository;
import repo.UserRepository;
import util.Session;
import util.InputValidator;
import java.util.List;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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

    // Window controls
    private double xOffset = 0;
    private double yOffset = 0;

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

    private final ForumPostRepository postRepo = new ForumPostRepository();
    private final ForumCommentRepository commentRepo = new ForumCommentRepository();
    private final UserRepository userRepo = new UserRepository();

    private final Map<Long, String> userNameCache = new HashMap<>();

    private final ObservableList<ForumPost> allPosts = FXCollections.observableArrayList();
    private final ObservableList<ForumComment> comments = FXCollections.observableArrayList();

    private FilteredList<ForumPost> filteredPosts;
    private SortedList<ForumPost> sortedPosts;

    private ForumPost selectedPost;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");

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

        // Live search over title/category/content.
        searchField.textProperty().addListener((obs, oldV, v) -> {
            String q = v == null ? "" : v.trim().toLowerCase();
            filteredPosts.setPredicate(p -> {
                if (q.isEmpty())
                    return true;
                return safe(p.getTitle()).toLowerCase().contains(q)
                        || safe(p.getCategory()).toLowerCase().contains(q)
                        || safe(p.getContent()).toLowerCase().contains(q);
            });
        });

        // Selection: Post -> updates both views
        postListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, p) -> {
            selectedPost = p;
            renderSelectedPost();
            loadComments();
        });

        // Comment selection -> fill form
        commentListView2.getSelectionModel().selectedItemProperty().addListener((obs, oldV, c) -> {
            if (c == null) {
                refreshAdminCommentActionVisibility();
                return;
            }
            commentArea2.setText(c.getContent());
            commentStatusBox2.setValue(c.getStatus());
            refreshAdminCommentActionVisibility();
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
            if (!allPosts.isEmpty() && postListView.getSelectionModel().getSelectedItem() == null) {
                postListView.getSelectionModel().selectFirst();
            }
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

        TextField category = new TextField(editing ? safe(existing.getCategory()) : "");
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
        gp.addRow(2, new Label("Category"), category);
        gp.addRow(3, new Label("Status"), status);
        gp.addRow(4, pinned, locked);

        GridPane.setHgrow(title, Priority.ALWAYS);
        GridPane.setHgrow(category, Priority.ALWAYS);
        GridPane.setHgrow(content, Priority.ALWAYS);

        ScrollPane sp = new ScrollPane(gp);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setPrefViewportHeight(420);
        dialog.getDialogPane().setContent(sp);

        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        // Keep dialog open until all post validation errors are fixed.
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
            ForumPost p = editing ? existing : new ForumPost();
            p.setAuthorId(Session.getCurrentUserId());
            p.setTitle(InputValidator.norm(title.getText()));
            p.setContent(InputValidator.norm(content.getText()));
            p.setCategory(InputValidator.normalizeNullable(category.getText()));
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
        categoryChip2.setText(safe(p.getCategory()).isBlank() ? "General" : p.getCategory());
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
                return;
            }
            comments.setAll(commentRepo.findByPostId(selectedPost.getId()));
            commentListView2.setItems(comments);
            commentListView2.getSelectionModel().clearSelection();
            commentArea2.clear();
            commentStatusBox2.setValue("PENDING");
            refreshAdminCommentActionVisibility();
        } catch (Exception ex) {
            showError("Failed to load comments", ex);
        }
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
        // Admin window is treated as app-root; closing exits process.
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
