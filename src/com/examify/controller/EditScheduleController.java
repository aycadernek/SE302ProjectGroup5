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

    private ScheduleManager scheduleManager;
    private MainController mainController;
    private Schedule selectedSchedule;

    public void setScheduleManager(ScheduleManager scheduleManager) {
        this.scheduleManager = scheduleManager;
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
    }

    private void clearFields() {
        txtName.clear();
        txtMinSlotNumber.clear();
        txtMaxSlotNumber.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
    }

    private void handleUpdate() {
        if (selectedSchedule == null) {
            showAlert(Alert.AlertType.WARNING, "No Schedule Selected", "Please select a schedule to update.");
            return;
        }

        try {
            int newMinSlot = Integer.parseInt(txtMinSlotNumber.getText());
            int newMaxSlot = Integer.parseInt(txtMaxSlotNumber.getText());
            String newName = txtName.getText();
            LocalDate newStartDate = startDatePicker.getValue();
            LocalDate newEndDate = endDatePicker.getValue();

            boolean slotsChanged = newMinSlot != selectedSchedule.getMinSlot() || newMaxSlot != selectedSchedule.getMaxSlot();

            if (slotsChanged) {
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Confirm Schedule Regeneration");
                confirmation.setHeaderText("Changing slot numbers requires regenerating the schedule.");
                confirmation.setContentText("This will delete the current exam placements and create a new schedule with the new settings. Are you sure you want to proceed?");

                Optional<ButtonType> result = confirmation.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    recreateSchedule(newName, newStartDate, newEndDate, newMinSlot, newMaxSlot);
                }
            } else {
                selectedSchedule.setName(newName);
                selectedSchedule.setStartDate(newStartDate);
                selectedSchedule.setEndDate(newEndDate);
                // The min/max slots are not changed, but we still need to update them in the db
                selectedSchedule.setMinSlot(newMinSlot);
                selectedSchedule.setMaxSlot(newMaxSlot);

                scheduleManager.updateSchedule(selectedSchedule);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule updated successfully.");
                mainController.refreshData();
                closeWindow();
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Slot numbers must be valid integers.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not update the schedule: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void recreateSchedule(String newName, LocalDate newStartDate, LocalDate newEndDate, int newMinSlot, int newMaxSlot) throws Exception {
        List<String> courseCodes = selectedSchedule.getExams().stream()
                .map(Exam::getCourseCode)
                .distinct()
                .collect(Collectors.toList());
        List<Course> courses = scheduleManager.getCoursesWithDetails(courseCodes);

        List<String> classroomIds = selectedSchedule.getExams().stream()
                .map(Exam::getClassroomId)
                .distinct()
                .collect(Collectors.toList());
        List<Classroom> classrooms = scheduleManager.getClassroomsWithDetails(classroomIds);

        scheduleManager.recreateSchedule(selectedSchedule.getScheduleId(), newName, newStartDate, newEndDate, newMinSlot, newMaxSlot, courses, classrooms);

        showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule regenerated and updated successfully.");
        mainController.refreshData();
        closeWindow();
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