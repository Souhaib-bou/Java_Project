package Controllers;

import Models.OnboardingTask;
import Services.PlanService;
import Services.TaskService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class OnboardingTaskEditController implements Initializable {

    @FXML private TextField txtPlanId;
    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private TextField txtFilepath;

    private final TaskService taskService = new TaskService();
    private final PlanService planService = new PlanService();

    private OnboardingTask taskToEdit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbStatus.getItems().addAll("Not Started", "In Progress", "Completed", "Blocked", "On Hold");
    }

    public void setTask(OnboardingTask task) {
        this.taskToEdit = task;

        txtPlanId.setText(String.valueOf(task.getPlanId()));
        txtTitle.setText(task.getTitle());
        txtDescription.setText(task.getDescription());
        cmbStatus.setValue(task.getStatus());
        txtFilepath.setText(task.getFilepath());

        txtPlanId.setDisable(true);
    }


    @FXML
    private void handleBrowse() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(txtFilepath.getScene().getWindow());
        if (file != null) {
            txtFilepath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleSave() {
        if (taskToEdit == null) return;

        try {
            if (txtPlanId.getText().trim().isEmpty() || txtTitle.getText().trim().isEmpty()) {
                showWarning("Validation", "Plan ID and Title are required.");
                return;
            }

            int planId = Integer.parseInt(txtPlanId.getText().trim());
            if (planService.getOnboardingPlanById(planId) == null) {
                showWarning("Validation", "Plan ID does not exist.");
                return;
            }

            OnboardingTask updated = new OnboardingTask(
                    taskToEdit.getTaskId(),
                    planId,
                    txtTitle.getText(),
                    txtDescription.getText(),
                    cmbStatus.getValue() == null ? "Not Started" : cmbStatus.getValue(),
                    txtFilepath.getText() // file is allowed here
            );

            taskService.updateOnboardingTask(taskToEdit.getTaskId(), updated);

            closeWindow();

        } catch (NumberFormatException e) {
            showWarning("Validation", "Plan ID must be a number.");
        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtTitle.getScene().getWindow();
        stage.close();
    }

    private void showError(String t, String c) {
        new Alert(Alert.AlertType.ERROR, c).showAndWait();
    }

    private void showWarning(String t, String c) {
        new Alert(Alert.AlertType.WARNING, c).showAndWait();
    }
}
