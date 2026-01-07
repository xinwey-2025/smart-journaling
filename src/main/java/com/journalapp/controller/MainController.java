package main.java.com.journalapp.controller;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.Objects;

import main.java.com.journalapp.util.Session;

public class MainController extends Application {

    private Stage primaryStage;
    private BorderPane mainAppLayout;

    // Sidebar State
    private VBox sidebar;
    private boolean isCollapsed = false;

    // Buttons declared here so we can change them in toggleSidebar()
    private Button homeBtn, newEntryBtn, journalsBtn, logoutBtn,
            settingsBtn, collapseBtn, userBtn;

    // Views
    private LoginView loginView;
    private SignUpView signUpView;
    private DashboardController dashboardView;
    private JournalsController journalView;
    private EntryEditorController entryEditorView;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // Initialize all views
        loginView = new LoginView(this);
        signUpView = new SignUpView(this);
        dashboardView = new DashboardController();
        journalView = new JournalsController();
        entryEditorView = new EntryEditorController();

        // Connect Dashboard "Write Now" Button
        dashboardView.setOnWriteNow(() -> {
            System.out.println("Write Now clicked from Dashboard");
            showMainApp();
            openTodayEntry();
        });

        // Connect the Journal Page to Editor
        journalView.setOnEditAction(selectedEntry -> {
            System.out.println("User clicked Edit on entry: " + selectedEntry.getDate());
            entryEditorView.setEntryToEdit(selectedEntry);
            loadNewEntry();
        });

        // Login
        Scene scene = new Scene(loginView.getView(), 1000, 700);

        try {
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/resources/app_icon(1).png")));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("Icon not found.");
        }

        primaryStage.setTitle("More Hair Journaling");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Nav
    public void showLoginView() { primaryStage.getScene().setRoot(loginView.getView()); }
    public void showSignUpView() { primaryStage.getScene().setRoot(signUpView.getView()); }

    public void showMainApp() {
        if (mainAppLayout == null) createMainAppLayout();
        primaryStage.getScene().setRoot(mainAppLayout);
        loadDashboard();
    }

    public void logout() { showLoginView(); }

    private void loadDashboard() { mainAppLayout.setCenter(dashboardView.getView()); }
    private void loadJournals() { mainAppLayout.setCenter(journalView.getView()); }
    private void loadNewEntry() { mainAppLayout.setCenter(entryEditorView.getView()); }

    // Layout & Sidebar
    private void createMainAppLayout() {
        mainAppLayout = new BorderPane();
        mainAppLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #CFE3F3, #FAD0C4);");
        createSidebar();
        mainAppLayout.setLeft(sidebar);
    }

    private void createSidebar() {
        sidebar = new VBox(20);
        sidebar.setPadding(new Insets(40, 20, 40, 30));
        sidebar.setPrefWidth(220);
        sidebar.setAlignment(Pos.TOP_LEFT);

        // Define Buttons
        String currentUsername = (Session.getUsername() != null) ? Session.getUsername() : "";
        userBtn = createMenuButton2("\uD83D\uDC64", currentUsername);
        Region divider = new Region();
        divider.setStyle("-fx-border-color: rgba(0,0,0,0.1); -fx-border-width: 0 0 1 0;");
        divider.setMinHeight(10);

        homeBtn = createMenuButton("⌂", "Home");
        homeBtn.setOnAction(e -> loadDashboard());

        newEntryBtn = createMenuButton("⊕", "New Entry");
        newEntryBtn.setOnAction(e -> {
            openTodayEntry();
            loadNewEntry();
        });

        journalsBtn = createMenuButton("d", "Journals");
        journalsBtn.setOnAction(e -> loadJournals());

        logoutBtn = createMenuButton("→", "Log out");
        logoutBtn.setOnAction(e -> logout());

        settingsBtn = createMenuButton("⚙", "Settings");

        // Collapse
        collapseBtn = createMenuButton("≡", "Collapse");
        collapseBtn.setOnAction(e -> toggleSidebar());

        Region spacerMiddle = new Region();
        VBox.setVgrow(spacerMiddle, Priority.ALWAYS);

        sidebar.getChildren().addAll(
                userBtn, divider, homeBtn, newEntryBtn, journalsBtn,
                spacerMiddle,
                logoutBtn, settingsBtn,
                new Region() {{ setMinHeight(30); }},
                collapseBtn
        );
    }

    // Collapse Logic
    private void toggleSidebar() {
        isCollapsed = !isCollapsed;

        if (isCollapsed) {
            // Mini mode
            sidebar.setPrefWidth(70);
            sidebar.setAlignment(Pos.TOP_CENTER);
            sidebar.setPadding(new Insets(40, 10, 40, 10));

            // Hide text, keep icons
            updateButtonText(userBtn, "\uD83D\uDC64","");
            updateButtonText(homeBtn, "⌂", "");
            updateButtonText(newEntryBtn, "⊕", "");
            updateButtonText(journalsBtn, "d", "");
            updateButtonText(logoutBtn, "→", "");
            updateButtonText(settingsBtn, "⚙", "");

            // Change collapse btn to expand btn
            updateButtonText(collapseBtn, "»", "");
            collapseBtn.setTooltip(new Tooltip("Expand Sidebar"));

        } else {
            // Expanded
            sidebar.setPrefWidth(220);
            sidebar.setAlignment(Pos.TOP_LEFT);
            sidebar.setPadding(new Insets(40, 20, 40, 30));

            String currentUsername = (Session.getUsername() != null) ? Session.getUsername() : "";

            // Show text
            updateButtonText(userBtn, "\uD83D\uDC64", currentUsername);
            updateButtonText(homeBtn, "⌂", "Home");
            updateButtonText(newEntryBtn, "⊕", "New Entry");
            updateButtonText(journalsBtn, "d", "Journals");
            updateButtonText(logoutBtn, "→", "Log out");
            updateButtonText(settingsBtn, "⚙", "Settings");

            // Change btn back to collapse
            updateButtonText(collapseBtn, "≡", "Collapse");
            collapseBtn.setTooltip(new Tooltip("Collapse Sidebar"));
        }

        refreshButtonStyles();
    }

    // Helper to force buttons to snap to Left or Center immediately
    private void refreshButtonStyles() {
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #1a1a1a; -fx-font-size: 16px; -fx-cursor: hand;";
        String align = isCollapsed ? "CENTER" : "CENTER_LEFT";

        String fullStyle = baseStyle + "-fx-alignment: " + align + ";";

        userBtn.setStyle(fullStyle);
        homeBtn.setStyle(fullStyle);
        newEntryBtn.setStyle(fullStyle);
        journalsBtn.setStyle(fullStyle);
        logoutBtn.setStyle(fullStyle);
        settingsBtn.setStyle(fullStyle);
        collapseBtn.setStyle(fullStyle);

        // User button needs bold
        userBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2c3e50; -fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: " + align + ";");
    }

    private void updateButtonText(Button btn, String icon, String text) {
        if (text.isEmpty()) {
            btn.setText(icon); // Icon Only
        } else {
            btn.setText(icon + "   " + text); // Icon + Text
        }
    }

    private void openTodayEntry() {
        java.time.LocalDate today = java.time.LocalDate.now();

        main.java.com.journalapp.model.Entry existingEntry = null;
        java.util.ArrayList<main.java.com.journalapp.model.Entry> entries = Session.listEntries();

        if (entries != null) {
            for (main.java.com.journalapp.model.Entry e : entries) {
                if (e.getDate().equals(today)) {
                    existingEntry = e;
                    break; // Found it! Stop searching.
                }
            }
        }
        // If existingEntry is NOT null, Editor knows to "Update" instead of "Create"
        entryEditorView.setEntryToEdit(existingEntry);

        loadNewEntry();
    }

    // Set Style
    private Button createMenuButton(String iconText, String labelText) {
        Button btn = new Button(iconText + "   " + labelText);

        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #1a1a1a; -fx-font-size: 16px; -fx-cursor: hand;";
        btn.setStyle(baseStyle + "-fx-alignment: CENTER_LEFT;");

        // Hover Effects (Centers the icon if collapsed)
        btn.setOnMouseEntered(e -> {
            String align = isCollapsed ? "CENTER" : "CENTER_LEFT";
            btn.setStyle(baseStyle + "-fx-background-color: rgba(255,255,255,0.4); -fx-background-radius: 5; -fx-alignment: " + align + ";");
        });

        btn.setOnMouseExited(e -> {
            String align = isCollapsed ? "CENTER" : "CENTER_LEFT";
            btn.setStyle(baseStyle + "-fx-alignment: " + align + ";");
        });

        btn.setMaxWidth(Double.MAX_VALUE);

        // Initial Tooltip
        btn.setTooltip(new Tooltip(labelText));

        return btn;
    }

    // Without hovering effects button
    private Button createMenuButton2(String iconText, String labelText) {
        Button btn = new Button(iconText + "   " + labelText);

        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #1a1a1a; -fx-font-size: 16px; -fx-cursor: hand;";
        btn.setStyle(baseStyle + "-fx-alignment: CENTER_LEFT;");

        btn.setMaxWidth(Double.MAX_VALUE);

        // Initial Tooltip
        btn.setTooltip(new Tooltip(labelText));

        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}