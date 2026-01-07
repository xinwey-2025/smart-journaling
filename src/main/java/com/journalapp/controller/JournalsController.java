package main.java.com.journalapp.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

// Imports for Data
import main.java.com.journalapp.util.Session;
import main.java.com.journalapp.model.Entry;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class JournalsController {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Trigger Listener by clicking Edit
    private Consumer<Entry> onEditListener;

    // Method to let the Main app connect the "Edit" action
    public void setOnEditAction(Consumer<Entry> action) {
        this.onEditListener = action;
    }

    public VBox getView() {
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(30, 50, 30, 50));

        // Title
        Label pageTitle = new Label("Journals");
        pageTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: 300; -fx-text-fill: #333333;");

        Separator separator = new Separator();
        separator.setOpacity(0.4);

        // Scroll Pane (Transparent)
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Container for the Cards
        VBox journalListContainer = new VBox(15);
        journalListContainer.setPadding(new Insets(10, 0, 0, 0));
        journalListContainer.setStyle("-fx-background-color: transparent;");

        // Fetch Real Data
        if (Session.hasActiveUser()) {
            List<Entry> myEntries = Session.listEntries();

            // Sort: Newest First
            if (myEntries != null) {
                Collections.reverse(myEntries);

                for (Entry entry : myEntries) {
                    journalListContainer.getChildren().add(createJournalCard(entry));
                }
            }
        } else {
            journalListContainer.getChildren().add(new Label("Please log in to see your journals."));
        }

        scrollPane.setContent(journalListContainer);
        contentBox.getChildren().addAll(pageTitle, separator, scrollPane);

        return contentBox;
    }

    private VBox createJournalCard(Entry entry) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15, 20, 15, 20));

        // Glassy Style
        String styleNormal = "-fx-background-color: rgba(255, 255, 255, 0.4);" +
                "-fx-background-radius: 12px;" +
                "-fx-border-color: rgba(255, 255, 255, 0.6);" +
                "-fx-border-radius: 12px;";

        String styleHover = "-fx-background-color: rgba(255, 255, 255, 0.7);" +
                "-fx-background-radius: 12px;" +
                "-fx-border-color: #fff;" +
                "-fx-border-radius: 12px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);";

        card.setStyle(styleNormal);

        // Hover Effects
        card.setOnMouseEntered(e -> card.setStyle(styleHover));
        card.setOnMouseExited(e -> card.setStyle(styleNormal));

        // HEADER (Date + Button)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label(entry.getDate().format(dateFormatter));
        dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label metaLabel = new Label(entry.getMood() + " | " + entry.getWeather());
        metaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        // Edit Btn
        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand; -fx-background-radius: 5;");

        // Trigger the Listener when clicked
        editBtn.setOnAction(e -> {
            if (onEditListener != null) {
                onEditListener.accept(entry);
            }
        });

        header.getChildren().addAll(dateLabel, spacer, metaLabel, editBtn);

        // Content Preview
        String previewText = entry.getContent().replace("\n", " ");
        if (previewText.length() > 80) previewText = previewText.substring(0, 80) + "...";

        Label contentLabel = new Label(previewText);
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: #444; -fx-font-size: 13px;");

        card.getChildren().addAll(header, contentLabel);
        return card;
    }
}