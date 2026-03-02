package Controllers;

import Models.JobOffer;
import Services.JobOfferService;
import Utils.UserSession;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class JobOfferController {

    // ================= FORM FIELDS =================
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> contractTypeBox;
    @FXML private TextField salaryField;
    @FXML private TextField locationField;
    @FXML private TextField experienceField;
    @FXML private DatePicker publicationDatePicker;
    @FXML private ComboBox<String> statusBox;

    // ================= TABLE =================
    @FXML private TableView<JobOffer> tableView;
    @FXML private TableColumn<JobOffer, Integer> colId;
    @FXML private TableColumn<JobOffer, Integer> colUserId;
    @FXML private TableColumn<JobOffer, String> colTitle;
    @FXML private TableColumn<JobOffer, String> colContract;
    @FXML private TableColumn<JobOffer, Double> colSalary;
    @FXML private TableColumn<JobOffer, Integer> colExperience;
    @FXML private TableColumn<JobOffer, String> colLocation;
    @FXML private TableColumn<JobOffer, String> colStatus;
    @FXML private TableColumn<JobOffer, Void> colAction;

    private final JobOfferService service = new JobOfferService();
    private final ObservableList<JobOffer> data = FXCollections.observableArrayList();

    private JobOffer selectedJob = null;
    private int currentUserId = 0;

    private MainShellController shell;
    /**
     * Sets the shell value.
     */
    public void setShell(MainShellController shell) {
        this.shell = shell;
    }

    // ================= INITIALIZE =================
    @FXML
    /**
     * Initializes UI components and loads initial data.
     */
    public void initialize() {
        // ✅ logged user id (no hardcode)
        if (UserSession.getInstance().getCurrentUser() != null) {
            currentUserId = UserSession.getInstance().getCurrentUser().getUserId();
        }

        colAction.setMinWidth(260);      // adjust if you want
        colAction.setPrefWidth(260);
        colAction.setMaxWidth(600);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);


        contractTypeBox.getItems().setAll("CDI", "CDD", "Internship", "Freelance");
        statusBox.getItems().setAll("Open", "Closed");
        statusBox.setValue("Open");

        colId.setCellValueFactory(new PropertyValueFactory<>("jobOfferId"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colContract.setCellValueFactory(new PropertyValueFactory<>("contractType"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));
        colExperience.setCellValueFactory(new PropertyValueFactory<>("experienceRequired"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("user_id"));

        addActionButtonsToTable();
        loadData();
        clearFields();
    }

    // ================= VALIDATION =================
    /**
     * Executes this operation.
     */
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (titleField.getText() == null || titleField.getText().trim().isEmpty())
            errors.append("- Title is required.\n");

        if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty())
            errors.append("- Description is required.\n");

        if (contractTypeBox.getValue() == null)
            errors.append("- Contract type is required.\n");

        if (salaryField.getText() == null || salaryField.getText().trim().isEmpty()) {
            errors.append("- Salary is required.\n");
        } else {
            try {
                double sal = Double.parseDouble(salaryField.getText().trim());
                if (sal <= 0) errors.append("- Salary must be greater than 0.\n");
            } catch (NumberFormatException e) {
                errors.append("- Salary must be a number.\n");
            }
        }

        if (locationField.getText() == null || locationField.getText().trim().isEmpty())
            errors.append("- Location is required.\n");

        if (experienceField.getText() == null || experienceField.getText().trim().isEmpty()) {
            errors.append("- Experience is required.\n");
        } else {
            try {
                int exp = Integer.parseInt(experienceField.getText().trim());
                if (exp < 0 || exp > 60) errors.append("- Experience must be between 0 and 60.\n");
            } catch (NumberFormatException e) {
                errors.append("- Experience must be a number.\n");
            }
        }

        LocalDate pub = publicationDatePicker.getValue();
        if (pub == null) errors.append("- Publication date is required.\n");
        else if (pub.isAfter(LocalDate.now().plusDays(1)))
            errors.append("- Publication date cannot be in the future.\n");

        if (statusBox.getValue() == null) errors.append("- Status is required.\n");

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

    // ================= ADD =================
    @FXML
    /**
     * Creates a new record and updates the UI.
     */
    public void addJobOffer() {
        if (!validateForm()) return;

        try {
            if (currentUserId <= 0) {
                new Alert(Alert.AlertType.ERROR, "No logged-in user found.").showAndWait();
                return;
            }

            JobOffer j = new JobOffer(
                    titleField.getText().trim(),
                    descriptionField.getText().trim(),
                    contractTypeBox.getValue(),
                    Double.parseDouble(salaryField.getText().trim()),
                    locationField.getText().trim(),
                    Integer.parseInt(experienceField.getText().trim()),
                    Date.valueOf(publicationDatePicker.getValue()),
                    statusBox.getValue(),
                    currentUserId
            );

            service.add(j);
            loadData();
            clearFields();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Add failed. Check console.").showAndWait();
        }
    }

    // ================= UPDATE =================
    @FXML
    /**
     * Updates the selected record and refreshes the UI.
     */
    public void updateJobOffer() {
        if (!validateForm()) return;

        try {
            if (selectedJob == null) {
                new Alert(Alert.AlertType.WARNING, "Click Edit on a row first.").showAndWait();
                return;
            }

            if (selectedJob.getUser_id() != currentUserId) {
                new Alert(Alert.AlertType.ERROR, "You can only edit your own job offers.").showAndWait();
                return;
            }

            selectedJob.setTitle(titleField.getText().trim());
            selectedJob.setDescription(descriptionField.getText().trim());
            selectedJob.setContractType(contractTypeBox.getValue());
            selectedJob.setSalary(Double.parseDouble(salaryField.getText().trim()));
            selectedJob.setLocation(locationField.getText().trim());
            selectedJob.setExperienceRequired(Integer.parseInt(experienceField.getText().trim()));
            selectedJob.setPublicationDate(Date.valueOf(publicationDatePicker.getValue()));
            selectedJob.setStatus(statusBox.getValue());

            service.update(selectedJob);
            loadData();
            clearFields();
            selectedJob = null;

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Update failed. Check console.").showAndWait();
        }
    }

    // ================= FORM HELPERS =================
    /**
     * Executes this operation.
     */
    private void fillForm(JobOffer job) {
        selectedJob = job;

        titleField.setText(job.getTitle());
        descriptionField.setText(job.getDescription());
        contractTypeBox.setValue(job.getContractType());
        salaryField.setText(String.valueOf(job.getSalary()));
        experienceField.setText(String.valueOf(job.getExperienceRequired()));
        locationField.setText(job.getLocation());
        statusBox.setValue(job.getStatus());
        if (job.getPublicationDate() != null) {
            publicationDatePicker.setValue(job.getPublicationDate().toLocalDate());
        }
    }

    /**
     * Executes this operation.
     */
    private void clearFields() {
        titleField.clear();
        descriptionField.clear();
        salaryField.clear();
        locationField.clear();
        experienceField.clear();
        publicationDatePicker.setValue(null);
        contractTypeBox.setValue(null);
        statusBox.setValue("Open");
    }

    /**
     * Loads and refreshes data displayed in the view.
     */
    private void loadData() {
        try {
            data.setAll(service.getAll());
            tableView.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= ACTION BUTTONS COLUMN =================
    /**
     * Creates a new record and updates the UI.
     */
    private void addActionButtonsToTable() {
        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button viewAppsBtn = new Button("View Applications");
            private final HBox pane = new HBox(10, editBtn, deleteBtn, viewAppsBtn);

            {
                pane.setMinWidth(USE_PREF_SIZE);
                pane.setPrefWidth(USE_COMPUTED_SIZE);

                editBtn.setMinWidth(70);
                deleteBtn.setMinWidth(80);
                viewAppsBtn.setMinWidth(140);

                editBtn.setStyle("-fx-background-color:#3498db; -fx-text-fill:white; -fx-background-radius:6;");
                deleteBtn.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-background-radius:6;");
                viewAppsBtn.setStyle("-fx-background-color:#2ecc71; -fx-text-fill:white; -fx-background-radius:6;");

                editBtn.setOnAction(event -> {
                    JobOffer job = getTableView().getItems().get(getIndex());
                    fillForm(job);
                });

                deleteBtn.setOnAction(event -> {
                    JobOffer job = getTableView().getItems().get(getIndex());

                    if (job.getUser_id() != currentUserId) {
                        new Alert(Alert.AlertType.ERROR, "You can only delete your own job offers.").showAndWait();
                        return;
                    }

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete job: " + job.getTitle() + " ?",
                            ButtonType.OK, ButtonType.CANCEL);

                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            try {
                                service.delete(job.getJobOfferId());
                                loadData();
                                if (selectedJob != null && selectedJob.getJobOfferId() == job.getJobOfferId()) {
                                    clearFields();
                                    selectedJob = null;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });

                // ✅ NEW: open Applications filtered by jobOfferId
                viewAppsBtn.setOnAction(event -> {
                    JobOffer job = getTableView().getItems().get(getIndex());
                    if (shell != null) {
                        shell.openApplicationsForJob(job.getJobOfferId());
                    } else {
                        new Alert(Alert.AlertType.ERROR,
                                "Shell is null. Make sure MainShellController calls controller.setShell(this).")
                                .showAndWait();
                    }
                });
            }

            @Override
            /**
             * Updates the selected record and refreshes the UI.
             */
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    // optional old sidebar button
    @FXML
    /**
     * Navigates to the requested screen.
     */
    public void goToApplications() {
        if (shell != null) shell.openApplications();
    }
}
