package Controllers;

import Models.Application;
import Services.ApplicationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.LocalDate;

public class ApplicationController {

    // ================= FORM FIELDS =================
    @FXML private DatePicker datePicker;
    @FXML private TextArea coverLetterField;
    @FXML private TextField resumeField;
    @FXML private TextField userIdField;
    @FXML private TextField jobIdField;
    @FXML private ComboBox<String> statusCombo;

    // ================= TABLE =================
    @FXML private TableView<Application> tableView;
    @FXML private TableColumn<Application, Integer> colId;
    @FXML private TableColumn<Application, Integer> colUserId;
    @FXML private TableColumn<Application, Integer> colJobId;
    @FXML private TableColumn<Application, LocalDate> colDate;
    @FXML private TableColumn<Application, String> colCoverLetter;
    @FXML private TableColumn<Application, String> colResume;
    @FXML private TableColumn<Application, String> colStatus;
    @FXML private TableColumn<Application, Void> colAction;

    private final ApplicationService service = new ApplicationService();
    private final ObservableList<Application> data = FXCollections.observableArrayList();

    private Application selectedApplication = null;
    private MainShellController shell;

    private Integer jobContextId = null;
    private Integer currentJobId = null; // null => show all

    // Mock user role
    private final String currentUserRole = "recruiter";

    /**
     * Initializes table column bindings, action buttons, loads data, and prepares UI controls.
     */
    @FXML
    public void initialize() throws SQLException {
        colId.setCellValueFactory(new PropertyValueFactory<>("applicationId"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("user_id"));
        colJobId.setCellValueFactory(new PropertyValueFactory<>("jobOfferId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("applicationDate"));
        colCoverLetter.setCellValueFactory(new PropertyValueFactory<>("coverLetter"));
        colResume.setCellValueFactory(new PropertyValueFactory<>("resumePath"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("currentStatus"));

        addEditDeleteButtonToTable();
        loadData();

        if (jobContextId != null && jobIdField != null) {
            jobIdField.setText(String.valueOf(jobContextId));
            jobIdField.setDisable(true);
        }

        if (statusCombo != null) {
            statusCombo.getItems().setAll("Pending", "Accepted", "Rejected");
            statusCombo.setValue("Pending");
        }
    }

    /**
     * Creates a new application using the form values and inserts it into the database.
     */
    @FXML
    public void addApplication() {
        if (!validateForm()) return;

        // If we are in a job-specific context, enforce the job id in the form.
        if (currentJobId != null) {
            jobIdField.setText(String.valueOf(currentJobId));
        }

        try {
            Application app = new Application(
                    datePicker.getValue(),
                    coverLetterField.getText().trim(),
                    resumeField.getText().trim(),
                    Integer.parseInt(userIdField.getText().trim()),
                    Integer.parseInt(jobIdField.getText().trim())
            );

            // Apply status from ComboBox when available.
            app.setCurrentStatus(statusCombo != null ? statusCombo.getValue() : "Pending");

            service.add(app);
            loadData();
            clearFields();

            new Alert(Alert.AlertType.INFORMATION, "Application added successfully!").showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to add application: " + e.getMessage()).showAndWait();
        }
    }

    /**
     * Navigates back to the Job Offers view inside the main shell.
     * (Hook this method to your Back button's onAction if needed.)
     */
    @FXML
    public void goBackToJobOffers() {
        if (shell != null) shell.openJobOffers();
    }

    /**
     * Alternative back handler (kept for compatibility with existing FXML bindings).
     * If your FXML uses onAction="#handleBackToJobOffers", it will keep working.
     */
    @FXML
    private void handleBackToJobOffers() {
        if (shell != null) shell.openJobOffers();
    }

    /**
     * Updates the selected application with the current form values.
     * Only users with the recruiter role are allowed to update (as currently implemented).
     */
    @FXML
    public void updateApplication() {
        if (!validateForm()) return;

        try {
            if (!"recruiter".equalsIgnoreCase(currentUserRole)) {
                new Alert(Alert.AlertType.WARNING, "Only recruiters can update applications.").showAndWait();
                return;
            }

            if (selectedApplication == null) {
                new Alert(Alert.AlertType.WARNING, "Select an application to update.").showAndWait();
                return;
            }

            selectedApplication.setApplicationDate(datePicker.getValue());
            selectedApplication.setCoverLetter(coverLetterField.getText().trim());
            selectedApplication.setResumePath(resumeField.getText().trim());
            selectedApplication.setUser_id(Integer.parseInt(userIdField.getText().trim()));
            selectedApplication.setJobOfferId(Integer.parseInt(jobIdField.getText().trim()));

            if (statusCombo != null) {
                selectedApplication.setCurrentStatus(statusCombo.getValue());
            }

            service.update(selectedApplication);
            loadData();
            clearFields();
            selectedApplication = null;

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update application: " + e.getMessage()).showAndWait();
        }
    }

    /**
     * Deletes the given application from the database and refreshes the table.
     */
    private void deleteApplication(Application app) {
        try {
            service.delete(app.getApplicationId());
            loadData();

            if (selectedApplication == app) {
                selectedApplication = null;
            }

            clearFields();

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to delete application: " + e.getMessage()).showAndWait();
        }
    }

    /**
     * Populates the form fields based on the selected application from the table.
     */
    private void fillForm(Application app) {
        selectedApplication = app;

        datePicker.setValue(app.getApplicationDate());
        coverLetterField.setText(app.getCoverLetter());
        resumeField.setText(app.getResumePath());

        // NOTE: keeping your original getter name as-is.
        // If you get a compile error here, your model likely uses getUser_id() or getUserId().
        userIdField.setText(String.valueOf(app.getuser_id()));

        jobIdField.setText(String.valueOf(app.getJobOfferId()));

        if (statusCombo != null) {
            statusCombo.setValue(app.getCurrentStatus());
        }
    }

    /**
     * Stores the shell controller reference so this controller can trigger navigation actions.
     */
    public void setShell(MainShellController shell) {
        this.shell = shell;
    }

    /**
     * Adds Edit/Delete buttons to each row of the action column.
     * Edit loads row data into the form; Delete removes the record.
     */
    private void addEditDeleteButtonToTable() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox pane = new HBox(8, editBtn, delBtn);

            {
                editBtn.setStyle("-fx-background-color:#3498db; -fx-text-fill:white; -fx-background-radius:6;");
                delBtn.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-background-radius:6;");

                editBtn.setOnAction(event ->
                        fillForm(getTableView().getItems().get(getIndex()))
                );

                delBtn.setOnAction(event ->
                        deleteApplication(getTableView().getItems().get(getIndex()))
                );
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    /**
     * Validates the current form values and shows a warning dialog if errors exist.
     *
     * @return true if the form is valid; false otherwise.
     */
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        LocalDate date = datePicker.getValue();
        if (date == null) {
            errors.append("- Application date is required.\n");
        } else if (date.isAfter(LocalDate.now())) {
            errors.append("- Application date cannot be in the future.\n");
        }

        String cover = coverLetterField.getText();
        if (cover == null || cover.trim().isEmpty()) {
            errors.append("- Cover letter cannot be empty.\n");
        }

        String resume = resumeField.getText();
        if (resume == null || resume.trim().isEmpty()) {
            errors.append("- Resume path cannot be empty.\n");
        }

        String userIdText = userIdField.getText();
        if (userIdText == null || userIdText.trim().isEmpty()) {
            errors.append("- User ID is required.\n");
        } else {
            try {
                int uid = Integer.parseInt(userIdText.trim());
                if (uid <= 0) errors.append("- User ID must be a positive number.\n");
            } catch (NumberFormatException e) {
                errors.append("- User ID must be a number.\n");
            }
        }

        String jobIdText = jobIdField.getText();
        if (jobIdText == null || jobIdText.trim().isEmpty()) {
            errors.append("- Job Offer ID is required.\n");
        } else {
            try {
                int jid = Integer.parseInt(jobIdText.trim());
                if (jid <= 0) errors.append("- Job Offer ID must be a positive number.\n");
            } catch (NumberFormatException e) {
                errors.append("- Job Offer ID must be a number.\n");
            }
        }

        if (statusCombo != null && statusCombo.getValue() == null) {
            errors.append("- Status must be selected.\n");
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Errors");
            alert.setHeaderText("Please fix the following errors:");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }

    /**
     * Loads applications from the database and displays them in the table.
     * If currentJobId is set, loads only applications for that job.
     */
    private void loadData() {
        try {
            if (currentJobId == null) {
                data.setAll(service.getAll());
            } else {
                data.setAll(service.getByJob(currentJobId));
            }
            tableView.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets a job context so this screen filters applications to a specific job offer.
     * Also auto-fills and locks the jobId field for consistency.
     */
    public void setJobContext(int jobOfferId) {
        this.currentJobId = jobOfferId;

        if (jobIdField != null) {
            jobIdField.setText(String.valueOf(jobOfferId));
            jobIdField.setEditable(false);
        }

        loadData();
    }

    /**
     * Clears all form fields and resets default status.
     */
    private void clearFields() {
        datePicker.setValue(null);
        coverLetterField.clear();
        resumeField.clear();
        userIdField.clear();
        jobIdField.clear();

        if (statusCombo != null) {
            statusCombo.setValue("Pending");
        }
    }
}
