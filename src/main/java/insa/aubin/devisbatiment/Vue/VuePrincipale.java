package insa.aubin.devisbatiment.Vue;

import javafx.scene.layout.BorderPane;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.Canvas;

public class VuePrincipale extends BorderPane {
    public VuePrincipale() {
        // Un bouton pour calculer le devis
        Button btnCalcul = new Button("Calculer le Devis");
        
        // Une zone pour dessiner le plan (Canvas)
        Canvas canvasPlan = new Canvas(600, 400);
        
        VBox menu = new VBox(btnCalcul);
        this.setLeft(menu);
        this.setCenter(canvasPlan);
    }
}