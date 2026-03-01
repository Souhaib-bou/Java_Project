package Controllers;

import Models.OnboardingPlan;
import Models.User;
import Services.PlanService;
import Services.UserService;
import Services.api.PlanApiService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import javafx.geometry.Insets;
import javafx.geometry.HPos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentHashMap;

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

    @FXML private Button btnClear;
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnSaveStatus;

    @FXML private Label lblStatus;

    /* header chip (optional if present in FXML) */
    @FXML private Label lblHeaderUserName;
    @FXML private Label lblHeaderInitials;

    /* ===== OPTIONAL PLAN EXPLORER CONTROLS (safe if not present in FXML yet) ===== */
    @FXML private TextField txtPlanSearch;
    @FXML private ComboBox<String> cmbPlanFilterStatus;
    @FXML private ComboBox<String> cmbPlanSort;
    @FXML private CheckBox chkOverdueOnly;

    /* ===== OPTIONAL SUMMARY CARDS LABELS (safe if not present in FXML yet) ===== */
    @FXML private Label lblMetricTotal;
    @FXML private Label lblMetricCompleted;
    @FXML private Label lblMetricInProgress;
    @FXML private Label lblMetricOverdue;

    /* one-window navigation host (optional depending on shell integration) */
    @FXML private BorderPane appRoot;
    @FXML private StackPane contentHost;
    @FXML private VBox plansCardsContainer;


    private boolean cardsScrollHandlerInstalled = false;
    /* ================= SERVICES ================= */
    private PlanService planService;
    private UserService userService;
    private PlanApiService planApiService;

    /* ================= STATE ================= */
    private final ObservableList<OnboardingPlan> plansList = FXCollections.observableArrayList(); // raw data from API
    private FilteredList<OnboardingPlan> filteredPlans;

    private final Map<Integer, String> userNameById = new HashMap<>();

    private OnboardingPlan selectedPlan;

    // keep original plans UI node if needed later
    private Parent plansContent;
    private MainShellController shell;

    // cache QR images so we don't re-download constantly
    private final Map<Integer, Image> qrCache = new ConcurrentHashMap<>();
    /**
     * Shell setter (used by MainShellController).
     */
    public void setShell(MainShellController shell) {
        this.shell = shell;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        planService = new PlanService(); // kept for compatibility with existing edit/delete flow
        userService = new UserService();
        planApiService = new PlanApiService();

        // save current content if embedded in a host (optional)
        if (contentHost != null && !contentHost.getChildren().isEmpty()) {
            try {
                plansContent = (Parent) contentHost.getChildren().get(0);
            } catch (Exception ignored) {
                plansContent = null;
            }
        }

        setupStatus();
        setupDeadlineDatePickerConstraints();
        setupTablePipeline();
        setupTable();
        setupSelection();
        setupFormListeners();
        setupExplorerControls(); // optional controls; safe if null
        setupSummaryAutoRefresh();

        updateStatusLabel("Loading plans...");
        hidePlanCrudButtonsForSafeDefault();
        loadInitialDataAsync();
        Platform.runLater(() -> {
            // Put focus on a neutral container so mouse wheel works immediately
            if (plansCardsContainer != null) {
                plansCardsContainer.setFocusTraversable(false);
            }
            if (tvPlans != null) {
                tvPlans.setFocusTraversable(false);
            }

            if (contentHost != null) {
                contentHost.requestFocus();
            } else if (appRoot != null) {
                appRoot.requestFocus();
            }
        });
    }


    /* ================== INIT HELPERS ================== */

    private void setupFormListeners() {
        if (cmbUser != null) {
            cmbUser.valueProperty().addListener((obs, oldU, newU) -> updateHeaderChip(newU));
        }
    }

    private void setupTablePipeline() {
        filteredPlans = new FilteredList<>(plansList, p -> true);

        if (tvPlans != null) {
            tvPlans.setItems(FXCollections.observableArrayList());
        }
    }

    private void setupSummaryAutoRefresh() {
        plansList.addListener((javafx.collections.ListChangeListener<OnboardingPlan>) change -> {
            refreshSummaryCards();
            updateExplorerFooterStatus();
        });

        if (filteredPlans != null) {
            filteredPlans.addListener((javafx.collections.ListChangeListener<OnboardingPlan>) change -> {
                refreshSummaryCards();
                updateExplorerFooterStatus();
                applyCustomSortIfRequested();
            });
        }
    }

    private void setupExplorerControls() {
        // These controls are OPTIONAL in FXML for now; no crash if missing.
        if (cmbPlanFilterStatus != null) {
            cmbPlanFilterStatus.setItems(FXCollections.observableArrayList(
                    "All Statuses", "Pending", "In Progress", "Completed", "On Hold"
            ));
            cmbPlanFilterStatus.setValue("All Statuses");
            cmbPlanFilterStatus.valueProperty().addListener((obs, oldV, newV) -> applyExplorerFilters());
        }

        if (cmbPlanSort != null) {
            cmbPlanSort.setItems(FXCollections.observableArrayList(
                    "Default (Table)",
                    "Deadline (Earliest)",
                    "Deadline (Latest)",
                    "Status (A-Z)",
                    "User (A-Z)",
                    "Plan ID (Newest)"
            ));
            cmbPlanSort.setValue("Default (Table)");
            cmbPlanSort.valueProperty().addListener((obs, oldV, newV) -> applyExplorerFilters());
        }

        if (txtPlanSearch != null) {
            txtPlanSearch.textProperty().addListener((obs, oldV, newV) -> applyExplorerFilters());
        }

        if (chkOverdueOnly != null) {
            chkOverdueOnly.selectedProperty().addListener((obs, oldV, newV) -> applyExplorerFilters());
        }
    }

    private void hidePlanCrudButtonsForSafeDefault() {
        if (btnAdd != null) {
            btnAdd.setDisable(true);
            btnAdd.setVisible(false);
            btnAdd.setManaged(false);
        }

        if (btnUpdate != null) {
            btnUpdate.setDisable(true);
            btnUpdate.setVisible(false);
            btnUpdate.setManaged(false);
        }

        if (btnDelete != null) {
            btnDelete.setDisable(true);
            btnDelete.setVisible(false);
            btnDelete.setManaged(false);
        }
    }



    private void loadInitialDataAsync() {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<User> users = userService.getAllUsers();
                List<OnboardingPlan> plans = fetchPlansFromApi();

                Platform.runLater(() -> {
                    bindUsers(users);
                    plansList.setAll(plans);

                    // if form user empty, preselect first for better UX
                    if (!users.isEmpty() && cmbUser.getValue() == null) {
                        cmbUser.getSelectionModel().selectFirst();
                    }

                    applyExplorerFilters();
                    refreshPlansCardsView();// apply current search/filter/sort if controls exist
                    applyRoleRulesDeferred();
                    refreshSummaryCards();
                    updateStatusLabel("Loaded " + plans.size() + " plans");
                });

                return null;
            }
        };

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            if (ex != null) ex.printStackTrace();

            Platform.runLater(() -> {
                showError("Loading Error", ex == null ? "Unknown error while loading plans." : ex.getMessage());
                updateStatusLabel("Load failed");
            });
        });

        Thread th = new Thread(loadTask, "onboarding-plan-load-thread");
        th.setDaemon(true);
        th.start();
    }

    private void bindUsers(List<User> users) {
        // Always fill the userNameById map (cards/table/search depend on it)
        userNameById.clear();

        if (users != null) {
            for (User u : users) {
                if (u == null) continue;

                String fullName = (safeTrim(u.getFirstName()) + " " + safeTrim(u.getLastName())).trim();
                if (fullName.isEmpty()) {
                    fullName = "User #" + u.getUserId();
                }
                userNameById.put(u.getUserId(), fullName);
            }
        }

        // ComboBox setup is optional (only if form exists in current FXML)
        if (cmbUser == null) return;

        cmbUser.setItems(FXCollections.observableArrayList(users));

        cmbUser.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(User u) {
                if (u == null) return "";
                String full = (safeTrim(u.getFirstName()) + " " + safeTrim(u.getLastName())).trim();
                return full.isEmpty() ? ("User #" + u.getUserId()) : full;
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });
    }

    /* ================== NAV ================== */

    private void openTasksInSameWindow(int planId) {
        if (shell == null) {
            showError("Navigation Error", "Shell is not set. Cannot open tasks.");
            return;
        }
        shell.openTasks(planId);
    }

    @SuppressWarnings("unused")
    private void showPlansContent() {
        if (contentHost == null || plansContent == null) return;

        contentHost.getChildren().setAll(plansContent);

        if (appRoot != null && appRoot.getScene() != null && appRoot.getScene().getWindow() != null) {
            Stage stage = (Stage) appRoot.getScene().getWindow();
            stage.setTitle("Onboarding Management System — Plans");
        }
    }

    /* ================== SETUP ================== */

    private void setupStatus() {
        if (cmbStatus != null) {
            cmbStatus.getItems().setAll("Pending", "In Progress", "Completed", "On Hold");
            cmbStatus.setValue("Pending");
        }
    }

    private void setupSelection() {
        if (tvPlans == null) return;

        tvPlans.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedPlan = newSel;
            if (newSel != null) {
                populateFields(newSel);
                updateStatusLabel("Selected plan #" + newSel.getPlanId());
            }
        });
    }

    private void setupTable() {
        if (colPlanId != null) colPlanId.setCellValueFactory(new PropertyValueFactory<>("planId"));
        if (colDeadline != null) colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));

        if (colUserName != null) {
            // user name from cached map
            colUserName.setCellValueFactory(cellData -> {
                int uid = cellData.getValue().getUserId();
                return new ReadOnlyStringWrapper(getUserDisplayName(uid));
            });
        }

        if (colStatus != null) {
            // status with colored dot
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
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);

                    if (empty || status == null || status.trim().isEmpty()) {
                        setGraphic(null);
                        return;
                    }

                    text.setText(status);
                    applyStatusClasses(dot, text, status);
                    setGraphic(box);
                }
            });
        }

        setupActionsColumn();
    }

    private void applyStatusClasses(Region dot, Label text, String status) {
        dot.getStyleClass().removeAll(
                "status-dot-pending", "status-dot-progress", "status-dot-completed", "status-dot-hold", "status-dot-neutral"
        );
        text.getStyleClass().removeAll(
                "status-text-pending", "status-text-progress", "status-text-completed", "status-text-hold", "status-text-neutral"
        );

        String s = safeTrim(status).toLowerCase(Locale.ROOT);
        switch (s) {
            case "pending" -> {
                dot.getStyleClass().add("status-dot-pending");
                text.getStyleClass().add("status-text-pending");
            }
            case "in progress" -> {
                dot.getStyleClass().add("status-dot-progress");
                text.getStyleClass().add("status-text-progress");
            }
            case "completed" -> {
                dot.getStyleClass().add("status-dot-completed");
                text.getStyleClass().add("status-text-completed");
            }
            case "on hold" -> {
                dot.getStyleClass().add("status-dot-hold");
                text.getStyleClass().add("status-text-hold");
            }
            default -> {
                dot.getStyleClass().add("status-dot-neutral");
                text.getStyleClass().add("status-text-neutral");
            }
        }
    }

    private void setupActionsColumn() {
        if (colActions == null) return;

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Tasks");

            {
                btn.getStyleClass().addAll("btn-primary", "btn-table");

                btn.setOnAction(e -> {
                    OnboardingPlan plan = getCurrentRowPlan();
                    if (plan != null) {
                        openTasksInSameWindow(plan.getPlanId());
                    }
                });

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            private OnboardingPlan getCurrentRowPlan() {
                if (getTableRow() == null) return null;
                Object item = getTableRow().getItem();
                if (item instanceof OnboardingPlan plan) {
                    return plan;
                }
                return null;
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                OnboardingPlan rowPlan = getCurrentRowPlan();
                if (empty || rowPlan == null) {
                    setGraphic(null);
                } else {
                    btn.setDisable(false);
                    setGraphic(btn);
                }
            }
        });
    }

    /* ================== EXPLORER (ADVANCED FEATURE #1) ================== */

    private void applyExplorerFilters() {
        if (filteredPlans == null) return;

        final String keyword = txtPlanSearch == null ? "" : safeTrim(txtPlanSearch.getText()).toLowerCase(Locale.ROOT);
        final String statusFilter = cmbPlanFilterStatus == null ? "All Statuses" : safeTrim(cmbPlanFilterStatus.getValue());
        final boolean overdueOnly = chkOverdueOnly != null && chkOverdueOnly.isSelected();

        filteredPlans.setPredicate(plan -> {
            if (plan == null) return false;

            if (statusFilter != null && !statusFilter.isBlank() && !"All Statuses".equalsIgnoreCase(statusFilter)) {
                String planStatus = safeTrim(plan.getStatus());
                if (!planStatus.equalsIgnoreCase(statusFilter)) {
                    return false;
                }
            }

            if (overdueOnly && !isOverdue(plan)) {
                return false;
            }

            if (keyword == null || keyword.isBlank()) return true;

            String userName = getUserDisplayName(plan.getUserId()).toLowerCase(Locale.ROOT);
            String status = safeTrim(plan.getStatus()).toLowerCase(Locale.ROOT);
            String planId = String.valueOf(plan.getPlanId());

            return userName.contains(keyword) || status.contains(keyword) || planId.contains(keyword);
        });

        applyCustomSortIfRequested();
        refreshPlansCardsView();
        updateExplorerFooterStatus();
        refreshSummaryCards();
    }

    private void applyCustomSortIfRequested() {
        if (tvPlans == null || filteredPlans == null) return;

        String sortMode = cmbPlanSort == null ? "Default (Table)" : safeTrim(cmbPlanSort.getValue());
        if (sortMode == null || sortMode.isBlank()) sortMode = "Default (Table)";

        List<OnboardingPlan> rows = new ArrayList<>(filteredPlans);

        if ("Default (Table)".equalsIgnoreCase(sortMode)) {
            tvPlans.setItems(FXCollections.observableArrayList(rows));
            return;
        }

        Comparator<OnboardingPlan> comparator;
        switch (sortMode) {
            case "Deadline (Earliest)" -> comparator = Comparator.comparing(
                    this::deadlineAsLocalDate,
                    Comparator.nullsLast(LocalDate::compareTo)
            );
            case "Deadline (Latest)" -> comparator = Comparator.comparing(
                    this::deadlineAsLocalDate,
                    Comparator.nullsLast(LocalDate::compareTo)
            ).reversed();
            case "Status (A-Z)" -> comparator = Comparator.comparing(
                    p -> safeTrim(p.getStatus()).toLowerCase(Locale.ROOT)
            );
            case "User (A-Z)" -> comparator = Comparator.comparing(
                    p -> getUserDisplayName(p.getUserId()).toLowerCase(Locale.ROOT)
            );
            case "Plan ID (Newest)" -> comparator = Comparator.comparingInt(OnboardingPlan::getPlanId).reversed();
            default -> comparator = null;
        }

        if (comparator != null) {
            rows.sort(comparator);
        }

        tvPlans.setItems(FXCollections.observableArrayList(rows));
    }

    private void updateExplorerFooterStatus() {
        if (lblStatus == null) return;

        int shown = filteredPlans == null ? plansList.size() : filteredPlans.size();
        int total = plansList.size();

        if (shown == total) {
            lblStatus.setText("Loaded " + total + " plans");
        } else {
            lblStatus.setText("Showing " + shown + " of " + total + " plans");
        }
    }

    @FXML
    private void handleRefreshPlans() {
        reloadPlansOnlyFromApi();
    }

    @FXML
    private void handleResetExplorer() {
        if (txtPlanSearch != null) txtPlanSearch.clear();
        if (cmbPlanFilterStatus != null) cmbPlanFilterStatus.setValue("All Statuses");
        if (cmbPlanSort != null) cmbPlanSort.setValue("Default (Table)");
        if (chkOverdueOnly != null) chkOverdueOnly.setSelected(false);

        applyExplorerFilters();
        updateStatusLabel("Explorer filters reset");
    }

    /* ================== SUMMARY CARDS (ADVANCED FEATURE #2 foundation) ================== */

    private void refreshSummaryCards() {
        // counts from all plans (not filtered) to reflect overall dataset
        int total = plansList.size();
        int completed = 0;
        int inProgress = 0;
        int overdue = 0;

        for (OnboardingPlan p : plansList) {
            String s = safeTrim(p.getStatus()).toLowerCase(Locale.ROOT);
            if ("completed".equals(s)) completed++;
            if ("in progress".equals(s)) inProgress++;
            if (isOverdue(p)) overdue++;
        }

        if (lblMetricTotal != null) lblMetricTotal.setText(String.valueOf(total));
        if (lblMetricCompleted != null) lblMetricCompleted.setText(String.valueOf(completed));
        if (lblMetricInProgress != null) lblMetricInProgress.setText(String.valueOf(inProgress));
        if (lblMetricOverdue != null) lblMetricOverdue.setText(String.valueOf(overdue));
    }

    private boolean isOverdue(OnboardingPlan plan) {
        if (plan == null || plan.getDeadline() == null) return false;

        LocalDate deadline = deadlineAsLocalDate(plan);
        if (deadline == null) return false;

        String status = safeTrim(plan.getStatus()).toLowerCase(Locale.ROOT);
        boolean completed = "completed".equals(status);

        return !completed && deadline.isBefore(LocalDate.now());
    }

    private LocalDate deadlineAsLocalDate(OnboardingPlan p) {
        if (p == null || p.getDeadline() == null) return null;
        try {
            return new java.sql.Date(p.getDeadline().getTime()).toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    private String getUserDisplayName(int userId) {
        String name = userNameById.get(userId);
        if (name == null || name.isBlank()) return "User #" + userId;
        return name;
    }

    /* ================== FORM FILL ================== */

    private void populateFields(OnboardingPlan plan) {
        if (plan == null) return;

        if (cmbUser != null) {
            // select matching user in combo
            for (User u : cmbUser.getItems()) {
                if (u.getUserId() == plan.getUserId()) {
                    cmbUser.setValue(u);
                    break;
                }
            }
        }

        if (cmbStatus != null) cmbStatus.setValue(plan.getStatus());

        if (dpDeadline != null) {
            if (plan.getDeadline() != null) {
                LocalDate localDate = new java.sql.Date(plan.getDeadline().getTime()).toLocalDate();
                dpDeadline.setValue(localDate);
            } else {
                dpDeadline.setValue(null);
            }
        }
    }

    /* ================== CRUD ================== */

    @FXML
    private void handleAddPlan() {
        if (!validateInputs()) return;

        try {
            int userId = cmbUser.getValue().getUserId();
            Date deadline = Date.from(dpDeadline.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());

            OnboardingPlan plan = new OnboardingPlan(0, userId, cmbStatus.getValue(), deadline);

            String createdJson = planApiService.createPlanJson(plan);
            showInfo("Success", "Plan created successfully.");
            System.out.println("CREATE PLAN RESPONSE = " + createdJson);

            reloadPlansOnlyFromApi();   // API-first reload
            handleClear();

        } catch (RuntimeException ex) {
            showError("Not allowed", ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", ex.getMessage());
        }
    }

    @FXML
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

            boolean isCandidate = Utils.UserSession.getInstance().getCurrentUser() != null
                    && Utils.UserSession.getInstance().getCurrentUser().getRoleId() == 1;
            editController.setCandidateMode(isCandidate);

            Scene scene = new Scene(root, 720, 520);
            scene.getStylesheets().add(getClass().getResource("/styles/hirely.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("Update Plan");
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(640);
            stage.setMinHeight(420);

            // Safe owner resolution (works with table mode OR card mode)
            javafx.stage.Window ownerWindow = null;

            if (btnUpdate != null && btnUpdate.getScene() != null) {
                ownerWindow = btnUpdate.getScene().getWindow();
            } else if (appRoot != null && appRoot.getScene() != null) {
                ownerWindow = appRoot.getScene().getWindow();
            } else if (contentHost != null && contentHost.getScene() != null) {
                ownerWindow = contentHost.getScene().getWindow();
            } else if (plansCardsContainer != null && plansCardsContainer.getScene() != null) {
                ownerWindow = plansCardsContainer.getScene().getWindow();
            } else if (tvPlans != null && tvPlans.getScene() != null) {
                ownerWindow = tvPlans.getScene().getWindow();
            }

            if (ownerWindow != null) {
                stage.initOwner(ownerWindow);
            }

            stage.showAndWait();

            // after edit modal closes, reload from API (not local DB) to avoid mismatch
            reloadPlansOnlyFromApi();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Cannot open update window. Check console stack trace.");
        }
    }

    @FXML
    private void handleDeletePlan() {
        if (selectedPlan == null) {
            showWarning("No Selection", "Select a plan first (for Update/Delete).");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this plan?");
        confirm.setHeaderText("Confirm Delete");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // kept as-is for compatibility with your current implementation
                // (we can switch this to API delete in the next PlanApiService step)
                planService.deleteOnboardingPlan(selectedPlan.getPlanId());

                showInfo("Success", "Plan deleted.");
                reloadPlansOnlyFromApi(); // API-first reload keeps UI synced
                handleClear();

            } catch (SQLException e) {
                showError("Database Error", e.getMessage());
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        }
    }

    @FXML
    private void handleClear() {
        if (cmbUser != null) cmbUser.setValue(null);
        if (cmbStatus != null) cmbStatus.setValue("Pending");
        if (dpDeadline != null) dpDeadline.setValue(null);

        selectedPlan = null;
        if (tvPlans != null) {
            tvPlans.getSelectionModel().clearSelection();
        }

        updateStatusLabel("Ready");
        refreshPlansCardsView();
    }

    // keep if FXML still references it
    @FXML
    private void handleManageTasks() {
        showWarning("Info", "Use the Tasks button inside the table row.");
    }

    @FXML
    private void handleSaveStatus() {
        if (selectedPlan == null) {
            showWarning("No Selection", "Select a plan first.");
            return;
        }

        try {
            planApiService.updateStatus(selectedPlan.getPlanId(), cmbStatus.getValue());
            reloadPlansOnlyFromApi();
            showInfo("Saved", "Status updated.");
        } catch (RuntimeException ex) {
            showError("Not allowed", ex.getMessage());
        } catch (Exception ex) {
            showError("Error", ex.getMessage());
        }
    }

    /* ================== DATA RELOAD ================== */

    /**
     * Legacy local DB reload (kept for compatibility / fallback if needed).
     */
    @SuppressWarnings("unused")
    private void reloadPlansOnly() {
        try {
            List<OnboardingPlan> plans = planService.getAllOnboardingPlans();
            plansList.setAll(plans);
            applyExplorerFilters();
            refreshSummaryCards();
            updateStatusLabel("Loaded " + plans.size() + " plans");
        } catch (SQLException e) {
            showError("Error loading plans", e.getMessage());
        }
    }

    private void reloadPlansOnlyFromApi() {
        try {
            List<OnboardingPlan> plans = fetchPlansFromApi();
            plansList.setAll(plans);
            applyExplorerFilters();
            refreshPlansCardsView();
            refreshSummaryCards();
            updateExplorerFooterStatus();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading plans", e.getMessage());
        }
    }

    private List<OnboardingPlan> fetchPlansFromApi() throws Exception {
        String json = planApiService.getPlansJson();
        return parsePlans(json);
    }

    /* ================== HELPERS ================== */

    private boolean validateInputs() {
        if (cmbUser == null || dpDeadline == null) return false;

        if (cmbUser.getValue() == null || dpDeadline.getValue() == null) {
            showWarning("Validation Error", "Select a user and deadline.");
            return false;
        }

        if (dpDeadline.getValue().isBefore(LocalDate.now())) {
            showWarning("Validation Error", "Deadline cannot be in the past.");
            return false;
        }

        return true;
    }

    private void updateHeaderChip(User u) {
        if (u == null) {
            if (lblHeaderUserName != null) lblHeaderUserName.setText("No user selected");
            if (lblHeaderInitials != null) lblHeaderInitials.setText("U");
            return;
        }

        if (lblHeaderUserName != null) {
            String full = (safeTrim(u.getFirstName()) + " " + safeTrim(u.getLastName())).trim();
            lblHeaderUserName.setText(full.isEmpty() ? ("User #" + u.getUserId()) : full);
        }

        String fn = safeTrim(u.getFirstName());
        String ln = safeTrim(u.getLastName());

        String i1 = fn.isEmpty() ? "" : fn.substring(0, 1).toUpperCase(Locale.ROOT);
        String i2 = ln.isEmpty() ? "" : ln.substring(0, 1).toUpperCase(Locale.ROOT);

        String initials = (i1 + i2).trim();
        if (lblHeaderInitials != null) lblHeaderInitials.setText(initials.isEmpty() ? "U" : initials);
    }

    private List<OnboardingPlan> parsePlans(String json) {
        List<OnboardingPlan> list = new ArrayList<>();
        if (json == null) return list;

        String t = json.trim();
        if (t.isEmpty() || t.equals("[]")) return list;

        // remove [ ]
        if (t.startsWith("[")) t = t.substring(1);
        if (t.endsWith("]")) t = t.substring(0, t.length() - 1);

        if (t.trim().isEmpty()) return list;

        // split objects (simple parser kept for compatibility with current project)
        String[] objs = t.split("\\},\\{");
        for (String o : objs) {
            String obj = o;
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";

            OnboardingPlan p = new OnboardingPlan();
            p.setPlanId(extractInt(obj, "planId"));
            p.setUserId(extractInt(obj, "userId"));

            String apiStatus = extractString(obj, "status"); // pending / in_progress / ...
            p.setStatus(fromApiStatus(apiStatus));           // Pending / In Progress / ...

            String deadlineStr = extractString(obj, "deadline"); // "2026-03-01"
            if (deadlineStr != null && !deadlineStr.isBlank()) {
                try {
                    LocalDate ld = LocalDate.parse(deadlineStr);
                    p.setDeadline(java.sql.Date.valueOf(ld));
                } catch (Exception ignored) {
                    // keep null if backend returns an unexpected format
                }
            }

            list.add(p);
        }

        return list;
    }

    private int extractInt(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return 0;

        int start = i + needle.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;

        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    private String extractString(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int i = json.indexOf(needle);
        if (i < 0) return "";

        int start = i + needle.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";

        return json.substring(start, end);
    }

    private String fromApiStatus(String apiStatus) {
        String s = apiStatus == null ? "" : apiStatus.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "in_progress" -> "In Progress";
            case "completed" -> "Completed";
            case "on_hold" -> "On Hold";
            default -> "Pending";
        };
    }

    private void applyRoleRules() {
        User current = Utils.UserSession.getInstance().getCurrentUser();
        if (current == null) {
            // Safe default: hide restricted actions until role is known
            hidePlanCrudButtonsForSafeDefault();

            // Optional: also prevent editing sensitive fields while session is unknown
            if (cmbUser != null) cmbUser.setDisable(true);
            if (dpDeadline != null) dpDeadline.setDisable(true);

            return;
        }

        int roleId = current.getRoleId(); // 1=candidate, 2=recruiter, 3=admin
        boolean isCandidate = (roleId == 1);

        if (isCandidate) {
            // Candidate can only change plan status
            if (cmbUser != null) cmbUser.setDisable(true);
            if (dpDeadline != null) dpDeadline.setDisable(true);

            if (btnAdd != null) {
                btnAdd.setDisable(true);
                btnAdd.setVisible(false);
                btnAdd.setManaged(false);
            }

            if (btnDelete != null) {
                btnDelete.setDisable(true);
                btnDelete.setVisible(false);
                btnDelete.setManaged(false);
            }

            // keep update visible (opens edit modal in candidate mode)
            if (btnUpdate != null) {
                btnUpdate.setDisable(true);
                btnUpdate.setVisible(false);
                btnUpdate.setManaged(false);
            }

            if (btnSaveStatus != null) {
                btnSaveStatus.setDisable(false);
                btnSaveStatus.setVisible(true);
                btnSaveStatus.setManaged(true);
            }

        } else {
            // admin / recruiter
            if (cmbUser != null) cmbUser.setDisable(false);
            if (dpDeadline != null) dpDeadline.setDisable(false);

            if (btnAdd != null) {
                btnAdd.setDisable(false);
                btnAdd.setVisible(true);
                btnAdd.setManaged(true);
            }

            if (btnUpdate != null) {
                btnUpdate.setDisable(false);
                btnUpdate.setVisible(true);
                btnUpdate.setManaged(true);
            }

            if (btnDelete != null) {
                btnDelete.setDisable(false);
                btnDelete.setVisible(true);
                btnDelete.setManaged(true);
            }

            if (btnSaveStatus != null) {
                btnSaveStatus.setDisable(false);
                btnSaveStatus.setVisible(true);
                btnSaveStatus.setManaged(true);
            }
        }
    }

    private void updateStatusLabel(String msg) {
        if (lblStatus != null) lblStatus.setText(msg);
    }
    private void setupDeadlineDatePickerConstraints() {
        if (dpDeadline == null) return;

        dpDeadline.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setDisable(false);
                    return;
                }

                // Disable past dates (today is allowed)
                boolean isPast = item.isBefore(LocalDate.now());
                setDisable(isPast);

                // Optional tooltip for disabled dates
                if (isPast) {
                    setTooltip(new Tooltip("Past dates are not allowed"));
                } else {
                    setTooltip(null);
                }
            }
        });
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private void showInfo(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    private void showError(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    private void showWarning(String title, String content) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
    private void applyRoleRulesDeferred() {
        // Run once after UI is attached (helps when session is set slightly after initialize)
        Platform.runLater(this::applyRoleRules);

        // Run again a bit later to be safe in real app startup flows
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {}
            Platform.runLater(this::applyRoleRules);
        }, "plan-role-rules-deferred");
        t.setDaemon(true);
        t.start();
    }
    private void refreshPlansCardsView() {
        if (plansCardsContainer == null) return;

        plansCardsContainer.getChildren().clear();

        List<OnboardingPlan> rows = getCurrentVisiblePlans();
        if (rows.isEmpty()) {
            Label empty = new Label("No plans found.");
            empty.getStyleClass().add("muted-text");

            VBox emptyCard = new VBox(empty);
            emptyCard.setSpacing(6);
            emptyCard.setPadding(new Insets(14));
            emptyCard.getStyleClass().add("card");

            plansCardsContainer.getChildren().add(emptyCard);
            return;
        }

        for (OnboardingPlan plan : rows) {
            plansCardsContainer.getChildren().add(buildPlanCard(plan));
        }
    }

    private List<OnboardingPlan> getCurrentVisiblePlans() {
        if (filteredPlans == null) return new ArrayList<>(plansList);

        List<OnboardingPlan> rows = new ArrayList<>(filteredPlans);

        String sortMode = cmbPlanSort == null ? "Default (Table)" : safeTrim(cmbPlanSort.getValue());
        if (sortMode == null || sortMode.isBlank()) sortMode = "Default (Table)";

        Comparator<OnboardingPlan> comparator = null;

        switch (sortMode) {
            case "Deadline (Earliest)" -> comparator = Comparator.comparing(
                    this::deadlineAsLocalDate,
                    Comparator.nullsLast(LocalDate::compareTo)
            );
            case "Deadline (Latest)" -> comparator = Comparator.comparing(
                    this::deadlineAsLocalDate,
                    Comparator.nullsLast(LocalDate::compareTo)
            ).reversed();
            case "Status (A-Z)" -> comparator = Comparator.comparing(
                    p -> safeTrim(p.getStatus()).toLowerCase(Locale.ROOT)
            );
            case "User (A-Z)" -> comparator = Comparator.comparing(
                    p -> getUserDisplayName(p.getUserId()).toLowerCase(Locale.ROOT)
            );
            case "Plan ID (Newest)" -> comparator = Comparator.comparingInt(OnboardingPlan::getPlanId).reversed();
            case "Default (Table)" -> comparator = Comparator.comparingInt(OnboardingPlan::getPlanId); // stable default
        }

        if (comparator != null) {
            rows.sort(comparator);
        }

        return rows;
    }

    private VBox buildPlanCard(OnboardingPlan plan) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "plan-item-card");
        card.setPadding(new Insets(14));
        card.setFillWidth(true);


        boolean isSelected = selectedPlan != null && selectedPlan.getPlanId() == plan.getPlanId();
        if (isSelected) {
            card.setStyle(
                    "-fx-border-color: #5B2B97; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 18; " +
                            "-fx-background-radius: 18;"
            );
        } else {
            card.setStyle(""); // let CSS handle normal style
        }

        /* ================= HEADER ================= */
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Plan #" + plan.getPlanId());
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnTasks = new Button("Tasks");
        btnTasks.getStyleClass().add("btn-primary");
        btnTasks.setOnAction(e -> {
            e.consume(); // prevent card click double-trigger
            openTasksInSameWindow(plan.getPlanId());
        });

        ImageView qrView = new ImageView();
        qrView.setFitWidth(92);
        qrView.setFitHeight(92);
        qrView.setPreserveRatio(true);
        qrView.setSmooth(true);
        qrView.setOnMouseClicked(e -> {
            if (qrView.getImage() == null) return;

            ImageView big = new ImageView(qrView.getImage());
            big.setPreserveRatio(true);
            big.setFitWidth(420);

            VBox box = new VBox(12, big);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(14));

            Stage s = new Stage();
            s.setTitle("Plan QR - Plan #" + plan.getPlanId());
            s.setScene(new Scene(box));
            s.initModality(Modality.WINDOW_MODAL);

            if (plansCardsContainer != null && plansCardsContainer.getScene() != null) {
                s.initOwner(plansCardsContainer.getScene().getWindow());
            }
            s.showAndWait();
        });

// nice border-ish look without CSS changes
        qrView.setStyle("-fx-background-color: white; -fx-padding: 6; -fx-border-color: #EEF0F7; -fx-border-radius: 10; -fx-background-radius: 10;");

// If cached, use it immediately
        Image cached = qrCache.get(plan.getPlanId());
        if (cached != null) {
            qrView.setImage(cached);
        } else {
            // load in background so UI doesn't freeze
            Task<Image> loadQr = new Task<>() {
                @Override
                protected Image call() throws Exception {
                    byte[] bytes = planApiService.getPlanQrPng(plan.getPlanId());
                    return new Image(new ByteArrayInputStream(bytes));
                }
            };

            loadQr.setOnSucceeded(ev -> {
                Image img = loadQr.getValue();
                if (img != null) {
                    qrCache.put(plan.getPlanId(), img);
                    qrView.setImage(img);
                }
            });

            loadQr.setOnFailed(ev -> {
                // optional: show nothing; you can log if you want
                // System.out.println("QR load failed for plan " + plan.getPlanId() + ": " + loadQr.getException());
            });

            Thread th = new Thread(loadQr, "plan-qr-" + plan.getPlanId());
            th.setDaemon(true);
            th.start();
        }
        header.getChildren().addAll(title, spacer, btnTasks, qrView);

        /* ================= BODY ================= */
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(18);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(32);
        c2.setHgrow(Priority.ALWAYS);

        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(18);

        ColumnConstraints c4 = new ColumnConstraints();
        c4.setPercentWidth(32);
        c4.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().setAll(c1, c2, c3, c4);

        // Keys
        Label lblUserKey = new Label("User");
        lblUserKey.getStyleClass().add("form-label");

        Label lblStatusKey = new Label("Status");
        lblStatusKey.getStyleClass().add("form-label");

        Label lblDeadlineKey = new Label("Deadline");
        lblDeadlineKey.getStyleClass().add("form-label");

        Label lblStateKey = new Label("State");
        lblStateKey.getStyleClass().add("form-label");

        // Values (robust fallback values)
        String userDisplay = getUserDisplayName(plan.getUserId());
        if (userDisplay == null || userDisplay.isBlank()) {
            userDisplay = "User #" + plan.getUserId();
        }

        LocalDate deadlineDate = deadlineAsLocalDate(plan);
        String deadlineText = (deadlineDate == null) ? "—" : deadlineDate.toString();

        Label lblUserVal = new Label(userDisplay == null || userDisplay.isBlank() ? ("User #" + plan.getUserId()) : userDisplay);
        lblUserVal.setWrapText(true);
        lblUserVal.setMaxWidth(Double.MAX_VALUE);
        lblUserVal.setStyle("-fx-text-fill: #1F2A44; -fx-font-weight: 600;");


        HBox statusBox = buildStatusNode(plan.getStatus());

        Label lblDeadlineVal = new Label(deadlineText == null || deadlineText.isBlank() ? "—" : deadlineText);
        lblDeadlineVal.setMaxWidth(Double.MAX_VALUE);
        lblDeadlineVal.setStyle("-fx-text-fill: #1F2A44; -fx-font-weight: 600;");

        boolean overdue = isOverdue(plan);
        Label lblStateVal = new Label(overdue ? "Overdue" : "On Track");
        lblStateVal.setStyle(
                overdue
                        ? "-fx-text-fill: #E84855; -fx-font-weight: 800;"
                        : "-fx-text-fill: #1E9B63; -fx-font-weight: 800;"
        );

        // Add nodes to grid
        grid.add(lblUserKey,     0, 0);
        grid.add(lblUserVal,     1, 0);
        grid.add(lblStatusKey,   2, 0);
        grid.add(statusBox,      3, 0);

        grid.add(lblDeadlineKey, 0, 1);
        grid.add(lblDeadlineVal, 1, 1);
        grid.add(lblStateKey,    2, 1);
        grid.add(lblStateVal,    3, 1);

        GridPane.setHgrow(lblUserVal, Priority.ALWAYS);
        GridPane.setHgrow(lblDeadlineVal, Priority.ALWAYS);
        GridPane.setHgrow(statusBox, Priority.ALWAYS);

        /* ================= CARD CLICK = SELECT ================= */
        card.setOnMouseClicked(e -> {
            selectedPlan = plan;
            populateFields(plan);
            updateStatusLabel("Selected plan #" + plan.getPlanId());
            refreshPlansCardsView();
        });

        card.setFocusTraversable(false);
        btnTasks.setFocusTraversable(false);
        card.getChildren().addAll(header, grid);
        return card;
    }

    private HBox buildStatusNode(String status) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("status-container");

        Region dot = new Region();
        dot.getStyleClass().add("status-dot");

        Label text = new Label(status == null || status.isBlank() ? "Unknown" : status);
        text.getStyleClass().add("status-text");

        applyStatusClasses(dot, text, text.getText());

        box.getChildren().addAll(dot, text);
        return box;

    }


}