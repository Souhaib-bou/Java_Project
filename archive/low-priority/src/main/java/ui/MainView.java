package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.ForumComment;
import model.ForumPost;
import repo.ForumCommentRepository;
import repo.ForumPostRepository;

public class MainView extends BorderPane {

    private final ForumPostRepository postRepo = new ForumPostRepository();
    private final ForumCommentRepository commentRepo = new ForumCommentRepository();

    // For now: hardcode Ali user_id = 1
    private final long CURRENT_USER_ID = 1;

    // ===== Posts UI =====
    private TableView<ForumPost> postTable;
    private ObservableList<ForumPost> postList;

    private TextField titleField;
    private TextArea contentArea;
    private TextField categoryField;
    private ComboBox<String> statusBox;
    private CheckBox pinnedBox;
    private CheckBox lockedBox;
    // ===== Posts UI =====
    private Button addBtn;
    private Button updateBtn;
    private Button deleteBtn;
    private Button clearBtn;
    private Button refreshBtn;

    private Button addCommentBtn, updateCommentBtn, deleteCommentBtn, clearCommentBtn, refreshCommentsBtn;

    // ===== Comments UI =====
    private Label selectedPostLabel;

    private TableView<ForumComment> commentTable;
    private ObservableList<ForumComment> commentList;

    private TextArea commentArea;
    private ComboBox<String> commentStatusBox;

    // Keep track of selected post
    private ForumPost selectedPost;

    public MainView() {
        buildUI();
        loadPosts();
        bindPostSelection();
    }

    private void buildUI() {
        TabPane tabs = new TabPane();

        Tab postsTab = new Tab("Posts");
        postsTab.setClosable(false);
        postsTab.setContent(buildPostsTab());

        Tab commentsTab = new Tab("Comments");
        commentsTab.setClosable(false);
        commentsTab.setContent(buildCommentsTab());

        tabs.getTabs().addAll(postsTab, commentsTab);
        setCenter(tabs);
    }

    // =========================
    // POSTS TAB
    // =========================
    private Pane buildPostsTab() {
        BorderPane root = new BorderPane();

        postTable = new TableView<>();

        TableColumn<ForumPost, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);

        TableColumn<ForumPost, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(260);

        TableColumn<ForumPost, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);

        TableColumn<ForumPost, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(160);

        postTable.getColumns().addAll(idCol, titleCol, statusCol, categoryCol);
        root.setCenter(postTable);

        VBox form = new VBox(10);
        form.setPadding(new Insets(10));
        form.setPrefWidth(340);

        Label header = new Label("Post CRUD");

        titleField = new TextField();
        titleField.setPromptText("Title");

        contentArea = new TextArea();
        contentArea.setPromptText("Content");
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(8);

        categoryField = new TextField();
        categoryField.setPromptText("Category (optional)");

        statusBox = new ComboBox<>();
        statusBox.getItems().addAll("PENDING", "APPROVED", "REJECTED");
        statusBox.setValue("PENDING");

        pinnedBox = new CheckBox("Pinned");
        lockedBox = new CheckBox("Locked");

        addBtn = new Button("Add");
        updateBtn = new Button("Update");
        deleteBtn = new Button("Delete");
        clearBtn = new Button("Clear");
        refreshBtn = new Button("Refresh");

        HBox actions1 = new HBox(10, addBtn, updateBtn);
        HBox actions2 = new HBox(10, deleteBtn, clearBtn);
        HBox actions3 = new HBox(10, refreshBtn);

        addBtn.setOnAction(e -> onAddPost());
        deleteBtn.setOnAction(e -> onDeletePost());
        clearBtn.setOnAction(e -> clearPostForm());
        refreshBtn.setOnAction(e -> loadPosts());
        updateBtn.setOnAction(e -> onUpdatePost());

        form.getChildren().addAll(
                header,
                new Label("Title"), titleField,
                new Label("Content"), contentArea,
                new Label("Category"), categoryField,
                new Label("Status"), statusBox,
                pinnedBox, lockedBox,
                actions1, actions2, actions3);

        root.setRight(form);
        return root;
    }

    private void loadPosts() {
        try {
            postList = FXCollections.observableArrayList(postRepo.findAll());
            postTable.setItems(postList);
        } catch (Exception ex) {
            showError("Failed to load posts", ex);
        }
    }

    private void bindPostSelection() {
        postTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, p) -> {
            selectedPost = p;

            if (p == null) {
                clearPostForm();
                setSelectedPostLabel(null);
                clearCommentsUI();
                return;
            }

            // Fill post form
            titleField.setText(p.getTitle());
            contentArea.setText(p.getContent());
            categoryField.setText(p.getCategory());
            statusBox.setValue(p.getStatus());
            pinnedBox.setSelected(p.isPinned());
            lockedBox.setSelected(p.isLocked());

            // Update comment tab label + load comments
            setSelectedPostLabel(p);
            loadCommentsForSelectedPost();
        });
    }

    // ===== Post CRUD =====
    private void onAddPost() {
        String title = titleField.getText();
        String content = contentArea.getText();
        String category = categoryField.getText();
        String status = statusBox.getValue();

        String validation = validatePost(title, content, category, status);
        if (validation != null) {
            showWarning(validation);
            return;
        }

        ForumPost p = new ForumPost();
        p.setAuthorId(CURRENT_USER_ID);
        p.setTitle(title.trim());
        p.setContent(content.trim());
        p.setCategory(normalizeNullable(category));
        p.setStatus(status);
        p.setPinned(pinnedBox.isSelected());
        p.setLocked(lockedBox.isSelected());

        try {
            postRepo.insert(p);
            showInfo("Post added successfully");
            clearPostForm();
            loadPosts();
        } catch (Exception ex) {
            showError("Failed to add post", ex);
        }
    }

    private void onUpdatePost() {
        ForumPost selected = postTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a post to update.");
            return;
        }

        String title = titleField.getText();
        String content = contentArea.getText();
        String category = categoryField.getText();
        String status = statusBox.getValue();

        String validation = validatePost(title, content, category, status);
        if (validation != null) {
            showWarning(validation);
            return;
        }

        selected.setTitle(title.trim());
        selected.setContent(content.trim());
        selected.setCategory(normalizeNullable(category));
        selected.setStatus(status);
        selected.setPinned(pinnedBox.isSelected());
        selected.setLocked(lockedBox.isSelected());

        try {
            postRepo.update(selected);
            showInfo("Post updated successfully");
            clearPostForm();
            loadPosts();
        } catch (Exception ex) {
            showError("Failed to update post", ex);
        }
    }

    private void onDeletePost() {
        ForumPost selected = postTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a post to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete post #" + selected.getId() + "?");
        confirm.setContentText("This will also delete its comments (ON DELETE CASCADE).");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;

        try {
            postRepo.delete(selected.getId());
            showInfo("Post deleted");
            clearPostForm();
            loadPosts();

            // If deleted post was selected for comments
            selectedPost = null;
            setSelectedPostLabel(null);
            clearCommentsUI();

        } catch (Exception ex) {
            showError("Failed to delete post", ex);
        }
    }

    private void clearPostForm() {
        titleField.clear();
        contentArea.clear();
        categoryField.clear();
        statusBox.setValue("PENDING");
        pinnedBox.setSelected(false);
        lockedBox.setSelected(false);
        postTable.getSelectionModel().clearSelection();
    }

    private String validatePost(String title, String content, String category, String status) {
        if (title == null || title.trim().isEmpty())
            return "Title is required.";
        if (title.trim().length() < 3)
            return "Title must be at least 3 characters.";
        if (title.trim().length() > 255)
            return "Title must be <= 255 characters.";

        if (content == null || content.trim().isEmpty())
            return "Content is required.";
        if (content.trim().length() < 5)
            return "Content must be at least 5 characters.";

        if (category != null && category.trim().length() > 100)
            return "Category must be <= 100 characters.";

        if (status == null || status.isBlank())
            return "Status is required.";
        return null;
    }

    // =========================
    // COMMENTS TAB
    // =========================
    private Pane buildCommentsTab() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        selectedPostLabel = new Label("Selected post: none");
        root.setTop(selectedPostLabel);

        commentTable = new TableView<>();

        TableColumn<ForumComment, Long> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(new PropertyValueFactory<>("id"));
        cId.setPrefWidth(80);

        TableColumn<ForumComment, String> cContent = new TableColumn<>("Content");
        cContent.setCellValueFactory(new PropertyValueFactory<>("content"));
        cContent.setPrefWidth(520);

        TableColumn<ForumComment, String> cStatus = new TableColumn<>("Status");
        cStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        cStatus.setPrefWidth(120);

        commentTable.getColumns().addAll(cId, cContent, cStatus);
        root.setCenter(commentTable);

        VBox form = new VBox(10);
        form.setPadding(new Insets(10));
        form.setPrefWidth(360);

        Label header = new Label("Comment CRUD");

        commentArea = new TextArea();
        commentArea.setPromptText("Write a comment...");
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(8);

        commentStatusBox = new ComboBox<>();
        commentStatusBox.getItems().addAll("PENDING", "APPROVED", "REJECTED");
        commentStatusBox.setValue("PENDING");

        addCommentBtn = new Button("Add Comment");
        deleteCommentBtn = new Button("Delete Comment");
        clearCommentBtn = new Button("Clear");
        refreshCommentsBtn = new Button("Refresh");
        updateCommentBtn = new Button("Update Comment");

        addCommentBtn.setOnAction(e -> onAddComment());
        deleteCommentBtn.setOnAction(e -> onDeleteComment());
        clearCommentBtn.setOnAction(e -> clearCommentForm());
        refreshCommentsBtn.setOnAction(e -> loadCommentsForSelectedPost());
        updateCommentBtn.setOnAction(e -> onUpdateComment());

        HBox actions1 = new HBox(10, addCommentBtn, updateCommentBtn);
        HBox actions2 = new HBox(10, deleteCommentBtn, clearCommentBtn);
        HBox actions3 = new HBox(10, refreshCommentsBtn);

        form.getChildren().addAll(
                header,
                new Label("Content"), commentArea,
                new Label("Status"), commentStatusBox,
                actions1, actions2, actions3);

        root.setRight(form);
        bindCommentSelection();
        return root;
    }

    private void setSelectedPostLabel(ForumPost p) {
        if (p == null) {
            selectedPostLabel.setText("Selected post: none");
        } else {
            selectedPostLabel.setText("Selected post: #" + p.getId() + " - " + p.getTitle());
        }
    }

    private void bindCommentSelection() {
        commentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, c) -> {
            if (c == null)
                return;

            commentArea.setText(c.getContent());
            commentStatusBox.setValue(c.getStatus());
        });
    }

    private void loadCommentsForSelectedPost() {
        try {
            if (selectedPost == null) {
                commentTable.setItems(FXCollections.observableArrayList());
                return;
            }

            commentList = FXCollections.observableArrayList(
                    commentRepo.findByPostId(selectedPost.getId()));
            commentTable.setItems(commentList);
            commentTable.getSelectionModel().clearSelection();
        } catch (Exception ex) {
            showError("Failed to load comments", ex);
        }
    }

    private void onAddComment() {
        if (selectedPost == null) {
            showWarning("Select a post first (Posts tab).");
            return;
        }

        String content = commentArea.getText();
        String status = commentStatusBox.getValue();

        String validation = validateComment(content, status);
        if (validation != null) {
            showWarning(validation);
            return;
        }

        ForumComment c = new ForumComment();
        c.setPostId(selectedPost.getId());
        c.setAuthorId(CURRENT_USER_ID);
        c.setContent(content.trim());
        c.setStatus(status);

        try {
            commentRepo.insert(c);
            showInfo("Comment added");
            clearCommentForm();
            loadCommentsForSelectedPost();
        } catch (Exception ex) {
            showError("Failed to add comment", ex);
        }
    }

    private void onDeleteComment() {
        ForumComment selected = commentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a comment to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete comment #" + selected.getId() + "?");
        confirm.setContentText("This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;

        try {
            commentRepo.delete(selected.getId());
            showInfo("Comment deleted");
            clearCommentForm();
            loadCommentsForSelectedPost();
        } catch (Exception ex) {
            showError("Failed to delete comment", ex);
        }
    }

    private void clearCommentForm() {
        commentArea.clear();
        commentStatusBox.setValue("PENDING");
        commentTable.getSelectionModel().clearSelection();
    }

    private void clearCommentsUI() {
        clearCommentForm();
        commentTable.setItems(FXCollections.observableArrayList());
    }

    private String validateComment(String content, String status) {
        if (content == null || content.trim().isEmpty())
            return "Comment content is required.";
        if (content.trim().length() < 2)
            return "Comment must be at least 2 characters.";
        if (content.trim().length() > 1000)
            return "Comment must be <= 1000 characters.";
        if (status == null || status.isBlank())
            return "Status is required.";
        return null;
    }

    // =========================
    // Helpers + Alerts
    // =========================
    private String normalizeNullable(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
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

    private void onUpdateComment() {
        ForumComment selected = commentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a comment to update.");
            return;
        }

        String content = commentArea.getText();
        String status = commentStatusBox.getValue();

        String validation = validateComment(content, status);
        if (validation != null) {
            showWarning(validation);
            return;
        }

        selected.setContent(content.trim());
        selected.setStatus(status);

        try {
            commentRepo.update(selected);
            showInfo("Comment updated");
            clearCommentForm();
            loadCommentsForSelectedPost();
        } catch (Exception ex) {
            showError("Failed to update comment", ex);
        }
    }
}
