package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {
    private Label appLabel;
    private Label instructionLabel;
    private Button btnValider;
    private PasswordField password;

    public LoginView() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(10);
        this.setPadding(new Insets(50));

        this.appLabel = new Label("InsaBuilder");
        this.appLabel.setStyle("-fx-font-family: 'Bauhaus 93'; -fx-font-size: 30px; -fx-text-fill: #2c3e50;");

        this.instructionLabel = new Label("Connectez-vous pour accéder à votre application");
        this.instructionLabel.setStyle("-fx-font-family: 'Arial Black'; -fx-font-size: 14px");
        this.instructionLabel.setWrapText(true);
        this.instructionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        this.password = new PasswordField();
        this.password.setPromptText("Entrez le mot de passe");
        this.password.setMaxWidth(250);

        this.btnValider = new Button("Validez");
        this.btnValider.setStyle("-fx-cursor: hand;");
        this.btnValider.setPrefWidth(100);

        this.getChildren().addAll(appLabel, instructionLabel, password, btnValider);
    }

    public String getPassword() {
        return this.password.getText();
    }

    public void nettoyerMotDePasse(){
        this.password.clear();
        this.password.requestFocus();
    }

    public Button getBtnValider() {
        return btnValider;
    }
}