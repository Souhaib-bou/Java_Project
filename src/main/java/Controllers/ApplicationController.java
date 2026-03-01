

package Controllers;

import Models.Application;
import Services.ApplicationService;
import Services.EmailService;
import Services.ResumeParserService;
import Utils.PdfTextExtractor;
import Utils.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
        import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.util.Map;

import java.sql.SQLException;
import java.time.LocalDate;

import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ApplicationController {

    // ================= FORM FIELDS =================
    @FXML private DatePicker datePicker;
    @FXML private TextArea coverLetterField;
    @FXML private TextField resumeField;
    @FXML private VBox formCard;
    @FXML private VBox reviewPanel;

    // Read-only labels
    @FXML private Label viewEmail;
    @FXML private Label viewPhone;
    @FXML private Label viewAppDate;
    @FXML private Label viewAvailDate;
    @FXML private Label viewSalary;
    @FXML private Label viewExperience;
    @FXML private Label viewCoverLetter;
    @FXML private Label viewResume;
    @FXML private Label viewPortfolio;

    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField expectedSalaryField;
    @FXML private DatePicker availabilityDatePicker;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField experienceYearsField;
    @FXML private TextField portfolioUrlField;
    @FXML private Button browseButton;
    @FXML private Button autofillButton;
    @FXML private Label autofillStatus;

    // Recruiter only
    @FXML private TextField scoreField;
    @FXML private TextArea reviewNoteField;
    @FXML private Button addButton;
    @FXML private Button updateButton;

    // ================= TABLE =================
    @FXML private TableView<Application> tableView;
    @FXML private TableColumn<Application, Integer> colId;
    @FXML private TableColumn<Application, LocalDate> colDate;
    @FXML private TableColumn<Application, String> colCoverLetter;
    @FXML private TableColumn<Application, String> colResume;
    @FXML private TableColumn<Application, String> colStatus;
    @FXML private TableColumn<Application, Void> colAction;

    private final ApplicationService service = new ApplicationService();
    private final ObservableList<Application> data = FXCollections.observableArrayList();

    private Application selectedApplication = null;
    private MainShellController shell;

    private Integer currentJobId = null;   // set via setJobContext(...)
    private int currentUserId;
    private String currentUserRole;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() throws SQLException {

        if (!UserSession.getInstance().isLoggedIn()) {
            new Alert(Alert.AlertType.ERROR, "You must be logged in!").showAndWait();
            return;
        }

        var currentUser = UserSession.getInstance().getCurrentUser();
        currentUserId = currentUser.getUserId();
        currentUserRole = currentUser.getRoleName();

        colId.setCellValueFactory(new PropertyValueFactory<>("applicationId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("applicationDate"));
        colCoverLetter.setCellValueFactory(new PropertyValueFactory<>("coverLetter"));
        colResume.setCellValueFactory(new PropertyValueFactory<>("resumePath"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("currentStatus"));

        addEditDeleteButtonToTable();

        statusCombo.getItems().setAll("Pending", "Accepted", "Rejected");
        statusCombo.setValue("Pending");

        applyRoleVisibility();
        loadData();
    }

    private void applyRoleVisibility() {
        boolean isRecruiter = "Recruiter".equalsIgnoreCase(currentUserRole);
        boolean isCandidate = "Candidate".equalsIgnoreCase(currentUserRole);

        // ── Form card: hidden entirely until recruiter clicks Review ──
        formCard.setVisible(isCandidate);
        formCard.setManaged(isCandidate);

        // ── Recruiter review panel: hidden until row selected ──
        reviewPanel.setVisible(false);
        reviewPanel.setManaged(false);

        // ── Buttons ──
        addButton.setVisible(isCandidate);
        addButton.setManaged(isCandidate);
        updateButton.setVisible(isRecruiter);
        updateButton.setManaged(isRecruiter);

        // ── Autofill only for candidates ──
        autofillButton.setVisible(isCandidate);
        autofillButton.setManaged(isCandidate);
        browseButton.setVisible(isCandidate);
        browseButton.setManaged(isCandidate);
    }

    // ================= ADD APPLICATION =================


    @FXML
    public void addApplication() {
        if (!"Candidate".equalsIgnoreCase(currentUserRole)) {
            new Alert(Alert.AlertType.WARNING, "Only candidates can apply for jobs.").showAndWait();
            return;
        }
        if (!validateForm()) return;

        if (currentJobId == null) {
            new Alert(Alert.AlertType.ERROR, "No job selected.").showAndWait();
            return;
        }

        // ✅ Prevent duplicate applications
        try {
            if (service.hasAlreadyApplied(currentUserId, currentJobId)) {
                new Alert(Alert.AlertType.WARNING, "You have already applied for this job.").showAndWait();
                return;
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Could not verify application status: " + e.getMessage()).showAndWait();
            return;
        }

        try {
            Application app = new Application(
                    datePicker.getValue(),
                    coverLetterField.getText(),
                    resumeField.getText(),
                    currentUserId,
                    currentJobId,
                    Double.parseDouble(expectedSalaryField.getText()),
                    availabilityDatePicker.getValue(),
                    phoneField.getText(),
                    emailField.getText(),
                    Integer.parseInt(experienceYearsField.getText()),
                    portfolioUrlField.getText()
            );

            app.setCurrentStatus("Pending");

            service.add(app);

            // ✅ Send email notification to recruiter in background
            try {
                String recruiterEmail = service.getRecruiterEmailByJob(currentJobId);
                String jobTitle = service.getJobTitle(currentJobId);
                String candidateEmail = emailField.getText();

                if (recruiterEmail != null) {
                    EmailService.sendApplicationNotification(
                            recruiterEmail,
                            "Recruiter",
                            candidateEmail,
                            jobTitle,
                            0
                    );
                } else {
                    System.err.println("⚠️ No recruiter email found for jobId: " + currentJobId);
                }
            } catch (SQLException e) {
                System.err.println("⚠️ Could not fetch recruiter info for email: " + e.getMessage());
            }

            loadData();
            clearFields();
            new Alert(Alert.AlertType.INFORMATION, "Application submitted successfully!").showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to add application: " + e.getMessage()).showAndWait();
        }
    }

    // ================= UPDATE =================//
    @FXML
    public void updateApplication() {
        try {
            if (!"Recruiter".equalsIgnoreCase(currentUserRole)) {
                new Alert(Alert.AlertType.WARNING, "Only recruiters can update applications.").showAndWait();
                return;
            }

            if (selectedApplication == null) {
                new Alert(Alert.AlertType.WARNING, "Please select an application to review first.").showAndWait();
                return;
            }

            // ✅ Recruiter-only validation — just score and status
            if (statusCombo.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Please select a status.").showAndWait();
                return;
            }

            if (!scoreField.getText().trim().isEmpty()) {
                try {
                    double score = Double.parseDouble(scoreField.getText().trim());
                    if (score < 0 || score > 100) {
                        new Alert(Alert.AlertType.WARNING, "Score must be between 0 and 100.").showAndWait();
                        return;
                    }
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.WARNING, "Score must be a valid number.").showAndWait();
                    return;
                }
            }

            // Only update the 3 recruiter fields — candidate data stays untouched
            selectedApplication.setCurrentStatus(statusCombo.getValue());
            selectedApplication.setScore(scoreField.getText().trim().isEmpty() ? null : Double.parseDouble(scoreField.getText().trim()));
            selectedApplication.setReviewNote(reviewNoteField.getText());

            service.update(selectedApplication);
            service.update(selectedApplication);

// ✅ Email candidate only if status changed to Accepted or Rejected
            String newStatus = statusCombo.getValue();
            if (newStatus.equalsIgnoreCase("Accepted") || newStatus.equalsIgnoreCase("Rejected")) {
                try {
                    String candidateEmail = selectedApplication.getEmail();
                    String jobTitle = service.getJobTitle(currentJobId != null ? currentJobId : selectedApplication.getJobOfferId());

                    if (candidateEmail != null && !candidateEmail.isEmpty()) {
                        EmailService.sendStatusUpdateNotification(
                                candidateEmail,
                                candidateEmail, // use email as name since we don't store full name
                                jobTitle,
                                newStatus
                        );
                    }
                } catch (SQLException e) {
                    System.err.println("⚠️ Could not fetch job title for status email: " + e.getMessage());
                    // Non-fatal — update already saved
                }
            }
            loadData();

            // Hide the review panel after saving
            reviewPanel.setVisible(false);
            reviewPanel.setManaged(false);
            selectedApplication = null;

            new Alert(Alert.AlertType.INFORMATION, "Application updated successfully!").showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update application: " + e.getMessage()).showAndWait();
        }
    }

    // ================= DELETE =================
    private void deleteApplication(Application app) {
        if (!"recruiter".equalsIgnoreCase(currentUserRole)) {
            new Alert(Alert.AlertType.WARNING, "Only recruiters can delete applications.").showAndWait();
            return;
        }
        try {
            service.delete(app.getApplicationId());
            loadData();
            clearFields();
            if (selectedApplication == app) selectedApplication = null;
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to delete application: " + e.getMessage()).showAndWait();
        }
    }

    // ================= FILL FORM =================
    private void fillForm(Application app) {
        selectedApplication = app;

        datePicker.setValue(app.getApplicationDate());
        coverLetterField.setText(app.getCoverLetter());
        resumeField.setText(app.getResumePath());
        expectedSalaryField.setText(String.valueOf(app.getExpectedSalary()));
        availabilityDatePicker.setValue(app.getAvailabilityDate());
        phoneField.setText(app.getPhone());
        emailField.setText(app.getEmail());
        experienceYearsField.setText(String.valueOf(app.getExperienceYears()));
        portfolioUrlField.setText(app.getPortfolioUrl());

        if (app.getScore() != null) scoreField.setText(String.valueOf(app.getScore()));
        else scoreField.clear();

        reviewNoteField.setText(app.getReviewNote());
        statusCombo.setValue(app.getCurrentStatus());
    }

    // ================= SHELL =================
    public void setShell(MainShellController shell) {
        this.shell = shell;
    }

    // ================= ACTION BUTTONS =================
    private void addEditDeleteButtonToTable() {
        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button reviewBtn = new Button("Review");
            private final Button delBtn = new Button("Delete");
            private final HBox pane = new HBox(8, reviewBtn, delBtn);

            {
                reviewBtn.setStyle("-fx-background-color:#0EA5E9; -fx-text-fill:white; -fx-background-radius:6; -fx-cursor:hand;");
                delBtn.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-background-radius:6; -fx-cursor:hand;");

                reviewBtn.setOnAction(event ->
                        openReviewPanel(getTableView().getItems().get(getIndex()))
                );

                delBtn.setOnAction(event ->
                        deleteApplication(getTableView().getItems().get(getIndex()))
                );
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                boolean isRecruiter = "Recruiter".equalsIgnoreCase(currentUserRole);
                setGraphic(empty || !isRecruiter ? null : pane);
            }
        });
    }

    private void openReviewPanel(Application app) {
        selectedApplication = app;

        // Populate read-only labels
        viewEmail.setText(app.getEmail());
        viewPhone.setText(app.getPhone());
        viewAppDate.setText(app.getApplicationDate() != null ? app.getApplicationDate().toString() : "—");
        viewAvailDate.setText(app.getAvailabilityDate() != null ? app.getAvailabilityDate().toString() : "—");
        viewSalary.setText(app.getExpectedSalary() + " TND");
        viewExperience.setText(app.getExperienceYears() + " years");
        viewCoverLetter.setText(app.getCoverLetter());
        viewResume.setText(app.getResumePath());
        viewPortfolio.setText(app.getPortfolioUrl() != null ? app.getPortfolioUrl() : "—");

        // Populate editable evaluation fields
        statusCombo.setValue(app.getCurrentStatus());
        scoreField.setText(app.getScore() != null ? String.valueOf(app.getScore()) : "");
        reviewNoteField.setText(app.getReviewNote() != null ? app.getReviewNote() : "");

        // Show the panel, scroll to top
        reviewPanel.setVisible(true);
        reviewPanel.setManaged(true);
    }

    @FXML
    private void handleCancelReview() {
        reviewPanel.setVisible(false);
        reviewPanel.setManaged(false);
        selectedApplication = null;
    }

    // ================= VALIDATION =================
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (datePicker.getValue() == null)
            errors.append("- Application date is required.\n");
        else if (datePicker.getValue().isAfter(LocalDate.now()))
            errors.append("- Application date cannot be in the future.\n");

        if (availabilityDatePicker.getValue() == null)
            errors.append("- Availability date is required.\n");
        else if (availabilityDatePicker.getValue().isBefore(LocalDate.now()))
            errors.append("- Availability date cannot be in the past.\n");

        if (coverLetterField.getText().trim().isEmpty())
            errors.append("- Cover letter is required.\n");

        try {
            double salary = Double.parseDouble(expectedSalaryField.getText());
            if (salary <= 0) errors.append("- Expected salary must be positive.\n");
        } catch (Exception e) {
            errors.append("- Expected salary must be a number.\n");
        }

        try {
            int exp = Integer.parseInt(experienceYearsField.getText());
            if (exp < 0) errors.append("- Experience years cannot be negative.\n");
        } catch (Exception e) {
            errors.append("- Experience years must be an integer.\n");
        }

        String email = emailField.getText().trim();
        if (!email.matches("^[^@]+@[^@]+\\.[^@]+$"))
            errors.append("- Invalid email format.\n");

        String phone = phoneField.getText().trim();
        if (!phone.matches("\\d{8,15}"))
            errors.append("- Phone must contain only digits (8–15).\n");

        String portfolio = portfolioUrlField.getText().trim();
        if (!portfolio.isEmpty() && !(portfolio.startsWith("http://") || portfolio.startsWith("https://")))
            errors.append("- Portfolio URL must start with http:// or https://\n");

        if (!scoreField.getText().trim().isEmpty()) {
            try {
                double score = Double.parseDouble(scoreField.getText());
                if (score < 0 || score > 100) errors.append("- Score must be between 0 and 100.\n");
            } catch (Exception e) {
                errors.append("- Score must be a number.\n");
            }
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Errors");
            alert.setHeaderText("Please fix the following:");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }
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

    // ================= LOAD DATA =================
    private void loadData() {
        try {
            // 👤 Candidate: only see his own applications
            if ("Candidate".equalsIgnoreCase(currentUserRole)) {
                data.setAll(service.getByUser(currentUserId));
            }
            // 🧑‍💼 Recruiter: see applications (all or by job)
            else {
                if (currentJobId == null) {
                    data.setAll(service.getAll());
                } else {
                    data.setAll(service.getByJob(currentJobId));
                }
            }

            tableView.setItems(data);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= JOB CONTEXT =================
    public void setJobContext(int jobOfferId) {
        this.currentJobId = jobOfferId;
        loadData();
    }

    // ================= CLEAR =================
    private void clearFields() {
        datePicker.setValue(null);
        coverLetterField.clear();
        resumeField.clear();
        expectedSalaryField.clear();
        availabilityDatePicker.setValue(null);
        phoneField.clear();
        emailField.clear();
        experienceYearsField.clear();
        portfolioUrlField.clear();
        scoreField.clear();
        reviewNoteField.clear();
        statusCombo.setValue("Pending");
    }


    @FXML
    private void handleBrowseResume() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Resume");

        // Only allow PDF and Word files
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Resume Files", "*.pdf", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        // Open dialog
        File selectedFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());

        if (selectedFile != null) {
            try {
                Path resumesDir = Paths.get("resumes");
                Files.createDirectories(resumesDir);
                Path destination = resumesDir.resolve(selectedFile.getName());
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                resumeField.setText(destination.toString());

                // ✅ Enable autofill now that a resume is selected
                autofillButton.setDisable(false);
                autofillStatus.setText("Resume ready — click Autofill to extract data");

            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to copy resume: " + e.getMessage()).showAndWait();
            }
        }
    }

    @FXML
    private void handleAutofill() {
        String path = resumeField.getText();
        if (path == null || path.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select a resume first.").showAndWait();
            return;
        }

        autofillButton.setDisable(true);
        autofillStatus.setText("⏳ Extracting data...");

        // Run in background so UI doesn't freeze
        new Thread(() -> {
            String resumeText = PdfTextExtractor.extract(path);

            if (resumeText.isEmpty()) {
                javafx.application.Platform.runLater(() -> {
                    autofillStatus.setText("❌ Could not read resume text.");
                    autofillButton.setDisable(false);
                });
                return;
            }


            Map<String, String> fields = ResumeParserService.parse(resumeText);

            javafx.application.Platform.runLater(() -> {
                // Application Date
                String appDate = fields.getOrDefault("applicationDate", "");
                if (!appDate.isEmpty()) {
                    try {
                        datePicker.setValue(LocalDate.parse(appDate));
                    } catch (Exception e) {
                        System.err.println("⚠️ Could not parse applicationDate: " + appDate);
                    }
                }

// Availability Date
                String availDate = fields.getOrDefault("availabilityDate", "");
                if (!availDate.isEmpty()) {
                    try {
                        availabilityDatePicker.setValue(LocalDate.parse(availDate));
                    } catch (Exception e) {
                        System.err.println("⚠️ Could not parse availabilityDate: " + availDate);
                    }
                }
                // Fill each field only if the extracted value is not empty
                if (!fields.getOrDefault("email", "").isEmpty())
                    emailField.setText(fields.get("email"));

                if (!fields.getOrDefault("phone", "").isEmpty())
                    phoneField.setText(fields.get("phone"));

                if (!fields.getOrDefault("experienceYears", "").isEmpty())
                    experienceYearsField.setText(fields.get("experienceYears"));

                if (!fields.getOrDefault("expectedSalary", "").isEmpty())
                    expectedSalaryField.setText(fields.get("expectedSalary"));

                if (!fields.getOrDefault("portfolioUrl", "").isEmpty())
                    portfolioUrlField.setText(fields.get("portfolioUrl"));

                if (!fields.getOrDefault("coverLetter", "").isEmpty())
                    coverLetterField.setText(fields.get("coverLetter"));

                autofillStatus.setText("✅ Fields filled! Review and adjust before submitting.");
                autofillButton.setDisable(false);
            });
        }).start();
    }
}