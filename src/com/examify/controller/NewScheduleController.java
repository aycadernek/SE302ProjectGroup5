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
    @FXML private TextField txtCourseList;
    @FXML private TextField txtClassroomList;

    private ScheduleManager scheduleManager;
    private FileImportService fileImportService;
    private MainController mainController;
    private File coursesFile;
    private File classroomsFile;

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
        btnImportCourses.setOnAction(e -> handleSelectFile(FileImportService.DataType.COURSES));
        btnImportClassrooms.setOnAction(e -> handleSelectFile(FileImportService.DataType.CLASSROOMS));
    }

    private void handleGenerate() {
        int scheduleId = -1;
        DatabaseConnection db = scheduleManager.getDbConnection();
        try {
            String name = txtName.getText();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (name.isEmpty() || startDate == null || endDate == null || 
                txtMinSlotNumber.getText().isEmpty() || txtMaxSlotNumber.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Information", "Please fill in all fields.");
                return;
            }

            int minSlot = Integer.parseInt(txtMinSlotNumber.getText());
            int maxSlot = Integer.parseInt(txtMaxSlotNumber.getText());

            if (coursesFile == null || classroomsFile == null) {
                showAlert(Alert.AlertType.WARNING, "Missing Files", "Please select both course and classroom files.");
                return;
            }

            scheduleId = db.insertInitialSchedule(name, startDate, endDate, (maxSlot - minSlot + 1), minSlot, maxSlot);

            FileImportService.ImportResult courseResult = fileImportService.importData(coursesFile.toPath(), FileImportService.DataType.ENROLLMENTS, scheduleId);
            if (!courseResult.isSuccess()) {
                throw new Exception("Failed to import courses: " + String.join("\n", courseResult.getErrors()));
            }

            FileImportService.ImportResult classroomResult = fileImportService.importData(classroomsFile.toPath(), FileImportService.DataType.CLASSROOMS, scheduleId);
            if (!classroomResult.isSuccess()) {
                throw new Exception("Failed to import classrooms: " + String.join("\n", classroomResult.getErrors()));
            }

            List<Course> courses = db.loadAllCourses(scheduleId);
            courses = courses.stream()
                            .collect(java.util.stream.Collectors.toMap(
                                Course::getCourseCode, 
                                c -> c, 
                                (existing, replacement) -> existing))
                            .values().stream().toList();

            List<Classroom> classrooms = db.loadAllClassrooms(scheduleId);

            if (courses.isEmpty() || classrooms.isEmpty()) {
                 throw new Exception("Imported files resulted in no data. Please check your files.");
            }

            scheduleManager.createSchedule(scheduleId, name, startDate, endDate, minSlot, maxSlot, courses, classrooms);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule '" + name + "' generated successfully.");
            mainController.refreshData();
            closeWindow();

        } catch (NumberFormatException e) {
            if (scheduleId != -1) try { db.deleteSchedule(scheduleId); } catch (Exception ex) {}
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Min and Max slots must be valid numbers.");
        } catch (Exception e) {
            if (scheduleId != -1) try { db.deleteSchedule(scheduleId); } catch (Exception ex) {}
            showAlert(Alert.AlertType.ERROR, "Error", "Could not generate schedule: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSelectFile(FileImportService.DataType dataType) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select " + dataType.toString() + " File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported Files", "*.csv", "*.json", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(btnImportCourses.getScene().getWindow());

        if (file != null) {
            if (dataType == FileImportService.DataType.COURSES) {
                coursesFile = file;
                txtCourseList.setText(file.getName());
            } else if (dataType == FileImportService.DataType.CLASSROOMS) {
                classroomsFile = file;
                txtClassroomList.setText(file.getName());
            }
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(400);
        alert.getDialogPane().setMinHeight(200);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) generateButton.getScene().getWindow();
        stage.close();
    }
}
