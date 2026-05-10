package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class EchelleVue extends VBox {

    private ToggleGroup groupeEchelle;
    private RadioButton rb05, rb1, rb2, rb5, rb10;

    public EchelleVue() {
        this.setSpacing(10);
        this.setPadding(new Insets(15));
        this.setStyle(
            "-fx-background-color: rgba(240, 240, 240, 0.9);" +
            "-fx-border-color: #bdc3c7;" +
            "-fx-border-radius: 5;" +
            "-fx-background-radius: 5;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);"
        );
        this.setMaxSize(200, Region.USE_PREF_SIZE);

        Label titre = new Label("Échelle");
        titre.setStyle("-fx-font-weight: bold;");

        // Groupe de boutons radio pour l'échelle
        groupeEchelle = new ToggleGroup();
        rb05  = new RadioButton("0,5 m / carreau");
        rb1   = new RadioButton("1 m / carreau");
        rb2   = new RadioButton("2 m / carreau");
        rb5   = new RadioButton("5 m / carreau");
        rb10  = new RadioButton("10 m / carreau");

        rb05.setToggleGroup(groupeEchelle);
        rb1.setToggleGroup(groupeEchelle);
        rb2.setToggleGroup(groupeEchelle);
        rb5.setToggleGroup(groupeEchelle);
        rb10.setToggleGroup(groupeEchelle);

        // Échelle par défaut : 1 m / carreau
        rb1.setSelected(true);

        this.getChildren().addAll(titre, new Separator(), rb05, rb1, rb2, rb5, rb10);

        // Par défaut, panneau caché
        this.setVisible(false);
    }

    /** Retourne la valeur d'échelle sélectionnée (taille d'un carreau en mètres) */
    public double getEchelleSelectionnee() {
        RadioButton selected = (RadioButton) groupeEchelle.getSelectedToggle();
        if (selected == rb05)  return 0.5;
        if (selected == rb2)   return 2.0;
        if (selected == rb5)   return 5.0;
        if (selected == rb10)  return 10.0;
        return 1.0; // défaut : rb1
    }

    /** Permet d'ajouter un listener externe sur le changement d'échelle */
    public ToggleGroup getGroupeEchelle() {
        return groupeEchelle;
    }
}