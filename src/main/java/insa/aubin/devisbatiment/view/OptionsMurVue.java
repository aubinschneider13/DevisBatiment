package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class OptionsMurVue extends VBox {
    private RadioButton rbRectangulaire;
    private RadioButton rbLibre;

    public OptionsMurVue() {
        this.setSpacing(10);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: rgba(240, 240, 240, 0.9);" +
                "-fx-border-color: #bdc3c7;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
        this.setMaxSize(200, Region.USE_PREF_SIZE);

        Label titre = new Label("Options du Mur");
        titre.setStyle("-fx-font-weight: bold;");

        ToggleGroup groupeForme = new ToggleGroup();
        rbRectangulaire = new RadioButton("Rectangulaire");
        rbLibre = new RadioButton("Forme libre");
        rbRectangulaire.setToggleGroup(groupeForme);
        rbLibre.setToggleGroup(groupeForme);
        rbRectangulaire.setSelected(true);

        this.getChildren().addAll(titre, new Separator(), rbRectangulaire, rbLibre);
        this.setVisible(false);
    }

    public boolean estRectangulaire() {
        return rbRectangulaire.isSelected();
    }

    public void setDefaultLibre() {
        rbLibre.setSelected(true);
    }
    
    public ToggleGroup getGroupeForme() {
        return rbRectangulaire.getToggleGroup();
    }
}