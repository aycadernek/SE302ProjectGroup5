package com.examify;

import com.examify.controller.MainController;
import com.examify.model.DatabaseConnection;
import com.examify.model.ScheduleManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private DatabaseConnection dbConnection;

    @Override
    public void start(Stage stage) {
        try {
            dbConnection = DatabaseConnection.getInstance();
            ScheduleManager scheduleManager = new ScheduleManager(dbConnection);

            Locale defaultLocale = Locale.getDefault();
            ResourceBundle bundle = ResourceBundle.getBundle("com.examify.resources.lang.lang", defaultLocale);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/examify/resources/views/Main.fxml"), bundle);
            Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setPrimaryStage(stage);
            controller.setScheduleManager(scheduleManager);
            controller.postInitialize(); 

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/examify/resources/styles/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Examify");
            stage.setWidth(900);
            stage.setHeight(600);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.show();

            logger.info("Examify application started successfully.");

        } catch (Exception e) {
            logger.error("Failed to start Examify application", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Could not start the application.");
            alert.setContentText("An unexpected error occurred. Please see the logs for more details.");
            alert.showAndWait();
        }
    }

    @Override
    public void stop() {
        logger.info("Examify application stopping.");
        if (dbConnection != null) {
            dbConnection.close();
        }
    }

    public static void main(String[] args) {
        System.setProperty("logback.configurationFile", "logback.xml");
        launch(args);
    }
}

class Alert extends javafx.scene.control.Alert {
    public Alert(AlertType alertType) {
        super(alertType);
    }
}