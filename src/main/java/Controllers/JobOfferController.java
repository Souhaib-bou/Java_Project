package Controllers;

import Models.JobOffer;
import Services.JobOfferService;
import Utils.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.Date;
import java.sql.SQLException;

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

    private int currentUserId;
    private MainShellController shell;

    public void setShell(MainShellController shell) {
        this.shell = shell;
    }

    @FXML
    public void initialize() {

        // ✅ logged user id (no hardcode)
        if (UserSession.getInstance().getCurrentUser() != null) {
            currentUserId = UserSession.getInstance().getCurrentUser().getUserId();
        } else {
            currentUserId = 0; // fallback
        }

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

        addEditButtonToTable();
        loadData();
        clearFields();
    }

    @FXML
    public void addJobOffer() {
        try {
            if (currentUserId <= 0) {
                new Alert(Alert.AlertType.ERROR, "No logged-in user found.").showAndWait();
                return;
            }

            JobOffer j = new JobOffer(
                    titleField.getText(),
                    descriptionField.getText(),
                    contractTypeBox.getValue(),
                    Double.parseDouble(salaryField.getText()),
                    locationField.getText(),
                    Integer.parseInt(experienceField.getText()),
                    Date.valueOf(publicationDatePicker.getValue()),
                    statusBox.getValue() == null ? "Open" : statusBox.getValue(),
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

    @FXML
    public void updateJobOffer() {
        try {
            if (selectedJob == null) {
                new Alert(Alert.AlertType.WARNING, "Click Edit on a row first.").showAndWait();
                return;
            }

            if (selectedJob.getUser_id() != currentUserId) {
                new Alert(Alert.AlertType.ERROR, "You can only edit your own job offers.").showAndWait();
                return;
            }

            selectedJob.setTitle(titleField.getText());
            selectedJob.setDescription(descriptionField.getText());
            selectedJob.setContractType(contractTypeBox.getValue());
            selectedJob.setSalary(Double.parseDouble(salaryField.getText()));
            selectedJob.setLocation(locationField.getText());
            selectedJob.setExperienceRequired(Integer.parseInt(experienceField.getText()));
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

    private void loadData() {
        try {
            data.setAll(service.getAll());
            tableView.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addEditButtonToTable() {
        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
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
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    // ✅ NAVIGATION: inside MainShell
    @FXML
    public void goToApplications() {
        if (shell != null) shell.openApplications();
    }
}
