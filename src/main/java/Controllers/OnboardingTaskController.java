package Controllers;

import Models.OnboardingTask;
import Services.PlanService;
import Services.TaskService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class OnboardingTaskController implements Initializable {

    @FXML private TextField txtPlanId;
    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbStatus;

    // keep them in UI, but disabled (file only on update)
    @FXML private TextField txtFilepath;
    @FXML private Button btnBrowse;

    @FXML private TableView<OnboardingTask> tvTasks;
    @FXML private TableColumn<OnboardingTask, Integer> colTaskId;
    @FXML private TableColumn<OnboardingTask, Integer> colPlanId;
    @FXML private TableColumn<OnboardingTask, String> colTitle;
    @FXML private TableColumn<OnboardingTask, String> colDescription;
    @FXML private TableColumn<OnboardingTask, String> colStatus;
    @FXML private TableColumn<OnboardingTask, String> colFilepath;

    @FXML private Label lblStatus;

    private TaskService taskService;
    private PlanService planService;
    private ObservableList<OnboardingTask> taskList;
    private OnboardingTask selectedTask;
    private Integer currentPlanId = null;


    @Override
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

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void handleBack() {
        if (onBack != null) onBack.run();
    }

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
            showError("Error", e.getMessage());
        }
    }


    private void populateFields(OnboardingTask task) {
        txtPlanId.setText(String.valueOf(task.getPlanId()));
        txtTitle.setText(task.getTitle());
        txtDescription.setText(task.getDescription());
        cmbStatus.setValue(task.getStatus());

        // show it but disabled
        txtFilepath.setText(task.getFilepath());
    }
    public void setPlanContext(int planId) {
        this.currentPlanId = planId;

        // lock the planId field so user can't type another plan id
        txtPlanId.setText(String.valueOf(planId));
        txtPlanId.setDisable(true);

        // load only tasks for this plan
        loadTasksForPlan();
    }


    @FXML
    private void handleAddTask() {
        try {
            if (txtPlanId.getText().trim().isEmpty() || txtTitle.getText().trim().isEmpty()) {
                showError("Validation", "Plan ID and Title are required.");
                return;
            }

            if (currentPlanId == null) {
                showError("Error", "This task window is not linked to a plan.");
                return;
            }
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
            showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateTask() {

        if (selectedTask == null) {
            showError("No Selection", "Select a task first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/OnboardingTaskEditView.fxml"));
            Parent root = loader.load();

            OnboardingTaskEditController editController = loader.getController();
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
            stage.setMinWidth(650);
            stage.setMinHeight(380);

            stage.showAndWait();


            loadTasksForPlan(); // will show all if no plan context was set yet

            clearFields();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Cannot open update window. Check console.");
        }
    }

    @FXML
    private void handleDeleteTask() {
        if (selectedTask == null) return;

        Optional<ButtonType> result = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete this task?"
        ).showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                taskService.deleteOnboardingTask(selectedTask.getTaskId());
                loadTasksForPlan(); // will show all if no plan context was set yet
                clearFields();
            } catch (SQLException e) {
                showError("Error", e.getMessage());
            }
        }
    }

    private void clearFields() {
        txtPlanId.clear();
        txtTitle.clear();
        txtDescription.clear();
        txtFilepath.clear();
        cmbStatus.setValue("Not Started");
        selectedTask = null;
        tvTasks.getSelectionModel().clearSelection();
    }

    private void showInfo(String t, String c) {
        new Alert(Alert.AlertType.INFORMATION, c).showAndWait();
    }

    private void showError(String t, String c) {
        new Alert(Alert.AlertType.ERROR, c).showAndWait();
    }
}
