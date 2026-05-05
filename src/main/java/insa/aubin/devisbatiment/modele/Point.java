package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Point implements Dessin {
    private double x;
    private double y;
    public static double RAYON_IN_DRAW = 5;
    private Color color = Color.BLACK;
    
    public Point(double x, double y){
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
    
    public static boolean sontOrthogonaux(Point p1, Point p2, Point p3) {
        double abX = p2.getX() - p1.getX();
        double abY = p2.getY() - p1.getY();
        double bcX = p3.getX() - p2.getX();
        double bcY = p3.getY() - p2.getY();
        double produitScalaire = abX * bcX + abY * bcY;
        return Math.abs(produitScalaire) <= 1e-9;
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
