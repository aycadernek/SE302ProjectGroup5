package com.examify.controller;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HelpManualController {

    @FXML
    private WebView helpManualWebView;

    @FXML
    private ResourceBundle resources;

    @FXML
    public void initialize() {
        String lang = resources.getLocale().getLanguage();
        String fileName = lang.equals("tr") ? "/com/examify/resources/help/help_manual_content_tr.html" : "/com/examify/resources/help/help_manual_content.html";

        helpManualWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) helpManualWebView.getEngine().executeScript("window");
                window.setMember("javaApp", this);
            }
        });

        helpManualWebView.getEngine().loadContent(getHtmlContent(fileName));
    }

    private String getHtmlContent(String fileName) {
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            if (is == null) return "Help Manual content could not be loaded.";
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "Error loading content.";
        }
    }
    
    public void download(String fileName) {
        String resourcePath = "/com/examify/resources/example_data/" + fileName;
        
        javafx.application.Platform.runLater(() -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Save Sample File");
            fileChooser.setInitialFileName(fileName);
            
            java.io.File dest = fileChooser.showSaveDialog(helpManualWebView.getScene().getWindow());
            if (dest != null) {
                try (InputStream is = getClass().getResourceAsStream(resourcePath);
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                    
                    if (is == null) {
                        throw new java.io.IOException("Resource not found: " + resourcePath);
                    }
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Download Successful");
                    alert.setHeaderText(null);
                    alert.setContentText("File saved to: " + dest.getAbsolutePath());
                    alert.showAndWait();
                    
                } catch (Exception e) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Download Failed");
                    alert.setContentText("Error saving file: " + e.getMessage());
                    alert.showAndWait();
                }
            }
        });
    }
}
