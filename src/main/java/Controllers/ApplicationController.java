package Controllers;

import Models.Application;
import Services.ApplicationService;
import Utils.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.LocalDate;

public class ApplicationController {

    @FXML private DatePicker datePicker;
    @FXML private TextArea coverLetterField;
    @FXML private TextField resumeField;
    @FXML private TextField userIdField;
    @FXML private TextField jobIdField;

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

    public void setShell(MainShellController shell) {
        this.shell = shell;
    }

    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("applicationId"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("user_id"));
        colJobId.setCellValueFactory(new PropertyValueFactory<>("jobOfferId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("applicationDate"));
        colCoverLetter.setCellValueFactory(new PropertyValueFactory<>("coverLetter"));
        colResume.setCellValueFactory(new PropertyValueFactory<>("resumePath"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("currentStatus"));

        addEditDeleteButtonToTable();
        loadData();

        // Optional: auto-fill logged user id
        if (UserSession.getInstance().getCurrentUser() != null) {
            userIdField.setText(String.valueOf(UserSession.getInstance().getCurrentUser().getUserId()));
        }
    }

    @FXML
    public void addApplication() {
        try {
            Application app = new Application(
                    datePicker.getValue(),
                    coverLetterField.getText(),
                    resumeField.getText(),
                    Integer.parseInt(userIdField.getText()),
                    Integer.parseInt(jobIdField.getText())
            );

            service.add(app);
            loadData();
            clearFields();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Add failed. Check console.").showAndWait();
        }
    }

    @FXML
    public void updateApplication() {
        try {
            if (selectedApplication == null) {
                new Alert(Alert.AlertType.WARNING, "Click Edit on a row first.").showAndWait();
                return;
            }

            selectedApplication.setApplicationDate(datePicker.getValue());
            selectedApplication.setCoverLetter(coverLetterField.getText());
            selectedApplication.setResumePath(resumeField.getText());
            selectedApplication.setUser_id(Integer.parseInt(userIdField.getText()));
            selectedApplication.setJobOfferId(Integer.parseInt(jobIdField.getText()));

            service.update(selectedApplication);
            loadData();
            clearFields();
            selectedApplication = null;

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Update failed. Check console.").showAndWait();
        }
    }

    private void deleteApplication(Application app) {
        try {
            service.delete(app.getApplicationId());
            loadData();
            if (selectedApplication == app) selectedApplication = null;
            clearFields();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillForm(Application app) {
        selectedApplication = app;

        datePicker.setValue(app.getApplicationDate());
        coverLetterField.setText(app.getCoverLetter());
        resumeField.setText(app.getResumePath());
        userIdField.setText(String.valueOf(app.getuser_id()));
        jobIdField.setText(String.valueOf(app.getJobOfferId()));
    }

    private void addEditDeleteButtonToTable() {
        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox pane = new HBox(8, editBtn, delBtn);

            {
                editBtn.setOnAction(event -> {
                    Application app = getTableView().getItems().get(getIndex());
                    fillForm(app);
                });

                delBtn.setOnAction(event -> {
                    Application app = getTableView().getItems().get(getIndex());
                    deleteApplication(app);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadData() {
        try {
            data.setAll(service.getAll());
            tableView.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        datePicker.setValue(null);
        coverLetterField.clear();
        resumeField.clear();
        // keep userId if you want:
        // userIdField.clear();
        jobIdField.clear();
    }

    // ✅ NAVIGATION: inside MainShell
    @FXML
    public void goBackToJobOffers() {
        if (shell != null) shell.openJobOffers();
    }
}
