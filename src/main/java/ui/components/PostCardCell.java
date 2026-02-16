package ui.components;

import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.ForumPost;
import repo.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Reusable list cell that renders forum posts as styled cards.
 * Used in feed/profile/admin lists with optional status chip visibility.
 */
public class PostCardCell extends ListCell<ForumPost> {

    private final VBox card = new VBox(6);
    private final Label title = new Label();
    private final Label preview = new Label();
    private final Label meta = new Label();
    private final HBox chipsRow = new HBox(8);
    private final Label catChip = new Label();
    private final Label statusChip = new Label();
    private final Label flags = new Label();

    private final UserRepository userRepo;
    private final Map<Long, String> userNameCache = new HashMap<>();
    private final boolean showStatus;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");
    private static final PseudoClass CARD_SELECTED = PseudoClass.getPseudoClass("selected");

    public PostCardCell(UserRepository userRepo) {
        this(userRepo, true);
    }

    public PostCardCell(UserRepository userRepo, boolean showStatus) {
        this.userRepo = userRepo;
        this.showStatus = showStatus;

        // Shared JavaFX style hooks used by hirely.css.
        card.getStyleClass().add("card");
        title.getStyleClass().add("post-card-title");
        preview.getStyleClass().add("post-card-preview");
        meta.getStyleClass().add("muted");
        flags.getStyleClass().add("muted");
        catChip.getStyleClass().add("chip");
        statusChip.getStyleClass().add("chip");
        preview.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox metaRow = new HBox(8, meta, spacer, flags);
        chipsRow.getChildren().addAll(catChip, statusChip);

        card.getChildren().addAll(title, preview, metaRow, chipsRow);

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
        preview.setText(makePreview(safe(p.getContent()), 140));

        String author = displayName(p.getAuthorId());
        String when = formatTime(p.getCreatedAt());
        meta.setText(author + (when.isBlank() ? "" : " - " + when));

        catChip.setText(safe(p.getCategory()).isBlank() ? "General" : p.getCategory());

        String statusText = safe(p.getStatus());
        statusChip.setText(statusText);
        statusChip.setVisible(showStatus && !statusText.isBlank());
        statusChip.setManaged(showStatus && !statusText.isBlank());

        // Visual flags for moderation/interaction state.
        String f = "";
        if (p.isPinned())
            f += "\uD83D\uDCCC ";
        if (p.isLocked())
            f += "\uD83D\uDD12";
        flags.setText(f.trim());

        card.pseudoClassStateChanged(CARD_SELECTED, isSelected());
        setGraphic(card);
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
