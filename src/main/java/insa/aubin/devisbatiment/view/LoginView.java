package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {
    public LoginView() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(10);
        this.setPadding(new Insets(50));

        Label appLabel = new Label("InsaBuilder");
        appLabel.setStyle("-fx-font-family: 'Bauhaus 93'; -fx-font-size: 30px; -fx-text-fill: #2c3e50;");
        Label instructionLabel = new Label("Connectez-vous pour accéder à votre application");
        instructionLabel.setStyle("-fx-font-family: 'Arial Black'; -fx-font-size: 14px");
        instructionLabel.setWrapText(true);
        instructionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        PasswordField password = new PasswordField();
        password.setPromptText("Entrez le mot de passe");
        password.setMaxWidth(250);

        Button button = new Button("Validez");
        button.setStyle("-fx-cursor: hand;");
        button.setPrefWidth(100);

        this.getChildren().addAll(appLabel, instructionLabel, password, button);
    }
}