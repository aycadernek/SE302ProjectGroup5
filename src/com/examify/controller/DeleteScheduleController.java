package com.examify.controller;

import com.examify.model.ScheduleManager;
import com.examify.model.entities.Schedule;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Optional;

public class DeleteScheduleController {

    @FXML private ComboBox<Schedule> scheduleSelector;
    @FXML private TextField txtName;
    @FXML private TextField txtMinSlotNumber;
    @FXML private TextField txtMaxSlotNumber;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button deleteButton;

    private ScheduleManager scheduleManager;
    private MainController mainController;

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
            if (newValue != null) {
                populateFields(newValue);
            } else {
                clearFields();
            }
        });

        deleteButton.setOnAction(e -> handleDelete());

        txtName.setEditable(false);
        txtMinSlotNumber.setEditable(false);
        txtMaxSlotNumber.setEditable(false);
        startDatePicker.setEditable(false);
        endDatePicker.setEditable(false);
    }

    public void loadSchedules() {
        if (scheduleManager != null) {
            scheduleSelector.setItems(FXCollections.observableArrayList(scheduleManager.getAllSchedules()));
        }
    }

    private void populateFields(Schedule schedule) {
        txtName.setText(schedule.getName());
        txtMinSlotNumber.setText(String.valueOf(schedule.getSlotsPerDay())); 
        txtMaxSlotNumber.setText(String.valueOf(schedule.getSlotsPerDay())); 
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

    private void handleDelete() {
        Schedule selectedSchedule = scheduleSelector.getValue();
        if (selectedSchedule != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Schedule");
            alert.setHeaderText("Are you sure you want to delete the schedule: " + selectedSchedule.getName() + "?");
            alert.setContentText("This action cannot be undone.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    scheduleManager.deleteSchedule(selectedSchedule.getScheduleId());
                    mainController.refreshData(); 
                    closeWindow();
                } catch (Exception ex) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error Deleting Schedule");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("Could not delete the schedule. Error: " + ex.getMessage());
                    errorAlert.showAndWait();
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Schedule Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a schedule to delete.");
            alert.showAndWait();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) deleteButton.getScene().getWindow();
        stage.close();
    }
}

