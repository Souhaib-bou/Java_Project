package Controllers;

import Models.OnboardingPlan;
import Models.User;
import Services.PlanService;
import Services.UserService;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class OnboardingPlanController implements Initializable {

    /* ================= FORM ================= */
    @FXML private ComboBox<User> cmbUser;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private DatePicker dpDeadline;

    /* ================= TABLE ================= */
    @FXML private TableView<OnboardingPlan> tvPlans;
    @FXML private TableColumn<OnboardingPlan, Integer> colPlanId;
    @FXML private TableColumn<OnboardingPlan, String> colUserName;
    @FXML private TableColumn<OnboardingPlan, String> colStatus;
    @FXML private TableColumn<OnboardingPlan, Date> colDeadline;
    @FXML private TableColumn<OnboardingPlan, Void> colActions;

    @FXML private Label lblStatus;

    /* header chip */
    @FXML private Label lblHeaderUserName;
    @FXML private Label lblHeaderInitials;

    /* one-window navigation host (must exist in Plan FXML) */
    @FXML private BorderPane appRoot;
    @FXML private StackPane contentHost;

    /* ================= SERVICES ================= */
    private PlanService planService;
    private UserService userService;

    private ObservableList<OnboardingPlan> plansList;
    private OnboardingPlan selectedPlan;

    // userId -> "First Last"
    private final Map<Integer, String> userNameById = new HashMap<>();

    // we keep the original plans UI node here so we can go back from Tasks
    private Parent plansContent;
    private MainShellController shell;

    /**
     * Sets the shell value.
     */
    public void setShell(MainShellController shell) {
        this.shell = shell;
    }

    @Override
    /**
     * Initializes UI components and loads initial data.
     */
    public void initialize(URL location, ResourceBundle resources) {

        planService = new PlanService();
        userService = new UserService();
        plansList = FXCollections.observableArrayList();

        // save the current plans view content (for back navigation)
        if (contentHost != null && !contentHost.getChildren().isEmpty()) {
            plansContent = (Parent) contentHost.getChildren().get(0);
        }

        setupStatus();
        setupTable();        // includes status renderer + actions renderer
        setupSelection();    // selection is still used for Update/Delete

        // header chip follows selected user in the FORM (optional)
        cmbUser.valueProperty().addListener((obs, oldU, newU) -> updateHeaderChip(newU));

        updateStatusLabel("Loading...");

        Task<Void> loadTask = new Task<>() {
            @Override
            /**
             * Executes this operation.
             */
            protected Void call() throws Exception {
                List<User> users = userService.getAllUsers();
                List<OnboardingPlan> plans = planService.getAllOnboardingPlans();

                Platform.runLater(() -> {
                    // users + map
                    cmbUser.setItems(FXCollections.observableArrayList(users));
                    userNameById.clear();
                    for (User u : users) {
                        userNameById.put(u.getUserId(), u.getFirstName() + " " + u.getLastName());
                    }

                    cmbUser.setConverter(new javafx.util.StringConverter<>() {
                        @Override public String toString(User u) {
                            return (u == null) ? "" : u.getFirstName() + " " + u.getLastName();
                        }
                        @Override public User fromString(String s) { return null; }
                    });

                    // plans
                    plansList.setAll(plans);
                    tvPlans.setItems(plansList);

                    // optional: pick first user so chip is never empty
                    if (!users.isEmpty() && cmbUser.getValue() == null) {
                        cmbUser.getSelectionModel().selectFirst();
                    }

                    updateStatusLabel("Loaded " + plans.size() + " plans");
                });

                return null;
            }
        };

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() -> showError("Loading Error", ex == null ? "Unknown error" : ex.getMessage()));
            updateStatusLabel("Load failed");
        });

        Thread th = new Thread(loadTask);
        th.setDaemon(true);
        th.start();
    }

    /* ================== ONE-WINDOW NAV ================== */

    /**
     * Navigates to the requested screen.
     */
    private void openTasksInSameWindow(int planId) {
        if (shell == null) {
            showError("Navigation Error", "Shell is not set. Cannot open tasks.");
            return;
        }
        shell.openTasks(planId); // ✅ let the shell swap the view
    }


    /**
     * Executes this operation.
     */
    private void showPlansContent() {
        if (contentHost == null || plansContent == null) return;

        contentHost.getChildren().setAll(plansContent);

        Stage stage = (Stage) appRoot.getScene().getWindow();
        stage.setTitle("Onboarding Management System — Plans");
    }

    /* ================== SETUP ================== */

    /**
     * Sets the upstatus value.
     */
    private void setupStatus() {
        cmbStatus.getItems().setAll("Pending", "In Progress", "Completed", "On Hold");
        cmbStatus.setValue("Pending");
    }

    /**
     * Sets the upselection value.
     */
    private void setupSelection() {
        tvPlans.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedPlan = newSel;
            if (newSel != null) populateFields(newSel);
        });
    }

    /**
     * Sets the uptable value.
     */
    private void setupTable() {
        colPlanId.setCellValueFactory(new PropertyValueFactory<>("planId"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));

        // display name from map
        colUserName.setCellValueFactory(cellData -> {
            int uid = cellData.getValue().getUserId();
            String name = userNameById.getOrDefault(uid, "Unknown");
            return new ReadOnlyStringWrapper(name);
        });

        // ✅ Status with dot + colored text
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<>() {

            private final HBox box = new HBox(8);
            private final Region dot = new Region();
            private final Label text = new Label();

            {
                box.getStyleClass().add("status-container");
                box.setAlignment(Pos.CENTER_LEFT);

                dot.getStyleClass().add("status-dot");
                text.getStyleClass().add("status-text");

                box.getChildren().addAll(dot, text);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            /**
             * Updates the selected record and refreshes the UI.
             */
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null || status.trim().isEmpty()) {
                    setGraphic(null);
                    return;
                }

                setGraphic(box);
                text.setText(status);

                dot.getStyleClass().removeAll(
                        "status-dot-pending","status-dot-progress","status-dot-completed","status-dot-hold","status-dot-neutral"
                );
                text.getStyleClass().removeAll(
                        "status-text-pending","status-text-progress","status-text-completed","status-text-hold","status-text-neutral"
                );

                String s = status.trim().toLowerCase();
                if (s.equals("pending")) {
                    dot.getStyleClass().add("status-dot-pending");
                    text.getStyleClass().add("status-text-pending");
                } else if (s.equals("in progress")) {
                    dot.getStyleClass().add("status-dot-progress");
                    text.getStyleClass().add("status-text-progress");
                } else if (s.equals("completed")) {
                    dot.getStyleClass().add("status-dot-completed");
                    text.getStyleClass().add("status-text-completed");
                } else if (s.equals("on hold")) {
                    dot.getStyleClass().add("status-dot-hold");
                    text.getStyleClass().add("status-text-hold");
                } else {
                    dot.getStyleClass().add("status-dot-neutral");
                    text.getStyleClass().add("status-text-neutral");
                }
            }
        });

        // ✅ Actions column: button per row
        setupActionsColumn();
    }

    /**
     * Sets the upactionscolumn value.
     */
    private void setupActionsColumn() {
        if (colActions == null) return;

        colActions.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("Tasks");

            {
                btn.getStyleClass().addAll("btn-primary", "btn-table");

                btn.setOnAction(e -> {
                    OnboardingPlan plan = getTableView().getItems().get(getIndex());
                    if (plan != null) openTasksInSameWindow(plan.getPlanId());
                });

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            /**
             * Updates the selected record and refreshes the UI.
             */
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    /* ================== FORM FILL ================== */

    /**
     * Executes this operation.
     */
    private void populateFields(OnboardingPlan plan) {
        // select user object in combobox
        for (User u : cmbUser.getItems()) {
            if (u.getUserId() == plan.getUserId()) {
                cmbUser.setValue(u);
                break;
            }
        }

        cmbStatus.setValue(plan.getStatus());

        if (plan.getDeadline() != null) {
            LocalDate localDate = new java.sql.Date(plan.getDeadline().getTime()).toLocalDate();
            dpDeadline.setValue(localDate);
        } else {
            dpDeadline.setValue(null);
        }
    }

    /* ================== CRUD ================== */

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleAddPlan() {
        if (!validateInputs()) return;

        try {
            int userId = cmbUser.getValue().getUserId();
            Date deadline = Date.from(dpDeadline.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());

            OnboardingPlan plan = new OnboardingPlan(0, userId, cmbStatus.getValue(), deadline);
            int id = planService.addOnboardingPlan(plan);

            showInfo("Success", "Plan added with ID: " + id);
            reloadPlansOnly();
            handleClear();

        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleUpdatePlan() {
        if (selectedPlan == null) {
            showWarning("No Selection", "Select a plan first (for Update/Delete).");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/OnboardingPlanEditView.fxml"));
            Parent root = loader.load();

            OnboardingPlanEditController editController = loader.getController();
            editController.setPlan(selectedPlan);

            Scene scene = new Scene(root, 720, 520);
            scene.getStylesheets().add(getClass().getResource("/styles/hirely.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("Update Plan");
            stage.setScene(scene);
            stage.initOwner(tvPlans.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(640);
            stage.setMinHeight(420);

            stage.showAndWait();

            reloadPlansOnly();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Cannot open update window. Check console stack trace.");
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleDeletePlan() {
        if (selectedPlan == null) {
            showWarning("No Selection", "Select a plan first (for Update/Delete).");
            return;
        }

        Optional<ButtonType> result =
                new Alert(Alert.AlertType.CONFIRMATION, "Delete this plan?").showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                planService.deleteOnboardingPlan(selectedPlan.getPlanId());
                showInfo("Success", "Plan deleted.");
                reloadPlansOnly();
                handleClear();
            } catch (SQLException e) {
                showError("Database Error", e.getMessage());
            }
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleClear() {
        cmbUser.setValue(null);
        cmbStatus.setValue("Pending");
        dpDeadline.setValue(null);
        selectedPlan = null;
        tvPlans.getSelectionModel().clearSelection();
        updateStatusLabel("Ready");
    }

    // keep it if your FXML still calls it
    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleManageTasks() {
        showWarning("Info", "Use the Tasks button inside the table row.");
    }

    /**
     * Executes this operation.
     */
    private void reloadPlansOnly() {
        try {
            List<OnboardingPlan> plans = planService.getAllOnboardingPlans();
            plansList.setAll(plans);
            tvPlans.setItems(plansList);
            updateStatusLabel("Loaded " + plans.size() + " plans");
        } catch (SQLException e) {
            showError("Error loading plans", e.getMessage());
        }
    }

    /* ================== HELPERS ================== */

    /**
     * Executes this operation.
     */
    private boolean validateInputs() {
        if (cmbUser.getValue() == null || dpDeadline.getValue() == null) {
            showWarning("Validation Error", "Select a user and deadline.");
            return false;
        }
        return true;
    }

    /**
     * Updates the selected record and refreshes the UI.
     */
    private void updateHeaderChip(User u) {
        if (u == null) return;

        if (lblHeaderUserName != null) lblHeaderUserName.setText(u.getFullName());

        String fn = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String ln = u.getLastName() == null ? "" : u.getLastName().trim();

        String i1 = fn.isEmpty() ? "" : fn.substring(0, 1).toUpperCase();
        String i2 = ln.isEmpty() ? "" : ln.substring(0, 1).toUpperCase();

        String initials = (i1 + i2).trim();
        if (lblHeaderInitials != null) lblHeaderInitials.setText(initials.isEmpty() ? "U" : initials);
    }

    /**
     * Updates the selected record and refreshes the UI.
     */
    private void updateStatusLabel(String msg) {
        if (lblStatus != null) lblStatus.setText(msg);
    }

    /**
     * Executes this operation.
     */
    private void showInfo(String t, String c) {
        new Alert(Alert.AlertType.INFORMATION, c).showAndWait();
    }

    /**
     * Executes this operation.
     */
    private void showError(String t, String c) {
        new Alert(Alert.AlertType.ERROR, c).showAndWait();
    }

    /**
     * Executes this operation.
     */
    private void showWarning(String t, String c) {
        new Alert(Alert.AlertType.WARNING, c).showAndWait();
    }
}
