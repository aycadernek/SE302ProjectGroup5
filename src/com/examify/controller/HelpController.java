package com.examify.controller;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HelpController {

    @FXML
    private WebView helpWebView;

    @FXML
    private ResourceBundle resources;

    @FXML
    public void initialize() {
        String lang = resources.getLocale().getLanguage();
        String fileName = lang.equals("tr") ? "/com/examify/resources/about/about_content_tr.html" : "/com/examify/resources/about/about_content.html";

        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            if (is == null) {
                helpWebView.getEngine().loadContent("<html><body>Help content could not be loaded. Path: " + fileName + "</body></html>");
                return;
            }
            String htmlContent = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
            helpWebView.getEngine().loadContent(htmlContent);
        } catch (Exception e) {
            e.printStackTrace();
            helpWebView.getEngine().loadContent("<html><body>Error loading help content.</body></html>");
        }
    }
}
