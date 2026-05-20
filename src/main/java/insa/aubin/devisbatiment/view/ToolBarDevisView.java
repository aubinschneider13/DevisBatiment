package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Barre d'outils spécifique à l'onglet "Matériaux et Devis".
 */
public class ToolBarDevisView extends HBox {

    private final Button btnAppliquerRevetement;
    private final Button btnValiderRevetement;
    private final Button btnExporterDevis;
    private final Label labelTotalDevis;

    public ToolBarDevisView() {
        setSpacing(15);
        setPadding(new Insets(10));
        setAlignment(Pos.CENTER_LEFT);

        btnAppliquerRevetement = new Button("Appliquer un revêtement");
        btnAppliquerRevetement.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-base: #3498db;");
        btnAppliquerRevetement.setPrefHeight(40);

        btnValiderRevetement = new Button("Valider la sélection");
        btnValiderRevetement.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-base: #2ecc71;");
        btnValiderRevetement.setPrefHeight(40);

        btnExporterDevis = new Button("Exporter le devis");
        btnExporterDevis.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-base: #e67e22; -fx-text-fill: white;");
        btnExporterDevis.setPrefHeight(40);

        labelTotalDevis = new Label("Total estimé : 0.00 €");
        labelTotalDevis.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Espaceur dynamique pour pousser le label du prix tout à droite
        Region devisSpacer = new Region();
        HBox.setHgrow(devisSpacer, Priority.ALWAYS);

        getChildren().addAll(btnAppliquerRevetement, btnValiderRevetement, btnExporterDevis, devisSpacer, labelTotalDevis);
    }

    // --- GETTERS pour le contrôleur ---
    public Button getBtnAppliquerRevetement() { return btnAppliquerRevetement; }
    public Button getBtnValiderRevetement() { return btnValiderRevetement; }
    public Button getBtnExporterDevis() { return btnExporterDevis; }
    public Label getLabelTotalDevis() { return labelTotalDevis; }
}