package main.java.com.journalapp.controller;

import main.java.com.journalapp.controller.MainController;  // Navigator
import main.java.com.journalapp.util.Session;               // Database handler
import javafx.geometry.Pos;                                 // Tell VBox where to put its "children"
import javafx.scene.control.Button;                         // For sign up action
import javafx.scene.control.Label;                          // For non-editable text
import javafx.scene.control.PasswordField;                  // Text field that hide the characters
import javafx.scene.control.TextField;                      // Textbox (for email and username)
import javafx.scene.layout.VBox;                            // Vertical box (stacks every element put inside it vertically)

public class SignUpView {
    // Navigator, a link to the main controller so this view can tell the app to switch screens
    private final MainController mainController;
    // Root layout, acts as the master container that holds every other visual element (background, form, buttons, etc.).
    private final VBox mainContainer;

    // Made fields class-level variables so they can be accessed
    private final TextField usernameField;
    private final TextField emailField;
    private final PasswordField passField;
    private final Label errorLabel;

    public SignUpView(MainController mainController) {
        this.mainController = mainController;

        /* Screen background */
        mainContainer = new VBox();
        // Javafx css: light blue and peach pink
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #CFE3F3, #FAD0C4);");
        mainContainer.setAlignment(Pos.CENTER);  // Tell the mainContainer to keep it perfectly in the middle
        // To cover the whole computer screen
        mainContainer.setMaxWidth(Double.MAX_VALUE);  // To cover the whole computer screen
        mainContainer.setMaxHeight(Double.MAX_VALUE);

        /* Type the information box */
        VBox formContainer = new VBox(15);
        formContainer.setMaxWidth(350);
        formContainer.setAlignment(Pos.CENTER);
        // Glass Style (javafx css)
        formContainer.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.4);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.8);" +
                        "-fx-border-radius: 15;" +
                        "-fx-border-width: 1px;" +
                        "-fx-padding: 40;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 4);"
        );

        Label titleLabel = new Label("Create Account");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        String inputStyle =
                "-fx-background-color: rgba(255,255,255,0.7); " +
                        "-fx-background-radius: 5; -fx-padding: 10; -fx-font-size: 14px; " +
                        "-fx-border-color: rgba(0,0,0,0.1); -fx-border-radius: 5;";

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setStyle(inputStyle);

        emailField = new TextField();
        emailField.setPromptText("Email address");
        emailField.setStyle(inputStyle);

        passField = new PasswordField();
        passField.setPromptText("Create Password");
        passField.setStyle(inputStyle);

        errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        errorLabel.setVisible(false);  // Hide the errorLabel
        errorLabel.setManaged(false);

        Button signUpBtn = new Button("Sign Up");
        signUpBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-font-size: 14px; -fx-cursor: hand;");
        signUpBtn.setMaxWidth(Double.MAX_VALUE);

        // Hover Effect
        signUpBtn.setOnMouseEntered(e -> signUpBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-font-size: 14px; -fx-background-radius: 5; -fx-cursor: hand;"));
        signUpBtn.setOnMouseExited(e -> signUpBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-font-size: 14px; -fx-background-radius: 5; -fx-cursor: hand;"));

        signUpBtn.setOnAction(e -> {  // Lambda way
            String email = emailField.getText().trim();
            String password = passField.getText();
            String username = usernameField.getText().trim();

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                errorLabel.setText("All fields must be filled!");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            // Email format validation
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            if (!email.matches(emailRegex)) {
                errorLabel.setText("Invalid email format!");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            // Call the Session API to save the user to the CSV database
            if (Session.signup(username, email, password)) {
                clearFields(); // Clear data before switching
                mainController.showMainApp();
            } else {
                errorLabel.setText("Email already exists!");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        Button loginLink = new Button("Already have an account? Login");
        loginLink.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-cursor: hand;");
        loginLink.setOnAction(e -> {
            clearFields(); // Clear data before switching
            mainController.showLoginView();
        });

        // Take all UI pieces and add them into the white box.
        // The order here matters: the first item in addAll() appears at the top of the VBox
        formContainer.getChildren().addAll(titleLabel, usernameField, emailField, passField, errorLabel, signUpBtn, loginLink);

        // CREATE FOOTER LABEL
        Label footerLabel = new Label("By signing in you agree to our terms.");
        footerLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-padding: 20 0 0 0;");

        // Add the box and the footer to the main gradient background
        mainContainer.getChildren().addAll(formContainer, footerLabel);
    }

    public void clearFields() {
        usernameField.clear();
        emailField.clear();
        passField.clear();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    // Output for this class, allows the MainController to grab the entire finished layout and show it on the screen
    public VBox getView() {
        return mainContainer;
    }
}