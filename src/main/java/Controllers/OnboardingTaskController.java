package Controllers;

import Models.OnboardingTask;
<<<<<<< HEAD
import Models.TaskRecommendation;
import Services.TaskDecisionGuideService;
import Services.api.TaskApiService;
import Utils.UserSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
=======
import Services.PlanService;
import Services.TaskService;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
<<<<<<< HEAD
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class OnboardingTaskController implements Initializable {

    /* ================= FORM ================= */
=======
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class OnboardingTaskController implements Initializable {

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    @FXML private TextField txtPlanId;
    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbStatus;

<<<<<<< HEAD
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;

    @FXML private TextField txtFilepath;
    @FXML private Button btnBrowse;

    /* ================= CARDS UI ================= */
    @FXML private ScrollPane tasksScrollPane;
    @FXML private VBox tasksCardsContainer;

    /* ================= OPTIONAL TASK EXPLORER CONTROLS ================= */
    @FXML private TextField txtTaskSearch;
    @FXML private ComboBox<String> cmbTaskFilterStatus;
    @FXML private ComboBox<String> cmbTaskSort;
    @FXML private CheckBox chkHasAttachmentOnly;

    /* ================= OPTIONAL TASK SUMMARY LABELS ================= */
    @FXML private Label lblTaskMetricTotal;
    @FXML private Label lblTaskMetricCompleted;
    @FXML private Label lblTaskMetricInProgress;
    @FXML private Label lblTaskMetricBlocked;

    /* ================= HIDDEN TABLE FALLBACK (compatibility) ================= */
=======
    // keep them in UI, but disabled (file only on update)
    @FXML private TextField txtFilepath;
    @FXML private Button btnBrowse;

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    @FXML private TableView<OnboardingTask> tvTasks;
    @FXML private TableColumn<OnboardingTask, Integer> colTaskId;
    @FXML private TableColumn<OnboardingTask, Integer> colPlanId;
    @FXML private TableColumn<OnboardingTask, String> colTitle;
    @FXML private TableColumn<OnboardingTask, String> colDescription;
    @FXML private TableColumn<OnboardingTask, String> colStatus;
    @FXML private TableColumn<OnboardingTask, String> colFilepath;

    @FXML private Label lblStatus;

<<<<<<< HEAD
    // Role-Based Decision Guide UI
    @FXML private VBox recommendationsContainer;
    @FXML private Label lblDecisionGuideRole;

    // Local decision engine (no API required)
    private final TaskDecisionGuideService decisionGuideService = new TaskDecisionGuideService();

    private final TaskApiService taskApiService = new TaskApiService();
    private final ObjectMapper om = new ObjectMapper();

    private final ObservableList<OnboardingTask> taskList = FXCollections.observableArrayList();
    private FilteredList<OnboardingTask> filteredTasks;

    private OnboardingTask selectedTask;
    private Integer currentPlanId = null;

    private Runnable onBack;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        /* ===== Hidden table (kept for safe compatibility) ===== */
        if (colTaskId != null) colTaskId.setCellValueFactory(new PropertyValueFactory<>("taskId"));
        if (colPlanId != null) colPlanId.setCellValueFactory(new PropertyValueFactory<>("planId"));
        if (colTitle != null) colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (colDescription != null) colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        if (colStatus != null) {
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colStatus.setCellFactory(column -> new TableCell<OnboardingTask, String>() {
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

                    text.setText(normalizeStatus(status));
                    applyTaskStatusClasses(dot, text, status);
                    setGraphic(box);
                }
            });
        }

        if (colFilepath != null) colFilepath.setCellValueFactory(new PropertyValueFactory<>("filepath"));

        /* ===== Main status combo ===== */
        if (cmbStatus != null) {
            cmbStatus.getItems().setAll("Not Started", "In Progress", "Completed", "Blocked", "On Hold");
            cmbStatus.setValue("Not Started");
        }

        /* ===== Main screen: file only in update popup ===== */
        if (txtFilepath != null) txtFilepath.setDisable(true);
        if (btnBrowse != null) btnBrowse.setDisable(true);

        /* ===== Hidden table selection sync (safe fallback) ===== */
        if (tvTasks != null) {
            tvTasks.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                selectedTask = newSel;
                if (newSel != null) {
                    populateFields(newSel);
                    updateStatusLabel("Selected task #" + newSel.getTaskId());
                    refreshTasksCardsView();
                }
            });
        }

        setupTaskPipeline();
        setupTaskExplorerControls();
        setupTaskSummaryAutoRefresh();
        refreshDecisionGuide();
        applyRoleRules();

        updateStatusLabel("Select a plan to load tasks.");
    }

    /* ================= NAV ================= */

=======
    private TaskService taskService;
    private PlanService planService;
    private ObservableList<OnboardingTask> taskList;
    private OnboardingTask selectedTask;
    private Integer currentPlanId = null;


    @Override
    /**
     * Initializes UI components and loads initial data.
     */
    public void initialize(URL location, ResourceBundle resources) {

        taskService = new TaskService();
        planService = new PlanService();
        taskList = FXCollections.observableArrayList();

        colTaskId.setCellValueFactory(new PropertyValueFactory<>("taskId"));
        colPlanId.setCellValueFactory(new PropertyValueFactory<>("planId"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        // ✅ STATUS as a colored "pill" (Task statuses)
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<OnboardingTask, String>() {

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
                        "status-dot-pending","status-dot-progress","status-dot-completed","status-dot-hold","status-dot-blocked","status-dot-neutral"
                );
                text.getStyleClass().removeAll(
                        "status-text-pending","status-text-progress","status-text-completed","status-text-hold","status-text-blocked","status-text-neutral"
                );

                String s = status.trim().toLowerCase();

                if (s.equals("not started")) {
                    dot.getStyleClass().add("status-dot-hold");
                    text.getStyleClass().add("status-text-hold");
                } else if (s.equals("in progress")) {
                    dot.getStyleClass().add("status-dot-progress");
                    text.getStyleClass().add("status-text-progress");
                } else if (s.equals("completed")) {
                    dot.getStyleClass().add("status-dot-completed");
                    text.getStyleClass().add("status-text-completed");
                } else if (s.equals("blocked")) {
                    dot.getStyleClass().add("status-dot-blocked");
                    text.getStyleClass().add("status-text-blocked");
                } else if (s.equals("on hold")) {
                    dot.getStyleClass().add("status-dot-hold");
                    text.getStyleClass().add("status-text-hold");
                } else {
                    dot.getStyleClass().add("status-dot-neutral");
                    text.getStyleClass().add("status-text-neutral");
                }
            }
        });


        colFilepath.setCellValueFactory(new PropertyValueFactory<>("filepath"));

        cmbStatus.getItems().addAll("Not Started", "In Progress", "Completed", "Blocked", "On Hold");
        cmbStatus.setValue("Not Started");

        // disable filepath in main screen (file only in update popup)
        txtFilepath.setDisable(true);
        if (btnBrowse != null) btnBrowse.setDisable(true);

        loadTasksForPlan(); // will show all if no plan context was set yet


        tvTasks.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedTask = newSel;
                populateFields(newSel);
            }
        });
    }

    private Runnable onBack;

    /**
     * Sets the onback value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
<<<<<<< HEAD
=======
    /**
     * Handles the associated UI event.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private void handleBack() {
        if (onBack != null) onBack.run();
    }

<<<<<<< HEAD
    /* ================= PLAN CONTEXT ================= */

    public void setPlanContext(int planId) {
        this.currentPlanId = planId;

        if (txtPlanId != null) {
            txtPlanId.setText(String.valueOf(planId));
            txtPlanId.setDisable(true);
        }

        loadTasksForPlan();
    }

    private void loadTasksForPlan() {
        try {
            if (currentPlanId == null) {
                taskList.clear();
                if (tvTasks != null) tvTasks.setItems(taskList);
                applyTaskExplorerFilters(); // refresh cards/summary/footer safely
                updateStatusLabel("No plan selected.");
                refreshDecisionGuide();
                return;
            }

            String json = taskApiService.getTasksJson(currentPlanId);
            List<OnboardingTask> tasks = om.readValue(json, new TypeReference<List<OnboardingTask>>() {});
            taskList.setAll(tasks);
            refreshDecisionGuide();

            // hidden fallback
            if (tvTasks != null) tvTasks.setItems(taskList);

            // explorer pipeline drives cards + footer + summary
            applyTaskExplorerFilters();

        } catch (Exception e) {
            e.printStackTrace();
=======
    /**
     * Loads and refreshes data displayed in the view.
     */
    private void loadTasksForPlan() {
        try {
            if (currentPlanId == null) {
                taskList.setAll(taskService.getAllOnboardingTasks()); // fallback if opened alone
            } else {
                taskList.setAll(taskService.getTasksByPlanId(currentPlanId));
            }
            tvTasks.setItems(taskList);
            lblStatus.setText("Loaded " + taskList.size() + " tasks");
        } catch (SQLException e) {
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
            showError("Error", e.getMessage());
        }
    }

<<<<<<< HEAD
    /* ================= EXPLORER / FILTER / SORT ================= */

    private void setupTaskPipeline() {
        filteredTasks = new FilteredList<>(taskList, t -> true);

        if (tvTasks != null) {
            tvTasks.setItems(FXCollections.observableArrayList());
        }
    }

    private void setupTaskExplorerControls() {
        if (cmbTaskFilterStatus != null) {
            cmbTaskFilterStatus.setItems(FXCollections.observableArrayList(
                    "All Statuses",
                    "Not Started",
                    "In Progress",
                    "Completed",
                    "Blocked",
                    "On Hold"
            ));
            cmbTaskFilterStatus.setValue("All Statuses");
            cmbTaskFilterStatus.valueProperty().addListener((obs, o, n) -> applyTaskExplorerFilters());
        }

        if (cmbTaskSort != null) {
            cmbTaskSort.setItems(FXCollections.observableArrayList(
                    "Default (Task ID)",
                    "Task ID (Newest)",
                    "Title (A-Z)",
                    "Status (A-Z)"
            ));
            cmbTaskSort.setValue("Default (Task ID)");
            cmbTaskSort.valueProperty().addListener((obs, o, n) -> applyTaskExplorerFilters());
        }

        if (txtTaskSearch != null) {
            txtTaskSearch.textProperty().addListener((obs, o, n) -> applyTaskExplorerFilters());
        }

        if (chkHasAttachmentOnly != null) {
            chkHasAttachmentOnly.selectedProperty().addListener((obs, o, n) -> applyTaskExplorerFilters());
        }
    }

    private void setupTaskSummaryAutoRefresh() {
        taskList.addListener((javafx.collections.ListChangeListener<OnboardingTask>) c -> {
            refreshTaskSummaryCards();
            updateTaskExplorerFooterStatus();
        });

        if (filteredTasks != null) {
            filteredTasks.addListener((javafx.collections.ListChangeListener<OnboardingTask>) c -> {
                updateTaskExplorerFooterStatus();
            });
        }
    }

    private void applyTaskExplorerFilters() {
        if (filteredTasks == null) return;

        final String keyword = txtTaskSearch == null ? "" : safe(txtTaskSearch.getText()).toLowerCase(Locale.ROOT);
        final String statusFilter = cmbTaskFilterStatus == null ? "All Statuses" : safe(cmbTaskFilterStatus.getValue());
        final boolean attachmentOnly = chkHasAttachmentOnly != null && chkHasAttachmentOnly.isSelected();

        filteredTasks.setPredicate(task -> {
            if (task == null) return false;

            // status filter
            if (statusFilter != null && !statusFilter.isBlank() && !"All Statuses".equalsIgnoreCase(statusFilter)) {
                String taskStatus = normalizeStatus(task.getStatus());
                if (!taskStatus.equalsIgnoreCase(statusFilter)) {
                    return false;
                }
            }

            // attachment filter (strict: only web attachments)
            if (attachmentOnly && !hasWebAttachment(task)) {
                return false;
            }

            // keyword search
            if (keyword == null || keyword.isBlank()) return true;

            String title = safe(task.getTitle()).toLowerCase(Locale.ROOT);
            String description = safe(task.getDescription()).toLowerCase(Locale.ROOT);
            String status = normalizeStatus(task.getStatus()).toLowerCase(Locale.ROOT);
            String taskId = String.valueOf(task.getTaskId());
            String planId = String.valueOf(task.getPlanId());

            return title.contains(keyword)
                    || description.contains(keyword)
                    || status.contains(keyword)
                    || taskId.contains(keyword)
                    || planId.contains(keyword);
        });

        // keep hidden table in sync (optional)
        applyTaskCustomSortIfRequested();

        refreshTasksCardsView();
        refreshTaskSummaryCards();
        updateTaskExplorerFooterStatus();
    }

    private void applyTaskCustomSortIfRequested() {
        if (filteredTasks == null) return;

        List<OnboardingTask> rows = getCurrentVisibleTasks();
        if (tvTasks != null) {
            tvTasks.setItems(FXCollections.observableArrayList(rows));
        }
    }

    @FXML
    private void handleResetTaskExplorer() {
        if (txtTaskSearch != null) txtTaskSearch.clear();
        if (cmbTaskFilterStatus != null) cmbTaskFilterStatus.setValue("All Statuses");
        if (cmbTaskSort != null) cmbTaskSort.setValue("Default (Task ID)");
        if (chkHasAttachmentOnly != null) chkHasAttachmentOnly.setSelected(false);

        applyTaskExplorerFilters();
        updateStatusLabel("Task explorer filters reset");
    }

    private List<OnboardingTask> getCurrentVisibleTasks() {
        List<OnboardingTask> rows = (filteredTasks != null)
                ? new ArrayList<>(filteredTasks)
                : new ArrayList<>(taskList);

        String sortMode = cmbTaskSort == null ? "Default (Task ID)" : safe(cmbTaskSort.getValue());
        if (sortMode == null || sortMode.isBlank()) sortMode = "Default (Task ID)";

        Comparator<OnboardingTask> comparator = switch (sortMode) {
            case "Task ID (Newest)" -> Comparator.comparingInt(OnboardingTask::getTaskId).reversed();
            case "Title (A-Z)" -> Comparator.comparing(t -> safe(t.getTitle()).toLowerCase(Locale.ROOT));
            case "Status (A-Z)" -> Comparator.comparing(t -> normalizeStatus(t.getStatus()).toLowerCase(Locale.ROOT));
            default -> Comparator.comparingInt(OnboardingTask::getTaskId);
        };

        rows.sort(comparator);
        return rows;
    }

    /* ================= SUMMARY ================= */

    private void refreshTaskSummaryCards() {
        int total = taskList.size();
        int completed = 0;
        int inProgress = 0;
        int blocked = 0;

        for (OnboardingTask t : taskList) {
            String s = normalizeStatus(t.getStatus()).toLowerCase(Locale.ROOT);

            if ("completed".equals(s)) completed++;
            if ("in progress".equals(s)) inProgress++;
            if ("blocked".equals(s)) blocked++;
        }

        if (lblTaskMetricTotal != null) lblTaskMetricTotal.setText(String.valueOf(total));
        if (lblTaskMetricCompleted != null) lblTaskMetricCompleted.setText(String.valueOf(completed));
        if (lblTaskMetricInProgress != null) lblTaskMetricInProgress.setText(String.valueOf(inProgress));
        if (lblTaskMetricBlocked != null) lblTaskMetricBlocked.setText(String.valueOf(blocked));
    }

    private void updateTaskExplorerFooterStatus() {
        if (lblStatus == null) return;

        int total = taskList.size();
        int shown = filteredTasks == null ? total : filteredTasks.size();

        if (shown == total) {
            lblStatus.setText("Loaded " + total + " tasks");
        } else {
            lblStatus.setText("Showing " + shown + " of " + total + " tasks");
        }
    }

    /* ================= FORM / SELECTION ================= */

    private void populateFields(OnboardingTask task) {
        if (task == null) return;

        if (txtPlanId != null) txtPlanId.setText(String.valueOf(task.getPlanId()));
        if (txtTitle != null) txtTitle.setText(task.getTitle());
        if (txtDescription != null) txtDescription.setText(task.getDescription());
        if (cmbStatus != null) cmbStatus.setValue(normalizeStatus(task.getStatus()));
        if (txtFilepath != null) txtFilepath.setText(task.getFilepath());
    }

    private void clearFields() {
        if (txtTitle != null) txtTitle.clear();
        if (txtDescription != null) txtDescription.clear();
        if (txtFilepath != null) txtFilepath.clear();
        if (cmbStatus != null) cmbStatus.setValue("Not Started");

        selectedTask = null;

        if (tvTasks != null) tvTasks.getSelectionModel().clearSelection();

        refreshTasksCardsView();
        updateStatusLabel("Ready");
        refreshDecisionGuide();
    }

    /* ================= CRUD ================= */

    @FXML
    private void handleAddTask() {
        try {
=======

    /**
     * Executes this operation.
     */
    private void populateFields(OnboardingTask task) {
        txtPlanId.setText(String.valueOf(task.getPlanId()));
        txtTitle.setText(task.getTitle());
        txtDescription.setText(task.getDescription());
        cmbStatus.setValue(task.getStatus());

        // show it but disabled
        txtFilepath.setText(task.getFilepath());
    }
    /**
     * Sets the plancontext value.
     */
    public void setPlanContext(int planId) {
        this.currentPlanId = planId;

        // lock the planId field so user can't type another plan id
        txtPlanId.setText(String.valueOf(planId));
        txtPlanId.setDisable(true);

        // load only tasks for this plan
        loadTasksForPlan();
    }


    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleAddTask() {
        try {
            if (txtPlanId.getText().trim().isEmpty() || txtTitle.getText().trim().isEmpty()) {
                showError("Validation", "Plan ID and Title are required.");
                return;
            }

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
            if (currentPlanId == null) {
                showError("Error", "This task window is not linked to a plan.");
                return;
            }
<<<<<<< HEAD

            if (txtTitle == null || txtTitle.getText() == null || txtTitle.getText().trim().isEmpty()) {
                showError("Validation", "Title is required.");
                return;
            }

            OnboardingTask task = new OnboardingTask(
                    0,
                    currentPlanId,
                    txtTitle.getText().trim(),
                    txtDescription == null ? null : txtDescription.getText(),
                    cmbStatus == null ? "Not Started" : cmbStatus.getValue(),
                    null
            );

            taskApiService.createTaskJson(currentPlanId, task);
            showInfo("Success", "Task added.");
            loadTasksForPlan();
            clearFields();

        } catch (RuntimeException ex) {
            showError("Not allowed", ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
=======
            int planId = currentPlanId;


            if (planService.getOnboardingPlanById(planId) == null) {
                showError("Error", "Plan ID does not exist.");
                return;
            }

            // IMPORTANT: filepath is not set at creation time
            OnboardingTask task = new OnboardingTask(
                    0,
                    planId,
                    txtTitle.getText(),
                    txtDescription.getText(),
                    cmbStatus.getValue(),
                    null
            );

            taskService.addOnboardingTask(task);

            showInfo("Success", "Task added (no file yet).");
            loadTasksForPlan(); // will show all if no plan context was set yet
            clearFields();

        } catch (NumberFormatException e) {
            showError("Validation", "Plan ID must be a number.");
        } catch (Exception e) {
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
            showError("Error", e.getMessage());
        }
    }

    @FXML
<<<<<<< HEAD
    private void handleUpdateTask() {
=======
    /**
     * Handles the associated UI event.
     */
    private void handleUpdateTask() {

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        if (selectedTask == null) {
            showError("No Selection", "Select a task first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/OnboardingTaskEditView.fxml"));
            Parent root = loader.load();

            OnboardingTaskEditController editController = loader.getController();
<<<<<<< HEAD

            boolean isCandidate = Utils.UserSession.getInstance().getCurrentUser() != null
                    && Utils.UserSession.getInstance().getCurrentUser().getRoleId() == 1;

            // IMPORTANT: set mode BEFORE setTask
            editController.setCandidateMode(isCandidate);
            editController.setTask(selectedTask);

            Stage stage = new Stage();
            stage.setTitle("Update Task");

            Scene scene = new Scene(root, 720, 420);
            scene.getStylesheets().add(getClass().getResource("/styles/hirely.css").toExternalForm());
            stage.setScene(scene);

            Window owner = resolveModalOwner();
            if (owner != null) {
                stage.initOwner(owner);
            }

            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(true);
=======
            editController.setTask(selectedTask);

            Stage stage = new Stage();
            stage.setTitle("Update Task / Upload File");

// ✅ set an initial size for the popup
            Scene scene = new Scene(root, 720, 420);
            scene.getStylesheets().add(getClass().getResource("/styles/hirely.css").toExternalForm());

            stage.setScene(scene);
            stage.initOwner(tvTasks.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);

// ✅ allow resizing
            stage.setResizable(true);

// ✅ optional: min sizes
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
            stage.setMinWidth(650);
            stage.setMinHeight(380);

            stage.showAndWait();

<<<<<<< HEAD
            loadTasksForPlan();
=======

            loadTasksForPlan(); // will show all if no plan context was set yet

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
            clearFields();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Cannot open update window. Check console.");
        }
    }

    @FXML
<<<<<<< HEAD
=======
    /**
     * Handles the associated UI event.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private void handleDeleteTask() {
        if (selectedTask == null) return;

        Optional<ButtonType> result = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete this task?"
        ).showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
<<<<<<< HEAD
                taskApiService.deleteTask(selectedTask.getTaskId());
                loadTasksForPlan();
                clearFields();
            } catch (RuntimeException ex) {
                showError("Not allowed", ex.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
=======
                taskService.deleteOnboardingTask(selectedTask.getTaskId());
                loadTasksForPlan(); // will show all if no plan context was set yet
                clearFields();
            } catch (SQLException e) {
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
                showError("Error", e.getMessage());
            }
        }
    }

<<<<<<< HEAD
    /* ================= ROLE RULES ================= */

    private void applyRoleRules() {
        int roleId = Utils.UserSession.getInstance().getCurrentUser().getRoleId();
        boolean isCandidate = (roleId == 1);

        // Candidate: no Add/Delete
        if (btnAdd != null) { btnAdd.setVisible(!isCandidate); btnAdd.setManaged(!isCandidate); }
        if (btnDelete != null) { btnDelete.setVisible(!isCandidate); btnDelete.setManaged(!isCandidate); }

        // Update visible for everyone
        if (btnUpdate != null) { btnUpdate.setVisible(true); btnUpdate.setManaged(true); }

        // Main form: candidate should not edit here (use popup)
        if (txtTitle != null) txtTitle.setDisable(isCandidate);
        if (txtDescription != null) txtDescription.setDisable(isCandidate);
        if (cmbStatus != null) cmbStatus.setDisable(isCandidate);
    }

    /* ================= CARDS UI ================= */

    private void refreshTasksCardsView() {
        if (tasksCardsContainer == null) return;

        tasksCardsContainer.getChildren().clear();

        List<OnboardingTask> rows = getCurrentVisibleTasks();

        if (rows.isEmpty()) {
            Label empty = new Label("No tasks found.");
            empty.getStyleClass().add("muted-text");

            VBox emptyCard = new VBox(empty);
            emptyCard.setSpacing(6);
            emptyCard.setPadding(new Insets(14));
            emptyCard.getStyleClass().add("card");

            tasksCardsContainer.getChildren().add(emptyCard);
            return;
        }

        for (OnboardingTask task : rows) {
            tasksCardsContainer.getChildren().add(buildTaskCard(task));
        }
    }

    private VBox buildTaskCard(OnboardingTask task) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "plan-item-card");
        card.setPadding(new Insets(14));
        card.setFillWidth(true);

        boolean isSelected = selectedTask != null && selectedTask.getTaskId() == task.getTaskId();
        if (isSelected) {
            card.setStyle(
                    "-fx-border-color: #5B2B97; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 18; " +
                            "-fx-background-radius: 18;"
            );
        } else {
            card.setStyle("");
        }

        /* HEADER */
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        String headerTitle = safe(task.getTitle()).isBlank() ? "Task #" + task.getTaskId() : safe(task.getTitle());
        Label title = new Label(headerTitle);
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCardUpdate = new Button("Update");
        btnCardUpdate.getStyleClass().add("btn-secondary");
        btnCardUpdate.setOnAction(e -> {
            e.consume();
            selectedTask = task;
            populateFields(task);
            updateStatusLabel("Selected task #" + task.getTaskId());
            refreshTasksCardsView();
            handleUpdateTask();
        });

        Button btnCardDelete = new Button("Delete");
        btnCardDelete.getStyleClass().add("btn-danger");
        btnCardDelete.setOnAction(e -> {
            e.consume();
            selectedTask = task;
            populateFields(task);
            updateStatusLabel("Selected task #" + task.getTaskId());
            refreshTasksCardsView();
            handleDeleteTask();
        });

        boolean canDelete = btnDelete == null || btnDelete.isVisible();
        btnCardDelete.setVisible(canDelete);
        btnCardDelete.setManaged(canDelete);

        actions.getChildren().add(btnCardUpdate);
        if (canDelete) actions.getChildren().add(btnCardDelete);

        header.getChildren().addAll(title, spacer, actions);

        /* BODY GRID */
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(18);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(32); c2.setHgrow(Priority.ALWAYS);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(18);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(32); c4.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().setAll(c1, c2, c3, c4);

        Label lblTaskIdKey = new Label("Task ID"); lblTaskIdKey.getStyleClass().add("form-label");
        Label lblStatusKey = new Label("Status"); lblStatusKey.getStyleClass().add("form-label");
        Label lblPlanIdKey = new Label("Plan ID"); lblPlanIdKey.getStyleClass().add("form-label");
        Label lblStateKey = new Label("State"); lblStateKey.getStyleClass().add("form-label");

        Label lblTaskIdVal = new Label(String.valueOf(task.getTaskId()));
        lblTaskIdVal.setMaxWidth(Double.MAX_VALUE);
        lblTaskIdVal.setStyle("-fx-text-fill: #1F2A44; -fx-font-weight: 600;");

        HBox statusBox = buildTaskStatusNode(task.getStatus());

        Label lblPlanIdVal = new Label(String.valueOf(task.getPlanId()));
        lblPlanIdVal.setMaxWidth(Double.MAX_VALUE);
        lblPlanIdVal.setStyle("-fx-text-fill: #1F2A44; -fx-font-weight: 600;");

        Label lblStateVal = new Label(computeTaskState(task));
        lblStateVal.setStyle(getTaskStateStyle(task));

        grid.add(lblTaskIdKey, 0, 0);
        grid.add(lblTaskIdVal, 1, 0);
        grid.add(lblStatusKey, 2, 0);
        grid.add(statusBox, 3, 0);

        grid.add(lblPlanIdKey, 0, 1);
        grid.add(lblPlanIdVal, 1, 1);
        grid.add(lblStateKey, 2, 1);
        grid.add(lblStateVal, 3, 1);

        GridPane.setHgrow(lblTaskIdVal, Priority.ALWAYS);
        GridPane.setHgrow(lblPlanIdVal, Priority.ALWAYS);
        GridPane.setHgrow(statusBox, Priority.ALWAYS);

        /* EXTRA INFO */
        VBox extraBox = new VBox(8);

        Label lblDescKey = new Label("Description");
        lblDescKey.getStyleClass().add("form-label");

        String desc = safe(task.getDescription());
        if (desc.length() > 180) desc = desc.substring(0, 177) + "...";

        Label lblDescVal = new Label(desc.isBlank() ? "No description" : desc);
        lblDescVal.setWrapText(true);
        lblDescVal.getStyleClass().add("muted-text");
        lblDescVal.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #485066;");

        // Attachment row (name + View + Remove)
        HBox attachRow = new HBox(10);
        attachRow.setAlignment(Pos.CENTER_LEFT);

        String link = task.getFilepath();
        boolean hasWebAttachment = hasWebAttachment(task);

        String name = safe(task.getOriginalFileName());
        if (name.isBlank() && hasWebAttachment) name = "Attachment";

        Label lblAttach = new Label(hasWebAttachment ? ("Attachment: " + name) : "Attachment: None");
        lblAttach.getStyleClass().add("muted-text");

        Region attachSpacer = new Region();
        HBox.setHgrow(attachSpacer, Priority.ALWAYS);

        Button btnOpenAttachment = new Button("View");
        btnOpenAttachment.getStyleClass().add("btn-secondary");
        btnOpenAttachment.setFocusTraversable(false);
        btnOpenAttachment.setVisible(hasWebAttachment);
        btnOpenAttachment.setManaged(hasWebAttachment);
        btnOpenAttachment.setOnAction(e -> {
            e.consume();
            openUrlInBrowser(task.getFilepath());
        });

        Button btnRemoveAttachment = new Button("Remove");
        btnRemoveAttachment.getStyleClass().add("btn-danger");
        btnRemoveAttachment.setFocusTraversable(false);
        btnRemoveAttachment.setVisible(hasWebAttachment);
        btnRemoveAttachment.setManaged(hasWebAttachment);
        btnRemoveAttachment.setOnAction(e -> {
            e.consume();
            try {
                Optional<ButtonType> ok = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Remove attachment for this task?"
                ).showAndWait();

                if (ok.isPresent() && ok.get() == ButtonType.OK) {
                    taskApiService.removeTaskFile(task.getTaskId());
                    loadTasksForPlan();
                }
            } catch (RuntimeException ex) {
                showError("Not allowed", ex.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Error", ex.getMessage());
            }
        });

        attachRow.getChildren().addAll(lblAttach, attachSpacer, btnOpenAttachment, btnRemoveAttachment);

        extraBox.getChildren().addAll(lblDescKey, lblDescVal, attachRow);

        /* CARD CLICK = SELECT */
        card.setOnMouseClicked(e -> {
            selectedTask = task;
            populateFields(task);
            updateStatusLabel("Selected task #" + task.getTaskId());

            if (tvTasks != null) {
                tvTasks.getSelectionModel().select(task);
            }

            refreshTasksCardsView();
        });

        card.setFocusTraversable(false);
        btnCardUpdate.setFocusTraversable(false);
        btnCardDelete.setFocusTraversable(false);

        card.getChildren().addAll(header, grid, extraBox);
        return card;
    }

    private HBox buildTaskStatusNode(String status) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("status-container");

        Region dot = new Region();
        dot.getStyleClass().add("status-dot");

        Label text = new Label(normalizeStatus(status));
        text.getStyleClass().add("status-text");

        applyTaskStatusClasses(dot, text, status);

        box.getChildren().addAll(dot, text);
        return box;
    }

    private void applyTaskStatusClasses(Region dot, Label text, String status) {
        dot.getStyleClass().removeAll(
                "status-dot-pending","status-dot-progress","status-dot-completed","status-dot-hold","status-dot-blocked","status-dot-neutral"
        );
        text.getStyleClass().removeAll(
                "status-text-pending","status-text-progress","status-text-completed","status-text-hold","status-text-blocked","status-text-neutral"
        );

        String s = safe(status).toLowerCase(Locale.ROOT);

        if (s.equals("not started") || s.equals("not_started")) {
            dot.getStyleClass().add("status-dot-pending");
            text.getStyleClass().add("status-text-pending");
        } else if (s.equals("in progress") || s.equals("in_progress")) {
            dot.getStyleClass().add("status-dot-progress");
            text.getStyleClass().add("status-text-progress");
        } else if (s.equals("completed")) {
            dot.getStyleClass().add("status-dot-completed");
            text.getStyleClass().add("status-text-completed");
        } else if (s.equals("blocked")) {
            dot.getStyleClass().add("status-dot-blocked");
            text.getStyleClass().add("status-text-blocked");
        } else if (s.equals("on hold") || s.equals("on_hold")) {
            dot.getStyleClass().add("status-dot-hold");
            text.getStyleClass().add("status-text-hold");
        } else {
            dot.getStyleClass().add("status-dot-neutral");
            text.getStyleClass().add("status-text-neutral");
        }
    }

    /* ================= TASK HELPERS ================= */

    private String computeTaskState(OnboardingTask task) {
        String s = safe(task == null ? null : task.getStatus()).toLowerCase(Locale.ROOT);

        if (s.equals("completed")) return "Completed";
        if (s.equals("blocked")) return "Blocked";
        if (s.equals("in progress") || s.equals("in_progress")) return "Active";
        if (s.equals("on hold") || s.equals("on_hold")) return "On Hold";
        return "Pending";
    }

    private String getTaskStateStyle(OnboardingTask task) {
        String state = computeTaskState(task).toLowerCase(Locale.ROOT);
        return switch (state) {
            case "completed" -> "-fx-text-fill: #1E9B63; -fx-font-weight: 800;";
            case "blocked" -> "-fx-text-fill: #E84855; -fx-font-weight: 800;";
            case "active" -> "-fx-text-fill: #5B2B97; -fx-font-weight: 800;";
            case "on hold" -> "-fx-text-fill: #4C5160; -fx-font-weight: 800;";
            default -> "-fx-text-fill: #FF6A2A; -fx-font-weight: 800;";
        };
    }

    private boolean hasAttachment(OnboardingTask task) {
        return task != null && task.getFilepath() != null && !task.getFilepath().trim().isEmpty();
    }

    private boolean isHttpUrl(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase(Locale.ROOT);
        return t.startsWith("http://") || t.startsWith("https://");
    }

    private boolean hasWebAttachment(OnboardingTask task) {
        return hasAttachment(task) && isHttpUrl(task.getFilepath());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "Unknown";
        String s = status.trim();

        if (s.equalsIgnoreCase("not_started")) return "Not Started";
        if (s.equalsIgnoreCase("in_progress")) return "In Progress";
        if (s.equalsIgnoreCase("on_hold")) return "On Hold";

        return s;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void openUrlInBrowser(String url) {
        try {
            if (url == null || url.isBlank()) {
                showError("No file", "This task has no attachment.");
                return;
            }
            if (!isHttpUrl(url)) {
                showError("Invalid link", "Attachment is not a web link:\n" + url);
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                showError("Unsupported", "Desktop browsing is not supported on this system.");
                return;
            }
            Desktop.getDesktop().browse(new URI(url.trim()));
        } catch (Exception e) {
            e.printStackTrace();
            showError("Open failed", e.getMessage());
        }
    }

    /* ================= WINDOW / ALERTS ================= */

    private Window resolveModalOwner() {
        if (btnUpdate != null && btnUpdate.getScene() != null) return btnUpdate.getScene().getWindow();
        if (tasksCardsContainer != null && tasksCardsContainer.getScene() != null) return tasksCardsContainer.getScene().getWindow();
        if (tasksScrollPane != null && tasksScrollPane.getScene() != null) return tasksScrollPane.getScene().getWindow();
        if (tvTasks != null && tvTasks.getScene() != null) return tvTasks.getScene().getWindow();
        if (txtTitle != null && txtTitle.getScene() != null) return txtTitle.getScene().getWindow();
        return null;
    }

    private void refreshDecisionGuide() {
        if (recommendationsContainer == null) return;

        recommendationsContainer.getChildren().clear();

        int roleId = 3; // default admin-safe fallback
        try {
            if (UserSession.getInstance() != null && UserSession.getInstance().getCurrentUser() != null) {
                roleId = UserSession.getInstance().getCurrentUser().getRoleId();
            }
        } catch (Exception ignored) { }

        if (lblDecisionGuideRole != null) {
            lblDecisionGuideRole.setText("Role: " + roleName(roleId));
        }

        List<OnboardingTask> source = (taskList == null) ? new ArrayList<>() : new ArrayList<>(taskList);
        List<TaskRecommendation> recommendations = decisionGuideService.buildRecommendations(roleId, source);

        int max = Math.min(4, recommendations.size());
        for (int i = 0; i < max; i++) {
            TaskRecommendation rec = recommendations.get(i);
            recommendationsContainer.getChildren().add(buildRecommendationRow(rec));
        }
    }

    private HBox buildRecommendationRow(TaskRecommendation rec) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle(
                "-fx-background-color: rgba(43,20,80,0.03);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #EEF0F7;" +
                        "-fx-border-radius: 12;"
        );

        Label priorityChip = new Label(rec.getPriority().name());
        priorityChip.setStyle(priorityChipStyle(rec.getPriority()));

        VBox textBox = new VBox(4);

        Label msg = new Label(rec.getMessage());
        msg.setWrapText(true);
        msg.setMaxWidth(Double.MAX_VALUE);
        msg.setStyle("-fx-text-fill: #1F2430; -fx-font-weight: 700;");

        Label reason = new Label(rec.getReason() == null ? "" : rec.getReason());
        reason.setWrapText(true);
        reason.setMaxWidth(Double.MAX_VALUE);
        reason.getStyleClass().add("muted-text");

        Label type = new Label(rec.getType().replace('_', ' '));
        type.getStyleClass().add("muted-text");
        type.setStyle("-fx-font-size: 11px;");

        textBox.getChildren().addAll(msg, reason, type);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button actionBtn = new Button(rec.getActionLabel() == null ? "Open" : rec.getActionLabel());
        actionBtn.getStyleClass().add("btn-secondary");
        actionBtn.setFocusTraversable(false);

        actionBtn.setOnAction(e -> {
            e.consume();
            handleRecommendationAction(rec);
        });

        row.getChildren().addAll(priorityChip, textBox, actionBtn);
        return row;
    }

    private void handleRecommendationAction(TaskRecommendation rec) {
        if (rec == null) return;

        TaskRecommendation.ActionType actionType = rec.getActionType();
        if (actionType == null) actionType = TaskRecommendation.ActionType.SELECT_TASK;

        switch (actionType) {
            case REFRESH -> {
                try {
                    loadTasksForPlan();
                    updateStatusLabel("Decision Guide refreshed.");
                } catch (Exception e) {
                    updateStatusLabel("Refresh failed.");
                }
            }

            case SELECT_TASK -> {
                if (rec.getTaskId() != null) {
                    selectTaskById(rec.getTaskId());
                } else {
                    updateStatusLabel("Recommendation: " + rec.getMessage());
                }
            }

            case OPEN_UPDATE -> {
                if (rec.getTaskId() != null) {
                    selectTaskById(rec.getTaskId());
                    if (selectedTask != null) {
                        handleUpdateTask();
                    }
                } else {
                    updateStatusLabel("No task linked to this recommendation.");
                }
            }
        }
    }

    private String priorityChipStyle(TaskRecommendation.Priority priority) {
        return switch (priority) {
            case HIGH -> "-fx-background-color: rgba(232,72,85,0.14); -fx-text-fill: #E84855; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 999;";
            case MEDIUM -> "-fx-background-color: rgba(255,106,42,0.14); -fx-text-fill: #FF6A2A; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 999;";
            case LOW -> "-fx-background-color: rgba(30,155,99,0.14); -fx-text-fill: #1E9B63; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 999;";
        };
    }

    private void selectTaskById(int taskId) {
        if (taskList == null || taskList.isEmpty()) return;

        for (OnboardingTask t : taskList) {
            if (t != null && t.getTaskId() == taskId) {
                selectedTask = t;
                populateFields(t);

                if (tvTasks != null) {
                    tvTasks.getSelectionModel().select(t);
                }

                updateStatusLabel("Selected task #" + t.getTaskId() + " from Decision Guide");
                refreshTasksCardsView();
                return;
            }
        }

        updateStatusLabel("Task #" + taskId + " not found in current list");
    }

    private String roleName(int roleId) {
        return switch (roleId) {
            case 1 -> "Candidate";
            case 2 -> "Recruiter";
            case 3 -> "Admin";
            default -> "User";
        };
    }

    private void updateStatusLabel(String msg) {
        if (lblStatus != null) lblStatus.setText(msg);
    }

    private void showInfo(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, c);
        a.setTitle(t);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showError(String t, String c) {
        Alert a = new Alert(Alert.AlertType.ERROR, c);
        a.setTitle(t);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
=======
    /**
     * Executes this operation.
     */
    private void clearFields() {
        txtPlanId.clear();
        txtTitle.clear();
        txtDescription.clear();
        txtFilepath.clear();
        cmbStatus.setValue("Not Started");
        selectedTask = null;
        tvTasks.getSelectionModel().clearSelection();
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
}
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
