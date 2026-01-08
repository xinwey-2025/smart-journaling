package main.java.com.journalapp.controller;

import javafx.application.Application;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
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

        entryEditorView.setOnEntrySaved(() -> {
            System.out.println("Entry saved! Switching to Journal View.");
            // Switch the center screen to the Journal List
            loadJournals();
        });

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(
                loginView.getView(),
                screenBounds.getWidth() * 0.85,
                screenBounds.getHeight() * 0.85
        );

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
        mainAppLayout = null;
        createMainAppLayout();
        primaryStage.getScene().setRoot(mainAppLayout);
        loadDashboard();
    }

    public void logout() {
        Session.logout();
        mainAppLayout = null;
        showLoginView();
    }

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

        homeBtn = createMenuButton("\uD83C\uDFE0", "Home");
        homeBtn.setOnAction(e -> loadDashboard());

        newEntryBtn = createMenuButton("\uD83D\uDD89", "Today's Entry");
        newEntryBtn.setOnAction(e -> {
            openTodayEntry();
            loadNewEntry();
        });

        journalsBtn = createMenuButton("\uD83D\uDCD6", "Journals");
        journalsBtn.setOnAction(e -> loadJournals());

        logoutBtn = createMenuButton("→", "Log out");
        logoutBtn.setOnAction(e -> logout());

        // Collapse
        collapseBtn = createMenuButton("≡", "Collapse");
        collapseBtn.setOnAction(e -> toggleSidebar());

        Region spacerMiddle = new Region();
        VBox.setVgrow(spacerMiddle, Priority.ALWAYS);

        sidebar.getChildren().addAll(
                userBtn, divider, homeBtn, newEntryBtn, journalsBtn,
                spacerMiddle,
                logoutBtn,
                new Region() {{ setMinHeight(30); }},
                collapseBtn
        );
    }

    // Collapse Logic
    private void toggleSidebar() {
        isCollapsed = !isCollapsed;

        if (isCollapsed) {
            // Mini mode
            sidebar.setPrefWidth(80);
            sidebar.setAlignment(Pos.TOP_CENTER);
            sidebar.setPadding(new Insets(40, 10, 40, 10));

            // Hide text, keep icons
            setButtonLabel(userBtn, "");
            setButtonLabel(homeBtn, "");
            setButtonLabel(newEntryBtn, "");
            setButtonLabel(journalsBtn, "");
            setButtonLabel(logoutBtn, "");

            // Change collapse btn to expand btn
            updateButtonIcon(collapseBtn, "»");
            setButtonLabel(collapseBtn, "");
            collapseBtn.setTooltip(new Tooltip("Expand Sidebar"));

        } else {
            // Expanded
            sidebar.setPrefWidth(220);
            sidebar.setAlignment(Pos.TOP_LEFT);
            sidebar.setPadding(new Insets(40, 20, 40, 30));

            String currentUsername = (Session.getUsername() != null) ? Session.getUsername() : "";

            // Show text
            setButtonLabel(userBtn, currentUsername);
            setButtonLabel(homeBtn, "Home");
            setButtonLabel(newEntryBtn, "Today's Entry");
            setButtonLabel(journalsBtn, "Journals");
            setButtonLabel(logoutBtn, "Log out");

            // Change btn back to collapse
            updateButtonIcon(collapseBtn, "≡");
            setButtonLabel(collapseBtn, "Collapse");
            collapseBtn.setTooltip(new Tooltip("Collapse Sidebar"));
        }

        refreshButtonStyles();
    }

    private void setButtonLabel(Button btn, String text) {
        btn.setText(text);
    }

    private void updateButtonIcon(Button btn, String newIcon) {
        if (btn.getGraphic() instanceof Label) {
            ((Label) btn.getGraphic()).setText(newIcon);
        }
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
        collapseBtn.setStyle(fullStyle);
        userBtn.setStyle(fullStyle);
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
        return buildButtonBase(iconText,labelText, true);
    }

    // Without hovering effects button
    private Button createMenuButton2(String iconText, String labelText) {
        return buildButtonBase(iconText,labelText, false);
    }

    // Use fixed button logic
    private Button buildButtonBase(String iconStr, String textStr, boolean hoverEffect) {
        // Create the Icon as a separate Label
        Label iconLabel = new Label(iconStr);
        iconLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #1a1a1a;");
        iconLabel.setMinWidth(35); // CRITICAL: Fixed width ensures alignment
        iconLabel.setAlignment(Pos.CENTER);

        // Create the Button with the Icon set as a Graphic
        Button btn = new Button(textStr);
        btn.setGraphic(iconLabel);
        btn.setGraphicTextGap(15); // Space between Icon and Text
        btn.setMaxWidth(Double.MAX_VALUE);

        // Base Style
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #1a1a1a; -fx-font-size: 16px; -fx-cursor: hand;";

        // Initial Alignment
        btn.setStyle(baseStyle + "-fx-alignment: CENTER_LEFT;");

        // Hover Logic
        if (hoverEffect) {
            btn.setOnMouseEntered(e -> {
                String align = isCollapsed ? "CENTER" : "CENTER_LEFT";
                btn.setStyle(baseStyle + "-fx-background-color: rgba(255,255,255,0.4); -fx-background-radius: 5; -fx-alignment: " + align + ";");
            });

            btn.setOnMouseExited(e -> {
                String align = isCollapsed ? "CENTER" : "CENTER_LEFT";
                btn.setStyle(baseStyle + "-fx-alignment: " + align + ";");
            });
        }

        // Store the text in the userData so we can retrieve it later if needed
        btn.setUserData(textStr);

        // Initial Tooltip
        btn.setTooltip(new Tooltip(textStr));

        return btn;
    }


    public static void main(String[] args) {
        launch(args);
    }
}