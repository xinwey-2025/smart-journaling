package main.java.com.journalapp.controller;

package main.java.com.journalapp.controller;

import main.java.com.journalapp.model.Entry;
import main.java.com.journalapp.util.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    private Runnable onWriteNow;

    // Data Variables
    private int currentStreak = 0;
    private int totalEntriesCount = 0; // New Variable
    private String weeklyMoodText = "No Data";
    private String weeklyMoodEmoji = "😐";
    private Map<LocalDate, Boolean> last7DaysActivity = new HashMap<>();
    private List<Entry> recentEntries = new ArrayList<>(); // New List for preview

    public void setOnWriteNow(Runnable action) {
        this.onWriteNow = action;
    }

    public VBox getView() {
        // Pull Data
        calculateRealData();

        // Main Controller
        VBox mainLayout = new VBox(30); // More spacing between rows
        mainLayout.setPadding(new Insets(40, 50, 40, 50));
        mainLayout.setAlignment(Pos.TOP_LEFT);
        mainLayout.setStyle("-fx-background-color: transparent;");

        // Header
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Greeting
        VBox greetingBox = new VBox(5);
        String currentUsername = (Session.getUsername() != null) ? Session.getUsername() : "Friend";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"));

        Label dateLabel = new Label(dateStr.toUpperCase());
        dateLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        dateLabel.setTextFill(Color.web("#7f8c8d"));

        Label greetingLabel = new Label("Good morning, " + currentUsername + ".");
        greetingLabel.setFont(Font.font("Georgia", FontWeight.NORMAL, 32));
        greetingLabel.setTextFill(Color.web("#2d3436"));

        greetingBox.getChildren().addAll(dateLabel, greetingLabel);

        // Button (Pushed to the right)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button writeNowBtn = new Button("+ Write New Entry");
        writeNowBtn.setStyle(
                "-fx-background-color: #2d3436; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-background-radius: 30; " +
                        "-fx-cursor: hand;"
        );
        writeNowBtn.setOnAction(e -> {
            if (onWriteNow != null) onWriteNow.run();
        });

        headerRow.getChildren().addAll(greetingBox, spacer, writeNowBtn);

        // Statistic Cards
        HBox statsContainer = new HBox(20); // Gap between cards
        statsContainer.setAlignment(Pos.CENTER_LEFT);

        // Add the 3 Cards
        statsContainer.getChildren().addAll(
                createStreakCard(),
                createMoodCard(),
                createTotalCard() // NEW CARD
        );

        // Recent Activity
        VBox recentSection = new VBox(15);
        Label recentTitle = new Label("Recent Activity");
        recentTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        recentTitle.setTextFill(Color.web("#2d3436"));

        VBox recentList = createRecentActivityList();
        recentSection.getChildren().addAll(recentTitle, recentList);

        // Assemble Final View
        mainLayout.getChildren().addAll(headerRow, statsContainer, recentSection);

        // Add a ScrollPane in case the history gets long (Optional, but good for safety)
        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Return a VBox wrapping the scrollpane to match your original return type
        VBox root = new VBox(scrollPane);
        root.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return root;
    }

    // Calculate Data
    private void calculateRealData() {
        if (!Session.hasActiveUser()) return;

        ArrayList<Entry> entries = Session.listEntries();

        if (entries == null || entries.isEmpty()) {
            return;
        }

        // Sort entries by Date (Newest first)
        entries.sort((e1, e2) -> e2.getDate().compareTo(e1.getDate()));

        // Total Count
        totalEntriesCount = entries.size();

        // Recent List
        recentEntries = entries.stream().limit(3).collect(Collectors.toList());

        // Streak & Mood Logic
        List<LocalDate> dates = new ArrayList<>();
        List<String> recentMoods = new ArrayList<>();

        for (Entry e : entries) {
            LocalDate d = e.getDate();
            dates.add(d);
            if (d.isAfter(LocalDate.now().minusDays(7))) {
                recentMoods.add(e.getMood());
            }
        }

        calculateStreak(dates);
        analyzeMoods(recentMoods);
    }

    private void calculateStreak(List<LocalDate> dates) {
        List<LocalDate> uniqueDates = dates.stream().distinct().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        currentStreak = 0;

        // Fill dots
        for (int i = 0; i < 7; i++) {
            last7DaysActivity.put(LocalDate.now().minusDays(i), uniqueDates.contains(LocalDate.now().minusDays(i)));
        }

        if (!uniqueDates.isEmpty()) {
            LocalDate cursor = LocalDate.now();
            if (uniqueDates.contains(cursor)) {
                currentStreak++;
                cursor = cursor.minusDays(1);
            } else if (uniqueDates.contains(cursor.minusDays(1))) {
                cursor = cursor.minusDays(1);
            }
            while (uniqueDates.contains(cursor)) {
                currentStreak++;
                cursor = cursor.minusDays(1);
            }
        }
    }

    private void analyzeMoods(List<String> moods) {
        if (moods.isEmpty()) {
            weeklyMoodText = "Neutral"; weeklyMoodEmoji = "😐"; return;
        }
        Map<String, Integer> freq = new HashMap<>();
        for (String m : moods) freq.put(m, freq.getOrDefault(m, 0) + 1);
        String mostCommon = Collections.max(freq.entrySet(), Map.Entry.comparingByValue()).getKey();

        weeklyMoodText = mostCommon;
        String lower = mostCommon.toLowerCase();
        if(lower.contains("very positive")) weeklyMoodEmoji = "😁";
        else if(lower.contains("positive")) weeklyMoodEmoji = "😊";
        else if(lower.contains("negative")) weeklyMoodEmoji = "😔";
        else weeklyMoodEmoji = "😐";
    }

    // UI BUILDERS (The Cards)

    // Streak Card
    private VBox createStreakCard() {
        VBox card = createBaseCard();
        card.setPrefWidth(260);

        HBox header = new HBox();
        Label title = new Label("Writing Streak");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#b2bec3"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label count = new Label(currentStreak + " Days");
        count.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        count.setTextFill(Color.web("#e67e22")); // Orange
        header.getChildren().addAll(title, spacer, count);

        Label icon = new Label("🔥");
        icon.setFont(Font.font(24));
        HBox iconRow = new HBox(icon);
        iconRow.setAlignment(Pos.CENTER);

        HBox dotsBox = new HBox(12);
        dotsBox.setAlignment(Pos.CENTER);

        // Create 7 dots
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            VBox dayCol = new VBox(5);
            dayCol.setAlignment(Pos.CENTER);

            // The Dot
            Circle dot = new Circle(6);
            if (last7DaysActivity.getOrDefault(d, false)) {
                dot.setFill(Color.web("#2ecc71")); // Green
            } else {
                dot.setFill(Color.web("#dfe6e9")); // Light Grey
            }

            // The Letter to Represent Day
            String dayLetter = d.format(DateTimeFormatter.ofPattern("E")).substring(0,1);
            Label letterLbl = new Label(dayLetter);
            letterLbl.setFont(Font.font("Segoe UI", 10));
            letterLbl.setTextFill(Color.web("#b2bec3")); // Grey text

            dayCol.getChildren().addAll(dot, letterLbl);
            dotsBox.getChildren().add(dayCol);
        }

        card.getChildren().addAll(header, iconRow, dotsBox);
        return card;
    }

    // Mood Card
    private VBox createMoodCard() {
        VBox card = createBaseCard();
        card.setPrefWidth(200);

        Label title = new Label("Weekly Vibe");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#b2bec3"));

        Label icon = new Label(weeklyMoodEmoji);
        icon.setStyle("-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Arial'; " +
                "-fx-font-size: 48px; " +
                "-fx-text-fill: black;");

        icon.setPadding(new Insets(10,0,10,0));
        icon.setAlignment(Pos.CENTER);

        // The Mood Text
        Label text = new Label(weeklyMoodText);
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        // Change text color based on the mood
        String lowerMood = weeklyMoodText.toLowerCase();
        if (lowerMood.contains("positive")) {
            text.setTextFill(Color.web("#27ae60")); // Green
        } else if (lowerMood.contains("negative")) {
            text.setTextFill(Color.web("#e74c3c")); // Red/Pink
        } else {
            text.setTextFill(Color.web("#f39c12")); // Orange (Neutral)
        }

        card.getChildren().addAll(title, icon, text);
        return card;
    }

    // New Total Card
    private VBox createTotalCard() {
        VBox card = createBaseCard();
        card.setPrefWidth(200);

        Label title = new Label("Total Entries");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#b2bec3"));

        Label count = new Label(String.valueOf(totalEntriesCount));
        count.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        count.setTextFill(Color.web("#0984e3")); // Blue

        Label sub = new Label("Lifetime memories");
        sub.setFont(Font.font("Segoe UI", 10));
        sub.setTextFill(Color.GRAY);

        card.getChildren().addAll(title, count, sub);
        return card;
    }

    // Setting for Common Card Styles
    private VBox createBaseCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefHeight(140);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15;");

        // JavaFX DropShadow Effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.1));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        card.setEffect(shadow);

        return card;
    }

    // Recent Activities List
    private VBox createRecentActivityList() {
        VBox list = new VBox(10);

        if (recentEntries.isEmpty()) {
            Label empty = new Label("No recent activity. Start writing!");
            empty.setTextFill(Color.GRAY);
            list.getChildren().add(empty);
            return list;
        }

        for (Entry e : recentEntries) {
            HBox row = new HBox(15);
            row.setPadding(new Insets(15));
            row.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #f1f2f6; -fx-border-radius: 10;");
            row.setAlignment(Pos.CENTER_LEFT);

            // Date Badge
            VBox dateBox = new VBox();
            dateBox.setAlignment(Pos.CENTER);
            dateBox.setPrefWidth(50);
            Label dayNum = new Label(String.valueOf(e.getDate().getDayOfMonth()));
            dayNum.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            dayNum.setTextFill(Color.web("#2d3436"));
            Label month = new Label(e.getDate().format(DateTimeFormatter.ofPattern("MMM")).toUpperCase());
            month.setFont(Font.font("Segoe UI", 10));
            month.setTextFill(Color.GRAY);
            dateBox.getChildren().addAll(dayNum, month);

            // Content Preview
            VBox contentBox = new VBox(5);
            Label contentText = new Label(e.getContent());
            contentText.setFont(Font.font("Segoe UI", 14));
            contentText.setTextFill(Color.web("#2d3436"));
            contentText.setWrapText(false); // One line only
            // Truncate text if too long
            if (e.getContent().length() > 60) {
                contentText.setText(e.getContent().substring(0, 60) + "...");
            }

            Label meta = new Label(e.getMood() + " • " + e.getWeather());
            meta.setFont(Font.font("Segoe UI", 10));
            meta.setTextFill(Color.web("#b2bec3"));

            contentBox.getChildren().addAll(contentText, meta);

            row.getChildren().addAll(dateBox, contentBox);
            list.getChildren().add(row);
        }
        return list;
    }
}