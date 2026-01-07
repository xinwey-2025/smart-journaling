package main.java.com.journalapp.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

// Import your Weather utility
import main.java.com.journalapp.util.Session;
import main.java.com.journalapp.util.Weather;
import main.java.com.journalapp.util.MoodAnalyzer;
import main.java.com.journalapp.model.Entry;

public class EntryEditorController {

    // Variable to store weather so we can save it later
    private String lastFetchedWeather = "Unknown";
    private Entry entryToEdit = null;

    private TextArea textArea;
    private Label header;
    private Button save;

    // Call to Start Editing Journals
    public void setEntryToEdit(Entry entry) {
        this.entryToEdit = entry;
    }

    public VBox getView() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.setPadding(new Insets(40, 50, 40, 50));

        // Header
        Label header = new Label("What's on your mind?");
        header.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        header.setStyle("-fx-text-fill: #333;");

        // Info Line (Date & Weather)
        LocalDate today = LocalDate.now();
        Label prompt = new Label("Entry for " + today + ":");
        prompt.setFont(Font.font("Arial", 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Weather Logic
        // Initial state
        Label condition = new Label("Weather: Loading...");
        condition.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        condition.setStyle("-fx-text-fill: #666;");

        // Run the API call in Background so the app won't freeze
        if (entryToEdit != null) {
            lastFetchedWeather = entryToEdit.getWeather();
            condition.setText("Weather: " + lastFetchedWeather);
        }
        else {
            CompletableFuture.runAsync(() -> {
                String w = Weather.getCurrentWeather();
                Platform.runLater(() -> {
                    // Only update if we are still in "New" mode
                    if (entryToEdit == null) {
                        lastFetchedWeather = w;
                        condition.setText("Weather: " + w);
                    }
                });
            });
        }

        // Text Area
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPromptText("Start writing your thoughts here...");

        // Pre-fill text idf editing
        if (entryToEdit != null) {
            textArea.setText(entryToEdit.getContent());
        }

        // Glassy Style
        textArea.setStyle(
                "-fx-control-inner-background: rgba(255,255,255,0.5);" +
                        "-fx-background-color: transparent;" +
                        "-fx-font-family: 'Arial';" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-color: rgba(255,255,255,0.6);" +
                        "-fx-border-radius: 5;"
        );

        VBox.setVgrow(textArea, Priority.ALWAYS);

        // Save Button
        save = new Button("Save Entry");
        save.setPrefWidth(150);
        save.setPrefHeight(35);
        save.setStyle(
                "-fx-background-color: #3498db;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);"
        );

        HBox infoLine = new HBox(10);
        infoLine.getChildren().addAll(prompt, spacer, condition);

        // Save Action
        save.setOnAction(e -> {
            System.out.println("--- STARTING SAVE PROCESS ---");

            String content = textArea.getText().trim();
            if (content.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please write something first!").showAndWait();
                return;
            }

            if (!Session.hasActiveUser()) {
                new Alert(Alert.AlertType.ERROR, "You are not logged in.").showAndWait();
                return;
            }

            // Disable UI
            save.setDisable(true);
            save.setText("Analyzing...");

            // Background Analysis
            CompletableFuture.supplyAsync(() -> {
                try {
                    return MoodAnalyzer.analyze(content);
                } catch (Exception ex) {
                    System.err.println("Mood Analysis Failed: " + ex.getMessage());
                    return "Neutral";
                }
            }).thenAccept(detectedMood -> {
                Platform.runLater(() -> {
                    String finalMood = (detectedMood == null || detectedMood.isEmpty()) ? "Neutral" : detectedMood;

                    try {
                        // --- BRANCHING LOGIC: EDIT OR CREATE? ---
                        if (entryToEdit != null) {
                            // OPTION A: UPDATE EXISTING
                            // We keep the original ID and original Date
                            Session.editEntry(
                                    entryToEdit.getId(),
                                    entryToEdit.getDate(), // keep original date
                                    content,
                                    finalMood,
                                    lastFetchedWeather // keep original weather
                            );

                            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Updated successfully!");
                            alert.showAndWait();

                        } else {
                            // OPTION B: CREATE NEW
                            Session.createEntry(LocalDate.now(), content, finalMood, lastFetchedWeather);

                            Alert alert = new Alert(Alert.AlertType.INFORMATION, "New entry saved!");
                            alert.showAndWait();
                            textArea.clear(); // Only clear if it was a new entry
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Save Failed: " + ex.getMessage()).showAndWait();
                    } finally {
                        // Restore Button
                        save.setText(entryToEdit != null ? "Update Entry" : "Save Entry");
                        save.setDisable(false);
                    }
                });
            });
        });

        layout.getChildren().addAll(header, infoLine, textArea, save);
        return layout;
    }
}