package com.examify.controller;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

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

        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            if (is == null) {
                helpManualWebView.getEngine().loadContent("Help Manual content could not be loaded. Path: " + fileName);
                return;
            }
            String html = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
            helpManualWebView.getEngine().loadContent(html);
        } catch (Exception e) {
            e.printStackTrace();
            helpManualWebView.getEngine().loadContent("Error loading Help Manual content.");
        }
    }
}
