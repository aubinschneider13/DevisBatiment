package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Point implements Dessin {
    private float x;
    private float y;
    public static double RAYON_IN_DRAW = 5;
    private Color color = Color.BLACK;
    
    public Point(float x, float y){
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    @Override
    public void dessiner(GraphicsContext gc) {
        gc.setFill(this.getColor());
        gc.fillOval(this.x-RAYON_IN_DRAW, this.y-RAYON_IN_DRAW, 2*RAYON_IN_DRAW, 2*RAYON_IN_DRAW);
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }
    @Override
    public Color getColor() {
        return this.color;
    }
}
