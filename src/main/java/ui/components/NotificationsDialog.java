package ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Notification;
import repo.NotificationRepository;
import util.DebugLog;
import util.Session;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Notification center modal for users.
 */
public final class NotificationsDialog {
    private static final int DEFAULT_LIMIT = 100;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");

    private NotificationsDialog() {
    }

    public static void show(long userId, NotificationRepository repo, Runnable onChanged, Consumer<Long> onOpenPost) {
        ObservableList<Notification> items = FXCollections.observableArrayList();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Notification Center");
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().getStylesheets()
                .add(NotificationsDialog.class.getResource(Session.getThemeStylesheetPath()).toExternalForm());
        dialog.getDialogPane().getStyleClass().add("root");
        dialog.getDialogPane().setPrefWidth(760);
        dialog.getDialogPane().setPrefHeight(620);

        Label unreadLabel = new Label("Unread: 0");
        unreadLabel.getStyleClass().add("muted");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("secondary");
        Button markReadBtn = new Button("Mark read");
        markReadBtn.getStyleClass().add("secondary");
        Button markAllBtn = new Button("Mark all read");
        markAllBtn.getStyleClass().add("secondary");
        Button openBtn = new Button("Open");
        openBtn.getStyleClass().add("primary");
        openBtn.setDisable(true);

        ListView<Notification> listView = new ListView<>(items);
        listView.setCellFactory(lv -> new NotificationCell());
        VBox.setVgrow(listView, Priority.ALWAYS);

        HBox actions = new HBox(8, refreshBtn, markReadBtn, markAllBtn);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, actions, spacer, unreadLabel, openBtn);

        VBox root = new VBox(10, toolbar, listView);
        root.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(root);

        Runnable reload = () -> {
            try {
                List<Notification> latest = repo.findLatestForUser(userId, DEFAULT_LIMIT);
                int unread = repo.countUnread(userId);
                items.setAll(latest);
                unreadLabel.setText("Unread: " + unread);
                if (onChanged != null) {
                    onChanged.run();
                }
            } catch (Exception ex) {
                DebugLog.error("NotificationsDialog", "Failed loading notifications", ex);
                showWarning("Failed to load notifications: " + ex.getMessage());
            }
        };

        refreshBtn.setOnAction(evt -> reload.run());

        markReadBtn.setOnAction(evt -> {
            Notification n = listView.getSelectionModel().getSelectedItem();
            if (n == null) {
                return;
            }
            try {
                repo.markRead(n.getId(), userId);
                reload.run();
            } catch (Exception ex) {
                DebugLog.error("NotificationsDialog", "Failed marking notification read #" + n.getId(), ex);
                showWarning("Failed to mark as read: " + ex.getMessage());
            }
        });

        markAllBtn.setOnAction(evt -> {
            try {
                repo.markAllRead(userId);
                reload.run();
            } catch (Exception ex) {
                DebugLog.error("NotificationsDialog", "Failed marking all notifications read for user #" + userId, ex);
                showWarning("Failed to mark all read: " + ex.getMessage());
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            openBtn.setDisable(selected == null || selected.getPostId() == null);
        });

        openBtn.setOnAction(evt -> {
            Notification n = listView.getSelectionModel().getSelectedItem();
            if (n == null || n.getPostId() == null) {
                showWarning("Selected notification has no post to open.");
                return;
            }
            try {
                repo.markRead(n.getId(), userId);
            } catch (Exception ex) {
                DebugLog.error("NotificationsDialog", "Failed marking opened notification read #" + n.getId(), ex);
            }
            reload.run();
            if (onOpenPost != null) {
                onOpenPost.accept(n.getPostId());
            }
        });

        listView.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2 && !openBtn.isDisabled()) {
                openBtn.fire();
            }
        });

        reload.run();
        dialog.showAndWait();
    }

    private static void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Notification Center");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static final class NotificationCell extends ListCell<Notification> {
        private final VBox card = new VBox(6);
        private final Label message = new Label();
        private final Label meta = new Label();

        private NotificationCell() {
            card.getStyleClass().add("card");
            message.setWrapText(true);
            meta.getStyleClass().add("muted");
            card.getChildren().addAll(message, meta);
            selectedProperty().addListener((obs, wasSelected, isSelected) -> applyCardStyle(getItem()));
        }

        @Override
        protected void updateItem(Notification item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            message.setText(item.getMessage() == null ? "" : item.getMessage());
            String when = item.getCreatedAt() == null ? "" : DT.format(item.getCreatedAt());
            String status = item.isRead() ? "Read" : "Unread";
            meta.setText(status + (when.isBlank() ? "" : " - " + when));

            applyCardStyle(item);
            setGraphic(card);
        }

        private void applyCardStyle(Notification item) {
            if (item == null) {
                card.setStyle("");
                return;
            }
            boolean selected = isSelected();
            String borderColor = item.isRead() ? "rgba(0,0,0,0.14)" : "-hirely-orange";
            String borderWidth = item.isRead() ? "1.0" : "1.4";
            String background = selected ? "rgba(255, 166, 77, 0.16)" : "transparent";
            card.setStyle(
                    "-fx-background-color: " + background + ";" +
                            "-fx-border-color: " + borderColor + ";" +
                            "-fx-border-width: " + borderWidth + ";" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 8 10 8 10;");
        }
    }
}
