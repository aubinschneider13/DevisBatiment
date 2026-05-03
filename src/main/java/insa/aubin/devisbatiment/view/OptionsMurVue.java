package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class OptionsMurVue extends VBox {
    private RadioButton rbManuel;
    private RadioButton rbCoordonnees;
    private RadioButton rbRectangulaire;
    private RadioButton rbLibre;

    public OptionsMurVue() {
        this.setSpacing(10);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: rgba(240, 240, 240, 0.9);" + "fx-border-color: #bdc3c7;" + "-fx-border-radius: 5;" + "-fx-background-radius: 5;" + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
        this.setMaxSize(200, Region.USE_PREF_SIZE );

        Label titre = new Label("Options du Mur");
        titre.setStyle("-fx-font-weight: bold;");

        //Mode de saisie
        ToggleGroup groupeSaisie = new ToggleGroup();
        rbManuel = new RadioButton("Tracé à la main");
        rbCoordonnees = new RadioButton("Saisie coordonnées");
        rbManuel.setToggleGroup(groupeSaisie);
        rbCoordonnees.setToggleGroup(groupeSaisie);
        rbManuel.setSelected(true);

        //Forme de la pièce
        ToggleGroup groupeForme = new ToggleGroup();
        rbRectangulaire = new RadioButton("Rectangulaire");
        rbLibre = new RadioButton("Forme libre");
        rbRectangulaire.setToggleGroup(groupeForme);
        rbLibre.setToggleGroup(groupeForme);
        rbRectangulaire.setSelected(true);

        this.getChildren().addAll(titre, new Separator(), rbManuel, rbCoordonnees, new Separator(), rbRectangulaire, rbLibre);

        // Par défaut, on cache le panneau
        this.setVisible(false);
    }

    public boolean estRectangulaire(){
        return  rbRectangulaire.isSelected();
    }
}
