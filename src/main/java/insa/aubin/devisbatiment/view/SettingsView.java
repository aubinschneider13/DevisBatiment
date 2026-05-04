package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SettingsView extends VBox {
    private ToggleButton toggleSauvegarde;
    private TextField cheminField;
    private Button validerButton;
    private Label statusLabel;

    public SettingsView() {
        this.setSpacing(15);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT); // corrigé : Pos.TOP_LEFT est valide
        this.setPrefWidth(300);
        this.setPrefHeight(500);

        Label titre = new Label("Paramètres");
        titre.setStyle("-fx-font-family: 'Arial Black'; -fx-font-size: 16px;");

        // Toggle sauvegarde
        HBox toggleBox = new HBox(10);
        toggleBox.setAlignment(Pos.CENTER_LEFT); // corrigé : Pos.CENTER_LEFT est valide
        Label toggleLabel = new Label("Sauvegarde automatique :");
        toggleLabel.setStyle("-fx-font-family: Arial; -fx-font-size: 13px;");
        toggleSauvegarde = new ToggleButton("Désactivée");
        toggleSauvegarde.setStyle("-fx-cursor: hand;");
        toggleBox.getChildren().addAll(toggleLabel, toggleSauvegarde);

        // Zone chemin (cachée par défaut)
        cheminField = new TextField();
        cheminField.setPromptText("Collez le chemin d'accès ici...");
        cheminField.setVisible(false);
        cheminField.setManaged(false);

        validerButton = new Button("Valider");
        validerButton.setStyle("-fx-cursor: hand;");
        validerButton.setVisible(false);
        validerButton.setManaged(false);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        this.getChildren().addAll(titre, toggleBox, cheminField, validerButton, statusLabel);
    }

    public ToggleButton getToggleSauvegarde() {
        return toggleSauvegarde;
    }

    public TextField getCheminField() {
        return cheminField;
    }

    public Button getValiderButton() {
        return validerButton;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }

    public void afficherZoneChemin(boolean visible) {
        cheminField.setVisible(visible);
        cheminField.setManaged(visible);
        validerButton.setVisible(visible);
        validerButton.setManaged(visible);
    }
}