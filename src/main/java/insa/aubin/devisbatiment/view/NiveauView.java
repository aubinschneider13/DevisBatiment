package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Vue d'un niveau : encapsule un DessinCanvas dédié + un label d'instructions
 * propre à ce niveau. Pas de toolbar (celle d'ImmeubleView est commune).
 */
public class NiveauView extends StackPane {

    private final DessinCanvas canvas;
    private final Label labelInstructions;
    private final OptionsMurVue optionsMurVue;

    public NiveauView() {
        // Canvas de dessin du niveau
        canvas = new DessinCanvas();
        
        optionsMurVue = new OptionsMurVue();
        optionsMurVue.setDefaultLibre(); // ← forme libre par défaut (voir plus bas)
        StackPane.setAlignment(optionsMurVue, Pos.TOP_RIGHT);
        StackPane.setMargin(optionsMurVue, new Insets(10));

        // Label d'instructions en bas, même style que dans ImmeubleView
        labelInstructions = new Label("Sélectionnez un outil dans la barre ci-dessus");
        labelInstructions.setStyle(
            "-fx-background-color: rgba(240,240,240,0.9);" +
            "-fx-padding: 6 12 6 12;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-text-fill: #2c3e50;" +
            "-fx-font-size: 13px;"
        );
        StackPane.setAlignment( labelInstructions, Pos.BOTTOM_CENTER);
        StackPane.setMargin(labelInstructions, new Insets(0, 0, 15, 0));

        // Le canvas se bind à la taille du parent au moment de l'affichage
        // (le bind est fait par ImmeubleView.afficherCanvas)
        this.getChildren().addAll(canvas, optionsMurVue, labelInstructions);
        this.setStyle("-fx-background-color: #fffefe;");
    }

    public DessinCanvas getCanvas() { return canvas; }
    public OptionsMurVue getOptionsMurVue() { return optionsMurVue; }
    public void setInstructions(String texte) { labelInstructions.setText(texte); }
}