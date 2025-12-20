package com.examify.controller;

import com.examify.model.ScheduleManager;
import com.examify.model.entities.Classroom;
import com.examify.model.entities.Course;
import com.examify.model.entities.Exam;
import com.examify.model.entities.Schedule;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EditScheduleController {

    @FXML private ComboBox<Schedule> scheduleSelector;
    @FXML private TextField txtName;
    @FXML private TextField txtMinSlotNumber;
    @FXML private TextField txtMaxSlotNumber;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button updateButton;

    @FXML private TextField txtCourseList;
    @FXML private TextField txtClassroomList;
    @FXML private Button btnImportCourses;
    @FXML private Button btnImportClassrooms;

    private ScheduleManager scheduleManager;
    private com.examify.model.FileImportService fileImportService;
    private MainController mainController;
    private Schedule selectedSchedule;
    private java.io.File coursesFile;
    private java.io.File classroomsFile;

    public void setScheduleManager(ScheduleManager scheduleManager) {
        this.scheduleManager = scheduleManager;
        this.fileImportService = new com.examify.model.FileImportService(scheduleManager.getDbConnection());
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        scheduleSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Schedule schedule) {
                return schedule == null ? "" : schedule.getName();
            }

            @Override
            public Schedule fromString(String string) {
                return scheduleSelector.getItems().stream().filter(s ->
                        s.getName().equals(string)).findFirst().orElse(null);
            }
        });

        scheduleSelector.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            selectedSchedule = newValue;
            if (selectedSchedule != null) {
                populateFields(selectedSchedule);
            } else {
                clearFields();
            }
        });

        updateButton.setOnAction(e -> handleUpdate());
        btnImportCourses.setOnAction(e -> handleSelectFile(com.examify.model.FileImportService.DataType.COURSES));
        btnImportClassrooms.setOnAction(e -> handleSelectFile(com.examify.model.FileImportService.DataType.CLASSROOMS));
    }

    private void handleSelectFile(com.examify.model.FileImportService.DataType dataType) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select " + dataType.toString() + " File");
        java.io.File file = fileChooser.showOpenDialog(updateButton.getScene().getWindow());

        if (file != null) {
            if (dataType == com.examify.model.FileImportService.DataType.COURSES) {
                coursesFile = file;
                txtCourseList.setText(file.getName());
            } else if (dataType == com.examify.model.FileImportService.DataType.CLASSROOMS) {
                classroomsFile = file;
                txtClassroomList.setText(file.getName());
            }
        }
    }

    public void loadSchedules() {
        if (scheduleManager != null) {
            scheduleSelector.setItems(FXCollections.observableArrayList(scheduleManager.getAllSchedules()));
        }
    }

    private void populateFields(Schedule schedule) {
        txtName.setText(schedule.getName());
        txtMinSlotNumber.setText(String.valueOf(schedule.getMinSlot()));
        txtMaxSlotNumber.setText(String.valueOf(schedule.getMaxSlot()));
        startDatePicker.setValue(schedule.getStartDate());
        endDatePicker.setValue(schedule.getEndDate());
        txtCourseList.clear();
        txtClassroomList.clear();
        coursesFile = null;
        classroomsFile = null;
    }

    private void clearFields() {
        txtName.clear();
        txtMinSlotNumber.clear();
        txtMaxSlotNumber.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        txtCourseList.clear();
        txtClassroomList.clear();
    }

    private void handleUpdate() {
        if (selectedSchedule == null) {
            showAlert(Alert.AlertType.WARNING, "No Schedule Selected", "Please select a schedule to update.");
            return;
        }

        int tempScheduleId = -1;
        com.examify.model.DatabaseConnection db = scheduleManager.getDbConnection();
        
        try {
            int newMinSlot = Integer.parseInt(txtMinSlotNumber.getText());
            int newMaxSlot = Integer.parseInt(txtMaxSlotNumber.getText());
            String newName = txtName.getText();
            LocalDate newStartDate = startDatePicker.getValue();
            LocalDate newEndDate = endDatePicker.getValue();

            boolean slotsChanged = newMinSlot != selectedSchedule.getMinSlot() || newMaxSlot != selectedSchedule.getMaxSlot();
            boolean datesChanged = !newStartDate.equals(selectedSchedule.getStartDate()) || !newEndDate.equals(selectedSchedule.getEndDate());
            boolean filesChanged = coursesFile != null || classroomsFile != null;

            if (slotsChanged || datesChanged || filesChanged) {
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Confirm Update");
                confirmation.setHeaderText("Significant changes detected.");
                confirmation.setContentText("This will regenerate the schedule. If successful, old data will be replaced. Proceed?");

                java.util.Optional<ButtonType> result = confirmation.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    
                    tempScheduleId = db.insertInitialSchedule("TEMP_EDIT_" + System.currentTimeMillis(), newStartDate, newEndDate, (newMaxSlot - newMinSlot + 1), newMinSlot, newMaxSlot);

                    if (coursesFile != null) {
                        com.examify.model.FileImportService.ImportResult res = fileImportService.importData(coursesFile.toPath(), com.examify.model.FileImportService.DataType.ENROLLMENTS, tempScheduleId);
                        if (!res.isSuccess()) {
                            db.deleteSchedule(tempScheduleId);
                            tempScheduleId = -1;
                            throw new Exception("Course import failed: " + String.join("\n", res.getErrors()));
                        }
                    }
                    if (classroomsFile != null) {
                        com.examify.model.FileImportService.ImportResult res = fileImportService.importData(classroomsFile.toPath(), com.examify.model.FileImportService.DataType.CLASSROOMS, tempScheduleId);
                        if (!res.isSuccess()) {
                            db.deleteSchedule(tempScheduleId);
                            tempScheduleId = -1;
                            throw new Exception("Classroom import failed: " + String.join("\n", res.getErrors()));
                        }
                    }

                    List<Course> courses = (coursesFile != null) ? 
                        db.loadAllCourses(tempScheduleId) : db.loadAllCourses(selectedSchedule.getScheduleId());
                    List<Classroom> classrooms = (classroomsFile != null) ? 
                        db.loadAllClassrooms(tempScheduleId) : db.loadAllClassrooms(selectedSchedule.getScheduleId());

                    if (courses.isEmpty() || classrooms.isEmpty()) {
                        throw new Exception("No data available for regeneration. Check your files.");
                    }

                    // 4. Generate schedule in memory
                    com.examify.model.ExamScheduler scheduler = new com.examify.model.ExamScheduler();
                    Schedule newGen = scheduler.generateSchedule(newName, courses, classrooms, newStartDate, newEndDate, newMinSlot, newMaxSlot);

                    // IMPORTANT: Set the scheduleId for each generated exam
                    int actualId = selectedSchedule.getScheduleId();
                    for (com.examify.model.entities.Exam exam : newGen.getExams()) {
                        exam.setScheduleId(actualId);
                    }

                    // 5. If generation success, finalize (replace old with new/temp)
                    db.finalizeScheduleUpdate(actualId, tempScheduleId, newName, newStartDate, newEndDate, newMinSlot, newMaxSlot, newGen.getExams());
                    
                    // Update the local object state to reflect changes immediately
                    selectedSchedule.setName(newName);
                    selectedSchedule.setStartDate(newStartDate);
                    selectedSchedule.setEndDate(newEndDate);
                    selectedSchedule.setMinSlot(newMinSlot);
                    selectedSchedule.setMaxSlot(newMaxSlot);
                    selectedSchedule.setExams(newGen.getExams());
                    
                    scheduleManager.setCurrentSchedule(selectedSchedule);
                    
                    db.deleteSchedule(tempScheduleId);
                    tempScheduleId = -1;
                    
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule updated and regenerated successfully.");
                    mainController.refreshData();
                    closeWindow();
                }
            } else {
                // Only name or metadata changed without regeneration
                selectedSchedule.setName(newName);
                selectedSchedule.setStartDate(newStartDate);
                selectedSchedule.setEndDate(newEndDate);
                selectedSchedule.setMinSlot(newMinSlot);
                selectedSchedule.setMaxSlot(newMaxSlot);
                
                db.updateSchedule(selectedSchedule);
                scheduleManager.setCurrentSchedule(selectedSchedule);
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule updated.");
                mainController.refreshData();
                closeWindow();
            }

        } catch (Exception e) {
            if (tempScheduleId != -1) {
                try { db.deleteSchedule(tempScheduleId); } catch (Exception ex) {}
            }
            showAlert(Alert.AlertType.ERROR, "Update Failed", e.getMessage());
            e.printStackTrace();
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
        Stage stage = (Stage) updateButton.getScene().getWindow();
        stage.close();
    }
}