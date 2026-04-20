package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public interface Dessin {
    void dessiner(GraphicsContext gc);
    Color getColor();
    void setColor(Color color);
}
