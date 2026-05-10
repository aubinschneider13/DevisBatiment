package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class AireImmeuble implements Dessin {

    private Point p1, p2, p3, p4; // p4 calculé automatiquement
    private Color couleurContour = Color.DARKBLUE;
    private Color couleurRemplissage = Color.web("#e8f0fe"); // bleu très clair
    private boolean complete; // true quand les 4 coins sont définis

    public AireImmeuble(Point p1) {
        this.p1 = p1;
        this.p2 = p1;
        this.p3 = null;
        this.p4 = null;
        this.complete = false;
    }

    /** Mise à jour du 2ème point (preview du premier côté) */
    public void setP2(Point p2) {
        this.p2 = p2;
    }

    /** Mise à jour du 3ème point — p4 est recalculé orthogonalement */
    public void setP3(Point p3) {
        this.p3 = p3;
        // p4 = p1 + (p3 - p2), ce qui ferme le rectangle
        this.p4 = new Point(
            p1.getX() + (p3.getX() - p2.getX()),
            p1.getY() + (p3.getY() - p2.getY())
        );
    }

    /** Marque le rectangle comme complet et figé */
    public void valider() {
        this.complete = true;
    }

    public boolean isComplete() {
        return complete;
    }

    public Point getP1() { return p1; }
    public Point getP2() { return p2; }
    public Point getP3() { return p3; }
    public Point getP4() { return p4; }

    @Override
    public Color getColor() { return couleurContour; }

    @Override
    public void setColor(Color color) { this.couleurContour = color; }

    @Override
    public void dessiner(GraphicsContext gc) {
        if (p1 == null) return;

        if (p3 != null && p4 != null) {
            // Rectangle complet : remplissage + contour
            double[] xs = { p1.getX(), p2.getX(), p3.getX(), p4.getX() };
            double[] ys = { p1.getY(), p2.getY(), p3.getY(), p4.getY() };

            gc.setFill(couleurRemplissage);
            gc.fillPolygon(xs, ys, 4);

            gc.setStroke(couleurContour);
            gc.setLineWidth(0.1);
            gc.strokePolygon(xs, ys, 4);

        } else {
            // Aperçu du premier côté seulement (p1 → p2)
            gc.setStroke(couleurContour);
            gc.setLineWidth(0.1);
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }

        // Points de contrôle sur chaque coin posé
        dessinerCoin(gc, p1);
        dessinerCoin(gc, p2);
        if (p3 != null) dessinerCoin(gc, p3);
        if (p4 != null) dessinerCoin(gc, p4);
    }

    private void dessinerCoin(GraphicsContext gc, Point p) {
        double r = 0.08;
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.DARKBLUE);
        gc.setLineWidth(0.02);
        gc.fillOval(p.getX() - r, p.getY() - r, r * 2, r * 2);
        gc.strokeOval(p.getX() - r, p.getY() - r, r * 2, r * 2);
    }
}