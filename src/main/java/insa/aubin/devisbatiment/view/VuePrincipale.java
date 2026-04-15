package insa.aubin.devisbatiment.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class VuePrincipale extends BorderPane {
    public VuePrincipale() {
        // Un bouton pour calculer le devis
        Label label1 = new Label("Créer un Devis");
        
        // Une zone pour dessiner le plan (Canvas)
        Canvas canvasPlan = new Canvas(600, 400);
        
        VBox menu = new VBox(label1);
        this.setLeft(menu);
        this.setCenter(canvasPlan);
    }
}