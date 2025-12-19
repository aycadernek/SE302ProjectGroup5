package com.examify.controller;

import com.examify.model.DatabaseConnection;
import com.examify.model.FileImportService;
import com.examify.model.ScheduleManager;
import com.examify.model.entities.Classroom;
import com.examify.model.entities.Course;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class NewScheduleController {

    @FXML private TextField txtName;
    @FXML private TextField txtMinSlotNumber;
    @FXML private TextField txtMaxSlotNumber;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button generateButton;
    @FXML private Button btnImportCourses;
    @FXML private Button btnImportClassrooms;

    private ScheduleManager scheduleManager;
    private FileImportService fileImportService;
    private MainController mainController;

    public void setScheduleManager(ScheduleManager scheduleManager) {
        this.scheduleManager = scheduleManager;
        this.fileImportService = new FileImportService(scheduleManager.getDbConnection());
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        generateButton.setOnAction(e -> handleGenerate());
        btnImportCourses.setOnAction(e -> handleImport(FileImportService.DataType.COURSES));
        btnImportClassrooms.setOnAction(e -> handleImport(FileImportService.DataType.CLASSROOMS));
    }

    private void handleGenerate() {
        try {
            String name = txtName.getText();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            int minSlot = Integer.parseInt(txtMinSlotNumber.getText());
            int maxSlot = Integer.parseInt(txtMaxSlotNumber.getText());

            if (name.isEmpty() || startDate == null || endDate == null) {
                showAlert(Alert.AlertType.WARNING, "Missing Information", "Please fill in all fields.");
                return;
            }

            DatabaseConnection db = scheduleManager.getDbConnection();
            List<Course> courses = db.loadAllCourses();
            List<Classroom> classrooms = db.loadAllClassrooms();

            if (courses.isEmpty() || classrooms.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Data Missing", "No courses or classrooms found in the database. Please import them first.");
                return;
            }

            scheduleManager.createSchedule(name, startDate, endDate, minSlot, maxSlot, courses, classrooms);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule '" + name + "' generated successfully.");
            mainController.refreshData();
            closeWindow();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Min and Max slots must be valid numbers.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not generate schedule: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleImport(FileImportService.DataType dataType) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import " + dataType.toString());
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported Files", "*.csv", "*.json", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(btnImportCourses.getScene().getWindow());

        if (file != null) {
            FileImportService.ImportResult result = fileImportService.importData(file.toPath(), dataType);
            if (result.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Import Successful", result.getMessage());
            } else {
                showAlert(Alert.AlertType.ERROR, "Import Failed", String.join("\n", result.getErrors()));
            }
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) generateButton.getScene().getWindow();
        stage.close();
    }
}
