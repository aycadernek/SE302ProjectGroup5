package com.examify.controller;

import com.examify.model.ScheduleManager;
import com.examify.model.entities.Schedule;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class EditScheduleController {

    @FXML private ComboBox<Schedule> scheduleSelector;
    @FXML private TextField txtName;
    @FXML private TextField txtSlotsPerDay;
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
        txtSlotsPerDay.setText(String.valueOf(schedule.getSlotsPerDay()));
        startDatePicker.setValue(schedule.getStartDate());
        endDatePicker.setValue(schedule.getEndDate());
    }

    private void clearFields() {
        txtName.clear();
        txtSlotsPerDay.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
    }

    private void handleUpdate() {
        if (selectedSchedule == null) {
            showAlert(Alert.AlertType.WARNING, "No Schedule Selected", "Please select a schedule to update.");
            return;
        }

        try {
            selectedSchedule.setName(txtName.getText());
            selectedSchedule.setSlotsPerDay(Integer.parseInt(txtSlotsPerDay.getText()));
            selectedSchedule.setStartDate(startDatePicker.getValue());
            selectedSchedule.setEndDate(endDatePicker.getValue());

            scheduleManager.updateSchedule(selectedSchedule);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule updated successfully.");
            mainController.refreshData();
            closeWindow();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Slots per day must be a valid number.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not update the schedule: " + e.getMessage());
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
