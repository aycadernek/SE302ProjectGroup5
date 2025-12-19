package com.examify.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HelpManualController {

    @FXML
    private TextArea helpManualTextArea;

    @FXML
    private ResourceBundle resources;

    @FXML
    public void initialize() {
        String lang = resources.getLocale().getLanguage();
        String fileName = lang.equals("tr") ? "/com/examify/resources/help/help_manual_content_tr.txt" : "/com/examify/resources/help/help_manual_content.txt";

        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            if (is == null) {
                helpManualTextArea.setText("Help Manual content could not be loaded. Path: " + fileName);
                return;
            }
            String text = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
            helpManualTextArea.setText(text);
        } catch (Exception e) {
            e.printStackTrace();
            helpManualTextArea.setText("Error loading Help Manual content.");
        }
    }
}
