package com.examify.controller;

import com.examify.model.ScheduleManager;
import com.examify.model.SearchCriteria;
import com.examify.model.entities.Course;
import com.examify.model.entities.Exam;
import com.examify.model.entities.Schedule;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringJoiner;

public class MainController {

    @FXML private ResourceBundle resources;

    @FXML private ComboBox<String> searchTypeCombo;
    @FXML private TextField searchField;
    @FXML private Button btnSearch;
    @FXML private ComboBox<Schedule> scheduleCombo;

    @FXML private TableView<Exam> scheduleTable;
    @FXML private TableColumn<Exam, String> colCourse;
    @FXML private TableColumn<Exam, String> colDate;
    @FXML private TableColumn<Exam, String> colSlot;
    @FXML private TableColumn<Exam, String> colClassroom;
    @FXML private TableColumn<Exam, Void> colStudents;

    @FXML private Menu menuSchedule;
    @FXML private Menu menuLanguage;
    @FXML private Menu menuHelp;
    @FXML private MenuItem menuNewSchedule;
    @FXML private MenuItem btnEditSchedule;
    @FXML private MenuItem btnDeleteSchedule;
    @FXML private MenuItem btnExportSchedule;
    @FXML private MenuItem btnHelpItem;
    @FXML private MenuItem btnHelpItem1;
    @FXML private RadioMenuItem btnTurkishLang;
    @FXML private RadioMenuItem btnEnglishLang;

    @FXML private Label relatedDataLabel;
    @FXML private Label dateIntervalLabel;
    @FXML private Label slotNumberLabel;
    @FXML private Label classroomListLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label numCoursesLabel;
    @FXML private Label resolvedConflictsLabel;

    @FXML private Label dateIntervalValue;
    @FXML private Label slotNumberValue;
    @FXML private Label classroomListValue;
    @FXML private Label totalStudentsValue;
    @FXML private Label numCoursesValue;
    @FXML private Label resolvedConflictsValue;

    private ScheduleManager scheduleManager;
    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setScheduleManager(ScheduleManager scheduleManager) {
        this.scheduleManager = scheduleManager;
    }

    @FXML
    public void initialize() {
        setupLanguageMenu();
        setupTable();
        setupEventHandlers();
        populateSearchTypes();
    }
    
    public void postInitialize() {
        refreshData();
    }

    private void setupLanguageMenu() {
        ToggleGroup langGroup = new ToggleGroup();
        btnTurkishLang.setToggleGroup(langGroup);
        btnEnglishLang.setToggleGroup(langGroup);

        if (resources.getLocale().getLanguage().equals("tr")) {
            btnTurkishLang.setSelected(true);
        } else {
            btnEnglishLang.setSelected(true);
        }

        langGroup.selectedToggleProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                String lang = ((RadioMenuItem) newValue) == btnTurkishLang ? "tr" : "en";
                loadLanguage(lang);
            }
        });
    }
    
    private void setupTable() {
        scheduleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colCourse.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCourseCode()));
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getExamDate().toString()));
        colSlot.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getSlot())));
        colClassroom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getClassroomId()));

        colStudents.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();

            {
                btn.setOnAction(e -> {
                    Exam exam = getTableView().getItems().get(getIndex());
                    openStudentsPopup(exam);
                });
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    btn.setText(resources.getString("button.show"));
                    setGraphic(btn);
                }
            }
        });
    }

    private void setupEventHandlers() {
        scheduleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Schedule schedule) {
                return schedule == null ? "" : schedule.getName();
            }

            @Override
            public Schedule fromString(String string) {
                return scheduleCombo.getItems().stream().filter(s -> s.getName().equals(string)).findFirst().orElse(null);
            }
        });

        scheduleCombo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue != null) {
                scheduleManager.setCurrentSchedule(newValue);
                populateInfoPanel(newValue);
                scheduleTable.setItems(FXCollections.observableArrayList(newValue.getExams()));
            }
        });

        menuNewSchedule.setOnAction(e -> openNewSchedulePopup());
        btnEditSchedule.setOnAction(e -> openEditSchedulePopup());
        btnDeleteSchedule.setOnAction(e -> openDeleteSchedulePopup());
        btnExportSchedule.setOnAction(e -> openExportSchedulePopup());
        btnHelpItem.setOnAction(e -> openHelpPopup());
        btnHelpItem1.setOnAction(e -> openHelpManualPopup());
        btnSearch.setOnAction(e -> handleSearch());
    }

    public void refreshData() {
        Schedule selected = scheduleCombo.getValue();
        scheduleCombo.setItems(FXCollections.observableArrayList(scheduleManager.getAllSchedules()));
        if (selected != null) {
            scheduleCombo.getSelectionModel().select(selected);
        } else {
            scheduleCombo.getSelectionModel().selectFirst();
        }
        
        Schedule current = scheduleManager.getCurrentSchedule();
        if(current != null){
            scheduleTable.setItems(FXCollections.observableArrayList(current.getExams()));
            populateInfoPanel(current);
        }
    }

    private void loadLanguage(String lang) {
        this.resources = ResourceBundle.getBundle("com.examify.resources.lang.lang", new Locale(lang));
        updateTexts();
    }

    private void updateTexts() {
        Schedule selectedSchedule = scheduleCombo.getValue();
        int selectedSearchIndex = searchTypeCombo.getSelectionModel().getSelectedIndex();

        menuSchedule.setText(resources.getString("menu.schedule"));
        menuLanguage.setText(resources.getString("menu.language"));
        menuHelp.setText(resources.getString("menu.help"));
        menuNewSchedule.setText(resources.getString("menu.newSchedule"));
        btnEditSchedule.setText(resources.getString("menu.editSchedule"));
        btnDeleteSchedule.setText(resources.getString("menu.deleteSchedule"));
        btnExportSchedule.setText(resources.getString("menu.exportSchedule"));
        btnHelpItem.setText(resources.getString("menu.about"));
        btnHelpItem1.setText(resources.getString("menu.helpManual"));

        colCourse.setText(resources.getString("column.courseName"));
        colClassroom.setText(resources.getString("column.classroom"));
        colSlot.setText(resources.getString("column.slot"));
        colDate.setText(resources.getString("column.date"));
        colStudents.setText(resources.getString("column.students"));

        relatedDataLabel.setText(resources.getString("label.scheduleInfo"));
        dateIntervalLabel.setText(resources.getString("label.dateInterval"));
        slotNumberLabel.setText(resources.getString("label.slotNumber"));
        classroomListLabel.setText(resources.getString("label.totalClassroomCount"));
        totalStudentsLabel.setText(resources.getString("label.totalStudents"));
        numCoursesLabel.setText(resources.getString("label.numCourses"));
        resolvedConflictsLabel.setText(resources.getString("label.resolvedConflicts"));
        
        searchField.setPromptText(resources.getString("search.prompt"));
        scheduleCombo.setPromptText(resources.getString("schedule.select"));
        searchTypeCombo.setPromptText(resources.getString("search.type"));

        populateSearchTypes();
        if(selectedSearchIndex != -1) {
            searchTypeCombo.getSelectionModel().select(selectedSearchIndex);
        }

        refreshData(); 
        if(selectedSchedule != null) {
             scheduleCombo.getSelectionModel().select(selectedSchedule);
        }

        scheduleTable.getColumns().get(0).setVisible(false);
        scheduleTable.getColumns().get(0).setVisible(true);
    }

    private void populateSearchTypes() {
        searchTypeCombo.setItems(FXCollections.observableArrayList(
                resources.getString("search.student"),
                resources.getString("search.course"),
                resources.getString("search.classroom"),
                resources.getString("search.date")
        ));
    }
    
    private void handleSearch() {
        Schedule currentSchedule = scheduleManager.getCurrentSchedule();
        if (currentSchedule == null) {
            showAlert(Alert.AlertType.WARNING, "No Schedule Selected", "Please select a schedule before searching.");
            return;
        }

        String searchTerm = searchField.getText();
        String searchType = searchTypeCombo.getValue();

        if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null) {
            scheduleTable.setItems(FXCollections.observableArrayList(currentSchedule.getExams()));
            return;
        }

        SearchCriteria.Builder builder = new SearchCriteria.Builder();
        builder.scheduleId(currentSchedule.getScheduleId());

        String searchTypeStudent = resources.getString("search.student");
        String searchTypeCourse = resources.getString("search.course");
        String searchTypeClassroom = resources.getString("search.classroom");
        String searchTypeDate = resources.getString("search.date");

        if (searchType.equals(searchTypeStudent)) {
            builder.studentId(searchTerm);
        } else if (searchType.equals(searchTypeCourse)) {
            builder.courseCode(searchTerm);
        } else if (searchType.equals(searchTypeClassroom)) {
            builder.classroomId(searchTerm);
        } else if (searchType.equals(searchTypeDate)) {
            try {
                builder.examDate(LocalDate.parse(searchTerm, DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Date", "Please use YYYY-MM-DD format for date search.");
                return;
            }
        }

        ObservableList<Exam> searchResults = FXCollections.observableArrayList(scheduleManager.searchExams(builder.build()));
        scheduleTable.setItems(searchResults);
    }
    
    private void populateInfoPanel(Schedule schedule) {
        if (schedule == null) {
            dateIntervalValue.setText("");
            slotNumberValue.setText("");
            classroomListValue.setText("");
            totalStudentsValue.setText("");
            numCoursesValue.setText("");
            resolvedConflictsValue.setText("");
            return;
        }

        dateIntervalValue.setText(schedule.getStartDate() + " / " + schedule.getEndDate());
        slotNumberValue.setText(String.valueOf(schedule.getSlotsPerDay()));
        long classroomCount = schedule.getExams().stream().map(Exam::getClassroomId).distinct().count();
        classroomListValue.setText(String.valueOf(classroomCount));
        
        numCoursesValue.setText(String.valueOf(schedule.getExams().stream().map(Exam::getCourseCode).distinct().count()));
        
        totalStudentsValue.setText("N/A");
        resolvedConflictsValue.setText("N/A");
    }

    private void openStudentsPopup(Exam exam) {
        if (exam == null) return;

        Course course = scheduleManager.getCourseWithStudents(exam.getCourseCode());

        if (course == null || course.getEnrolledStudents().isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION,
                    resources.getString("students.title"),
                    resources.getString("students.noneFound") + " " + exam.getCourseCode());
            return;
        }

        Set<String> students = course.getEnrolledStudents();
        StringJoiner studentList = new StringJoiner("\n");
        students.forEach(studentList::add);

        TextArea textArea = new TextArea(studentList.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(resources.getString("students.title"));
        alert.setHeaderText(resources.getString("students.forCourse") + " " + course.getCourseCode() + " (" + students.size() + ")");
        alert.getDialogPane().setContent(textArea);
        alert.setResizable(true);

        alert.showAndWait();
    }
    
    private void openNewSchedulePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/examify/resources/views/NewSchedule.fxml"), resources);
            Parent root = loader.load();
            
            NewScheduleController controller = loader.getController();
            controller.setScheduleManager(scheduleManager);
            controller.setMainController(this);

            showPopup(root, resources.getString("schedule.new"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openEditSchedulePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/examify/resources/views/EditSchedule.fxml"), resources);
            Parent root = loader.load();

            EditScheduleController controller = loader.getController();
            controller.setScheduleManager(scheduleManager);
            controller.setMainController(this);
            controller.loadSchedules();

            showPopup(root, resources.getString("label.editSchedule"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openDeleteSchedulePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/examify/resources/views/DeleteSchedule.fxml"), resources);
            Parent root = loader.load();

            DeleteScheduleController controller = loader.getController();
            controller.setScheduleManager(scheduleManager);
            controller.setMainController(this);
            controller.loadSchedules();

            showPopup(root, resources.getString("label.deleteSchedule"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openExportSchedulePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/examify/resources/views/ExportSchedule.fxml"), resources);
            Parent root = loader.load();

            ExportScheduleController controller = loader.getController();
            controller.setScheduleManager(scheduleManager);
            controller.loadSchedules();
            
            showPopup(root, resources.getString("label.exportSchedule"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openHelpPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/examify/resources/views/Help.fxml"), resources);
            Parent root = loader.load();
            showPopup(root, resources.getString("menu.about"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void openHelpManualPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/examify/resources/views/HelpManual.fxml"), resources);
            Parent root = loader.load();
            showPopup(root, resources.getString("menu.helpManual"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showPopup(Parent root, String title) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/examify/resources/styles/style.css").toExternalForm());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        stage.show();
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}