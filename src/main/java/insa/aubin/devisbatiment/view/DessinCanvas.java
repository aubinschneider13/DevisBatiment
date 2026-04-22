package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.controlleur.PieceVueControleur;
import insa.aubin.devisbatiment.modele.Dessin;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Transform;

import java.util.ArrayList;
import java.util.List;

public class DessinCanvas extends Canvas {

    private List<Dessin> elements;

    public DessinCanvas() {
        this.elements = new ArrayList<>();
        this.heightProperty().addListener(o -> this.redrawAll());
        this.widthProperty().addListener(o -> this.redrawAll());
    }

    public void redrawAll(){
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.clearRect(0, 0, this.getWidth(), this.getHeight());

        for(Dessin d: this.elements){
            d.dessiner(gc);
        }
    }

    public Transform getTransform() {
        return this.getGraphicsContext2D().getTransform();
    }

    public void ajouterElement(Dessin d) {
        this.elements.add(d);
        this.redrawAll(); // On rafraîchit dès qu'on ajoute quelque chose
    }
}
