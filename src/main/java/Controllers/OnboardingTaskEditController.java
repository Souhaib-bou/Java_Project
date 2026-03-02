package Controllers;

import Models.OnboardingTask;
<<<<<<< HEAD
import Services.api.TaskApiService;
=======
import Services.PlanService;
import Services.TaskService;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

<<<<<<< HEAD

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

=======
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
public class OnboardingTaskEditController implements Initializable {

    @FXML private TextField txtPlanId;
    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private TextField txtFilepath;

<<<<<<< HEAD
    private final TaskApiService taskApiService = new TaskApiService();

    private OnboardingTask taskToEdit;
    private boolean candidateMode = false;

    // NEW: keep actual selected file here (don’t rely on local path)
    private File selectedFile;

    public void setCandidateMode(boolean candidateMode) {
        this.candidateMode = candidateMode;

        // Candidate: only status + filepath editable
        if (txtTitle != null) txtTitle.setDisable(candidateMode);
        if (txtDescription != null) txtDescription.setDisable(candidateMode);

        // planId always locked
        if (txtPlanId != null) txtPlanId.setDisable(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbStatus.getItems().setAll("Not Started", "In Progress", "Completed", "Blocked", "On Hold");
    }

=======
    private final TaskService taskService = new TaskService();
    private final PlanService planService = new PlanService();

    private OnboardingTask taskToEdit;

    @Override
    /**
     * Initializes UI components and loads initial data.
     */
    public void initialize(URL location, ResourceBundle resources) {
        cmbStatus.getItems().addAll("Not Started", "In Progress", "Completed", "Blocked", "On Hold");
    }

    /**
     * Sets the task value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public void setTask(OnboardingTask task) {
        this.taskToEdit = task;

        txtPlanId.setText(String.valueOf(task.getPlanId()));
        txtTitle.setText(task.getTitle());
        txtDescription.setText(task.getDescription());
        cmbStatus.setValue(task.getStatus());
        txtFilepath.setText(task.getFilepath());

        txtPlanId.setDisable(true);
<<<<<<< HEAD

        // apply mode locking (in case setTask called after mode)
        txtTitle.setDisable(candidateMode);
        txtDescription.setDisable(candidateMode);

        // reset selection when opening editor
        selectedFile = null;
    }

    @FXML
=======
    }


    @FXML
    /**
     * Handles the associated UI event.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private void handleBrowse() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(txtFilepath.getScene().getWindow());
        if (file != null) {
<<<<<<< HEAD
            selectedFile = file;
            // show local path just to confirm selection (we’ll replace with Cloudinary URL after upload)
=======
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
            txtFilepath.setText(file.getAbsolutePath());
        }
    }

    @FXML
<<<<<<< HEAD
=======
    /**
     * Handles the associated UI event.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private void handleSave() {
        if (taskToEdit == null) return;

        try {
<<<<<<< HEAD
            String uiStatus = cmbStatus.getValue();

            // If user typed a URL manually, keep it. If they selected a file, we'll upload and replace it.
            String filepath = txtFilepath.getText();
            if (filepath != null && filepath.isBlank()) filepath = null;

            // 1) If a file was selected, upload to backend -> backend uploads to Cloudinary -> returns updated task JSON
            if (selectedFile != null) {
                String uploadedTaskJson = taskApiService.uploadTaskFile(taskToEdit.getTaskId(), selectedFile);

                // Extract Cloudinary URL from returned JSON (field "filePath")
                String cloudUrl = extractJsonStringValue(uploadedTaskJson, "filePath");
                if (cloudUrl != null && !cloudUrl.isBlank()) {
                    filepath = cloudUrl;
                    txtFilepath.setText(cloudUrl); // show final cloud URL in UI
                }

                // clear selection after upload
                selectedFile = null;
            }

            // 2) Save the rest according to mode
            if (candidateMode) {
                // Candidate endpoint (status + filePath only)
                taskApiService.candidateUpdate(taskToEdit.getTaskId(), uiStatus, filepath);
                closeWindow();
                return;
            }

            // Admin/Recruiter: full update via API
            int planId = Integer.parseInt(txtPlanId.getText().trim());
=======
            if (txtPlanId.getText().trim().isEmpty() || txtTitle.getText().trim().isEmpty()) {
                showWarning("Validation", "Plan ID and Title are required.");
                return;
            }

            int planId = Integer.parseInt(txtPlanId.getText().trim());
            if (planService.getOnboardingPlanById(planId) == null) {
                showWarning("Validation", "Plan ID does not exist.");
                return;
            }
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67

            OnboardingTask updated = new OnboardingTask(
                    taskToEdit.getTaskId(),
                    planId,
                    txtTitle.getText(),
                    txtDescription.getText(),
<<<<<<< HEAD
                    uiStatus,
                    filepath
            );

            taskApiService.updateTaskJson(taskToEdit.getTaskId(), updated);
            closeWindow();

        } catch (RuntimeException ex) {
            showError("Not allowed", ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", e.getMessage());
=======
                    cmbStatus.getValue() == null ? "Not Started" : cmbStatus.getValue(),
                    txtFilepath.getText() // file is allowed here
            );

            taskService.updateOnboardingTask(taskToEdit.getTaskId(), updated);

            closeWindow();

        } catch (NumberFormatException e) {
            showWarning("Validation", "Plan ID must be a number.");
        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
        }
    }

    @FXML
<<<<<<< HEAD
=======
    /**
     * Handles the associated UI event.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private void handleCancel() {
        closeWindow();
    }

<<<<<<< HEAD
=======
    /**
     * Executes this operation.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    private void closeWindow() {
        Stage stage = (Stage) txtTitle.getScene().getWindow();
        stage.close();
    }

<<<<<<< HEAD
    private void showError(String t, String c) {
        Alert a = new Alert(Alert.AlertType.ERROR, c);
        a.setTitle(t);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /**
     * Minimal JSON string field extractor (no external JSON lib needed).
     * Works for simple Spring Boot JSON like: "filePath":"https://...."
     */
    private String extractJsonStringValue(String json, String field) {
        if (json == null) return null;

        String needle = "\"" + field + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;

        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;

        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;

        int j = firstQuote + 1;
        StringBuilder sb = new StringBuilder();
        boolean escape = false;

        while (j < json.length()) {
            char ch = json.charAt(j);
            if (escape) {
                // handle basic escapes
                if (ch == '"' || ch == '\\' || ch == '/') sb.append(ch);
                else if (ch == 'n') sb.append('\n');
                else if (ch == 'r') sb.append('\r');
                else if (ch == 't') sb.append('\t');
                else sb.append(ch);
                escape = false;
            } else {
                if (ch == '\\') escape = true;
                else if (ch == '"') break;
                else sb.append(ch);
            }
            j++;
        }

        return sb.toString();
    }
}
=======
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
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
