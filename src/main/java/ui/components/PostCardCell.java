package ui.components;

import javafx.animation.PauseTransition;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.ForumPost;
import repo.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reusable list cell that renders forum posts as styled cards.
 * Used in feed/profile/admin lists with optional status chip visibility.
 */
public class PostCardCell extends ListCell<ForumPost> {

    private final VBox card = new VBox(9);
    private final VBox headingBox = new VBox(4);
    private final HBox titleRow = new HBox(8);
    private final Label title = new Label();
    private final Label meta = new Label();
    private final Label preview = new Label();
    private final HBox footerRow = new HBox(10);
    private final HBox footerLeft = new HBox(8);
    private final HBox footerRight = new HBox(8);
    private final Label tagBadge = new Label();
    private final Label statusChip = new Label();
    private final Label dupChip = new Label();
    private final Label flags = new Label();

    private final Button likeBtn = new Button();
    private final Label likesLabel = new Label();
    private final Button shareBtn = new Button();
    private final Label shareCountLabel = new Label();
    private final Label shareHint = new Label();

    private final UserRepository userRepo;
    private final Map<Long, String> userNameCache = new HashMap<>();
    private final boolean showStatus;
    private final Consumer<ForumPost> likeToggleHandler;
    private final Function<ForumPost, String> shareHandler;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");
    private static final PseudoClass CARD_SELECTED = PseudoClass.getPseudoClass("selected");

    public PostCardCell(UserRepository userRepo) {
        this(userRepo, true, null, null);
    }

    public PostCardCell(UserRepository userRepo, boolean showStatus) {
        this(userRepo, showStatus, null, null);
    }

    public PostCardCell(UserRepository userRepo, boolean showStatus, Consumer<ForumPost> likeToggleHandler) {
        this(userRepo, showStatus, likeToggleHandler, null);
    }

    public PostCardCell(UserRepository userRepo, boolean showStatus, Consumer<ForumPost> likeToggleHandler,
            Function<ForumPost, String> shareHandler) {
        this.userRepo = userRepo;
        this.showStatus = showStatus;
        this.likeToggleHandler = likeToggleHandler;
        this.shareHandler = shareHandler;

        // Shared JavaFX style hooks used by hirely.css.
        setPrefHeight(USE_COMPUTED_SIZE);
        setMinHeight(USE_PREF_SIZE);
        card.getStyleClass().add("post-card");
        card.prefWidthProperty().bind(widthProperty().subtract(24));

        title.getStyleClass().add("post-title");
        meta.getStyleClass().add("post-meta");
        preview.getStyleClass().add("post-preview");
        flags.getStyleClass().add("muted");
        tagBadge.getStyleClass().add("tag-badge");
        statusChip.getStyleClass().addAll("pill", "gray");
        dupChip.getStyleClass().addAll("pill", "gray");
        likesLabel.getStyleClass().addAll("post-meta", "social-count");
        shareCountLabel.getStyleClass().addAll("post-meta", "social-count");
        shareHint.getStyleClass().add("muted");
        title.setWrapText(true);
        preview.setWrapText(true);
        preview.setMaxWidth(Double.MAX_VALUE);
        tagBadge.setTextOverrun(OverrunStyle.CLIP);
        statusChip.setTextOverrun(OverrunStyle.CLIP);
        dupChip.setTextOverrun(OverrunStyle.CLIP);
        tagBadge.setMinWidth(Region.USE_PREF_SIZE);
        statusChip.setMinWidth(Region.USE_PREF_SIZE);
        dupChip.setMinWidth(Region.USE_PREF_SIZE);

        likeBtn.getStyleClass().addAll("social-icon-btn", "like-btn");
        shareBtn.getStyleClass().addAll("social-icon-btn", "share-btn");
        likeBtn.setContentDisplay(ContentDisplay.TEXT_ONLY);
        shareBtn.setContentDisplay(ContentDisplay.TEXT_ONLY);
        likeBtn.setText("\u2661");
        shareBtn.setText("\u2197");
        shareHint.setVisible(false);
        shareHint.setManaged(false);

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        titleRow.setAlignment(Pos.TOP_LEFT);
        titleRow.getChildren().addAll(title, titleSpacer, flags);

        headingBox.getChildren().addAll(titleRow, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        footerRight.setAlignment(Pos.CENTER_RIGHT);
        footerLeft.getChildren().addAll(tagBadge, statusChip);
        footerRight.getChildren().addAll(likeBtn, likesLabel, shareBtn, shareCountLabel, shareHint);
        footerRow.getChildren().addAll(footerLeft, spacer, footerRight);

        card.getChildren().addAll(headingBox, preview, footerRow);

        card.setOnMouseEntered(e -> card.setCursor(javafx.scene.Cursor.HAND));
        card.setOnMouseExited(e -> card.setCursor(javafx.scene.Cursor.DEFAULT));

        // Keep custom card selected state synchronized with list selection.
        selectedProperty().addListener((obs, oldV, isSel) -> card.pseudoClassStateChanged(CARD_SELECTED, isSel));
    }

    @Override
    protected void updateItem(ForumPost p, boolean empty) {
        super.updateItem(p, empty);
        if (empty || p == null) {
            setGraphic(null);
            return;
        }

        title.setText(safe(p.getTitle()));
        preview.setText(makePreview(safe(p.getContent()), 260));

        String author = displayName(p.getAuthorId());
        String when = formatTime(p.getCreatedAt());
        meta.setText(when.isBlank() ? author : (author + " - " + when));

        tagBadge.setText(safe(p.getTag()).isBlank() ? "General" : p.getTag());

        String statusText = safe(p.getStatus());
        statusChip.setText(statusText);
        statusChip.setVisible(showStatus && !statusText.isBlank());
        statusChip.setManaged(showStatus && !statusText.isBlank());

        dupChip.setVisible(false);
        dupChip.setManaged(false);

        likesLabel.setText(Integer.toString(Math.max(0, p.getLikeCount())));
        shareCountLabel.setText(Integer.toString(Math.max(0, p.getShareCount())));
        likeBtn.getStyleClass().remove("liked");
        if (p.isLikedByCurrentUser()) {
            likeBtn.getStyleClass().add("liked");
        }
        likeBtn.setText(p.isLikedByCurrentUser() ? "\u2665" : "\u2661");

        boolean canLike = likeToggleHandler != null;
        likeBtn.setDisable(!canLike);
        if (canLike) {
            likeBtn.setOnAction(event -> {
                event.consume();
                likeToggleHandler.accept(p);
            });
        } else {
            likeBtn.setOnAction(null);
        }

        boolean canShare = shareHandler != null && isShareAllowed(p);
        shareBtn.setDisable(!canShare);
        if (canShare) {
            shareBtn.setOnAction(event -> {
                event.consume();
                copyShareTextToClipboard(p);
                String hint = "Copied";
                if (shareHandler != null) {
                    try {
                        String out = shareHandler.apply(p);
                        if (out != null && !out.isBlank()) {
                            hint = out.trim();
                        }
                    } catch (Exception ignored) {
                        hint = "Copied";
                    }
                }
                showShareCopiedHint(hint);
            });
        } else {
            shareBtn.setOnAction(null);
        }

        // Visual flags for moderation/interaction state.
        String f = "";
        if (p.isPinned()) {
            f += "\uD83D\uDCCC ";
        }
        if (p.isLocked()) {
            f += "\uD83D\uDD12";
        }
        flags.setText(f.trim());

        card.pseudoClassStateChanged(CARD_SELECTED, isSelected());
        setGraphic(card);
    }

    private void showShareCopiedHint(String text) {
        shareHint.setText((text == null || text.isBlank()) ? "Copied" : text);
        shareHint.setVisible(true);
        shareHint.setManaged(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(evt -> {
            shareHint.setVisible(false);
            shareHint.setManaged(false);
            shareHint.setText("");
        });
        pause.playFromStart();
    }

    private void copyShareTextToClipboard(ForumPost p) {
        String text = buildShareText(p);
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private String buildShareText(ForumPost p) {
        String titleText = safe(p == null ? "" : p.getTitle());
        long postId = p == null ? 0 : p.getId();
        String snippet = makePreview(safe(p == null ? "" : p.getContent()), 120);
        return "[" + titleText + "] (Post #" + postId + ")\n" + snippet;
    }

    private boolean isShareAllowed(ForumPost p) {
        return p != null && "APPROVED".equalsIgnoreCase(safe(p.getStatus()));
    }

    private String displayName(long userId) {
        // Cache avoids repeated DB lookups while users scroll the list.
        return userNameCache.computeIfAbsent(userId, id -> {
            String name = userRepo.getDisplayNameById(id);
            return (name == null || name.isBlank()) ? "User #" + id : name;
        });
    }

    private String formatTime(LocalDateTime dt) {
        return dt == null ? "" : DT.format(dt);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String makePreview(String s, int max) {
        String t = s.replace("\n", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
