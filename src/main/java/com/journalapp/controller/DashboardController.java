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
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Tooltip;
import java.time.DayOfWeek;

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
    private Map<LocalDate, String> moodHistory = new HashMap<>();
    private Set<Integer> availableYears = new HashSet<>();
    private int selectedYear = LocalDate.now().getYear();

    private GridPane graphGrid;
    private ComboBox<Integer> yearSelector;

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
        String defaultStyle = "-fx-background-color: #2d3436; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10 20 10 20; " +
                "-fx-background-radius: 30; " +
                "-fx-cursor: hand;";

        String hoverStyle = "-fx-background-color: #636e72; " + // Lighter Grey
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10 20 10 20; " +
                "-fx-background-radius: 30; " +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 3);";

        writeNowBtn.setStyle(defaultStyle);

        writeNowBtn.setOnMouseEntered(e -> writeNowBtn.setStyle(hoverStyle));
        writeNowBtn.setOnMouseExited(e -> writeNowBtn.setStyle(defaultStyle));

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

        // --- NEW GRAPH SECTION ---
        VBox graphSection = new VBox(15);

        // Header: Title + Year Selector
        HBox graphHeader = new HBox(15);
        graphHeader.setAlignment(Pos.CENTER_LEFT);

        Label graphTitle = new Label("Mood Activity");
        graphTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        graphTitle.setTextFill(Color.web("#2d3436"));

        Region graphSpacer = new Region();
        HBox.setHgrow(graphSpacer, Priority.ALWAYS);

        // Create Year Selector
        yearSelector = new ComboBox<>();
        yearSelector.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-radius: 5;");

        // Fill selector with data found in calculateRealData
        updateYearSelectorItems();
        yearSelector.setValue(selectedYear);

        // Action: Redraw graph when year changes
        yearSelector.setOnAction(e -> {
            if (yearSelector.getValue() != null) {
                selectedYear = yearSelector.getValue();
                drawCalendarGraph(selectedYear);
            }
        });

        graphHeader.getChildren().addAll(graphTitle, graphSpacer, new Label("Year: "), yearSelector);

        // Create the Graph Container and Draw Initial Data
        VBox moodGraphContainer = createMoodGraph();
        drawCalendarGraph(selectedYear);

        graphSection.getChildren().addAll(graphHeader, moodGraphContainer);

        // Assemble Final View
        mainLayout.getChildren().addAll(headerRow, statsContainer, graphSection);
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

        // Initialize available years with current year so it's never empty
        availableYears.clear();
        availableYears.add(LocalDate.now().getYear());
        moodHistory.clear();

        for (Entry e : entries) {
            LocalDate d = e.getDate();
            dates.add(d);

            // Save mood and year
            moodHistory.put(d, e.getMood());
            availableYears.add(d.getYear());

            if (d.isAfter(LocalDate.now().minusDays(7))) {
                recentMoods.add(e.getMood());
            }
        }

        // Update the dropdown if it exists
        if (yearSelector != null) {
            updateYearSelectorItems();
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

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.1));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        card.setEffect(shadow);

        return card;
    }

    // Method for graph
    private void updateYearSelectorItems() {
        if (yearSelector == null) return;
        List<Integer> sortedYears = new ArrayList<>(availableYears);
        sortedYears.sort(Collections.reverseOrder()); // Newest first
        yearSelector.setItems(FXCollections.observableArrayList(sortedYears));
    }

    private VBox createMoodGraph() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        // Wrapper: Holds Fixed Labels (Left) + Scrollable Graph (Right)
        container.setMinHeight(250);
        HBox graphWrapper = new HBox(10);
        graphWrapper.setAlignment(Pos.TOP_LEFT);

        // Define Grid Rules
        RowConstraints headerRowConstraint = new RowConstraints(15);
        headerRowConstraint.setValignment(javafx.geometry.VPos.BOTTOM);

        // Rule 2: Day Rows (The Pixels) - Fixed height 12px (matches rectangle height)
        RowConstraints dayRowConstraint = new RowConstraints(12);
        dayRowConstraint.setValignment(javafx.geometry.VPos.CENTER);

        // Fixed Day Labels
        GridPane labelsGrid = new GridPane();
        labelsGrid.setVgap(7);
        labelsGrid.setPadding(new Insets(0, 5, 0, 0));
        labelsGrid.setMinWidth(30); // Prevent shrinking

        // Apply Rules to Left Grid
        labelsGrid.getRowConstraints().add(headerRowConstraint); // Row 0
        for (int i = 0; i < 7; i++) labelsGrid.getRowConstraints().add(dayRowConstraint); // Rows 1-7

        // Invisible Spacer for Row 0
        Label spacer = new Label("Jan");
        spacer.setFont(Font.font("Segoe UI", 10));
        spacer.setVisible(false);
        labelsGrid.add(spacer, 0, 0);

        // Add Day Labels (Sun to Sat) with vertical centering
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(days[i]);
            lbl.setFont(Font.font("Segoe UI", 10));
            lbl.setTextFill(Color.web("#b2bec3"));
            // Ensure the text itself is centered in its 12px slot
            lbl.setMaxHeight(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER_LEFT);
            labelsGrid.add(lbl, 0, i + 1);
        }

        // Scroll Graph
        graphGrid = new GridPane();
        graphGrid.setHgap(7);
        graphGrid.setVgap(7);
        graphGrid.setAlignment(Pos.TOP_LEFT);
        graphGrid.setPadding(new Insets(0, 0, 20, 0)); // Add 20px of buffer

        graphGrid.getRowConstraints().add(headerRowConstraint); // Row 0
        for (int i = 0; i < 7; i++) graphGrid.getRowConstraints().add(dayRowConstraint); // Rows 1-7

        ScrollPane gridScrollPane = new ScrollPane(graphGrid);
        gridScrollPane.setFitToHeight(true);
        gridScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gridScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        gridScrollPane.setMinHeight(160);
        gridScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        HBox.setHgrow(labelsGrid, Priority.NEVER);
        HBox.setHgrow(gridScrollPane, Priority.ALWAYS);

        graphWrapper.getChildren().addAll(labelsGrid, gridScrollPane);
        container.getChildren().addAll(graphWrapper, createLegend());
        return container;
    }

    private void drawCalendarGraph(int year) {
        if (graphGrid == null) return;
        graphGrid.getChildren().clear(); // Reset grid

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        // Variables to track grid positioning
        int column = 0;
        LocalDate cursor = start;
        java.time.Month currentMonth = null;

        while (!cursor.isAfter(end)) {
            int dayOfWeek = cursor.getDayOfWeek().getValue();
            int row = (dayOfWeek == 7) ? 0 : dayOfWeek;

            if (cursor.getMonth() != currentMonth) {
                currentMonth = cursor.getMonth();
                // Only draw if not too close to the right edge
                if (column < 51) {
                    Label monthLabel = new Label(currentMonth.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH));
                    monthLabel.setFont(Font.font("Segoe UI", 10));
                    monthLabel.setTextFill(Color.GRAY);

                    graphGrid.add(monthLabel, column, 0);
                    GridPane.setColumnSpan(monthLabel, 2);
                }
            }

            Rectangle rect = new Rectangle(12, 12);
            rect.setArcWidth(3); rect.setArcHeight(3);

            String mood = moodHistory.getOrDefault(cursor, "No Data");
            rect.setFill(getColorForMood(mood));

            Tooltip.install(rect, new Tooltip(cursor.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + "\n" + mood));
            graphGrid.add(rect, column, row + 1);
            if (row == 6) {
                column++;
            }
            cursor = cursor.plusDays(1);
        }
    }

    private Color getColorForMood(String mood) {
        if (mood == null || mood.equals("No Data")) return Color.web("#ebedf0");
        String lower = mood.toLowerCase();

        if (lower.contains("very positive")) return Color.web("#2ecc71");      // Green
        else if (lower.contains("very negative")) return Color.web("#e74c3c"); // Red
        else if (lower.contains("positive")) return Color.web("#3498db");      // Blue
        else if (lower.contains("negative")) return Color.web("#fd79a8");      // Pink
        else if (lower.contains("neutral")) return Color.web("#f1c40f");       // Yellow
        return Color.web("#ebedf0");
    }

    private HBox createLegend() {
        HBox legend = new HBox(15);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.setPadding(new Insets(10, 0, 0, 0));

        legend.getChildren().add(createLegendItem("Very +ve", "#2ecc71"));
        legend.getChildren().add(createLegendItem("Positive", "#3498db"));
        legend.getChildren().add(createLegendItem("Neutral", "#f1c40f"));
        legend.getChildren().add(createLegendItem("Negative", "#fd79a8"));
        legend.getChildren().add(createLegendItem("Very -ve", "#e74c3c"));
        legend.getChildren().add(createLegendItem("No Data", "#ebedf0"));
        return legend;
    }

    private HBox createLegendItem(String text, String colorHex) {
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER);
        Rectangle box = new Rectangle(10, 10, Color.web(colorHex));
        box.setArcWidth(2); box.setArcHeight(2);
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", 10));
        lbl.setTextFill(Color.GRAY);
        item.getChildren().addAll(box, lbl);
        return item;
    }
}