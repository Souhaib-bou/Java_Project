package Controllers;

import Models.OnboardingTask;
import Services.api.TaskApiService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class OnboardingTaskEditController implements Initializable {

    @FXML private TextField txtPlanId;
    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private TextField txtFilepath;

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

    public void setTask(OnboardingTask task) {
        this.taskToEdit = task;

        txtPlanId.setText(String.valueOf(task.getPlanId()));
        txtTitle.setText(task.getTitle());
        txtDescription.setText(task.getDescription());
        cmbStatus.setValue(task.getStatus());
        txtFilepath.setText(task.getFilepath());

        txtPlanId.setDisable(true);

        // apply mode locking (in case setTask called after mode)
        txtTitle.setDisable(candidateMode);
        txtDescription.setDisable(candidateMode);

        // reset selection when opening editor
        selectedFile = null;
    }

    @FXML
    private void handleBrowse() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(txtFilepath.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            // show local path just to confirm selection (we’ll replace with Cloudinary URL after upload)
            txtFilepath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleSave() {
        if (taskToEdit == null) return;

        try {
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

            OnboardingTask updated = new OnboardingTask(
                    taskToEdit.getTaskId(),
                    planId,
                    txtTitle.getText(),
                    txtDescription.getText(),
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