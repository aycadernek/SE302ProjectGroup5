package com.examify.controller;

import com.examify.model.ExportService;
import com.examify.model.ScheduleManager;
import com.examify.model.entities.Schedule;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;

public class ExportScheduleController {

    @FXML private ComboBox<Schedule> scheduleSelector;
    @FXML private ComboBox<String> fileTypeSelector;
    @FXML private Button exportButton;

    private ScheduleManager scheduleManager;

    public void setScheduleManager(ScheduleManager scheduleManager) {
        this.scheduleManager = scheduleManager;
    }

    @FXML
    public void initialize() {
        fileTypeSelector.setItems(FXCollections.observableArrayList("CSV", "XLSX", "PDF"));
        fileTypeSelector.getSelectionModel().selectFirst();

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

        exportButton.setOnAction(e -> handleExport());
    }

    public void loadSchedules() {
        if (scheduleManager != null) {
            scheduleSelector.setItems(FXCollections.observableArrayList(scheduleManager.getAllSchedules()));
            scheduleSelector.getSelectionModel().selectFirst();
        }
    }

    private void handleExport() {
        Schedule selectedSchedule = scheduleSelector.getValue();
        String selectedType = fileTypeSelector.getValue();

        if (selectedSchedule == null) {
            showAlert(Alert.AlertType.WARNING, "No Schedule Selected", "Please select a schedule to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Schedule");
        fileChooser.setInitialFileName(selectedSchedule.getName().replace(" ", "_") + "." + selectedType.toLowerCase());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(selectedType + " Files", "*." + selectedType.toLowerCase()));

        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());

        if (file != null) {
            try {
                switch (selectedType) {
                    case "CSV":
                        ExportService.exportToCSV(selectedSchedule, file.getAbsolutePath());
                        break;
                    case "XLSX":
                        ExportService.exportToExcel(selectedSchedule, file.getAbsolutePath());
                        break;
                    case "PDF":
                        ExportService.exportToPDF(selectedSchedule, file.getAbsolutePath());
                        break;
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule exported successfully to " + file.getAbsolutePath());
                closeWindow();
            } catch (IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Export Error", "Could not export the schedule: " + ex.getMessage());
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
        Stage stage = (Stage) exportButton.getScene().getWindow();
        stage.close();
    }
}
