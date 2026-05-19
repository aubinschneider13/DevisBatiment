package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class AireImmeuble implements Dessin {

    private Point p1, p2, p3, p4;
    private Color couleurContour = Color.DARKBLUE;
    // Remplissage semi-transparent
    private Color couleurRemplissage = Color.web("#4a90d9", 0.15);
    private Color couleurContourSurvol = Color.web("#1a5fb4"); // Côté survolé/glissé
    private boolean complete;

    // Index du côté en cours de glissement (-1 = aucun)
    // 0=p1-p2, 1=p2-p3, 2=p3-p4, 3=p4-p1
    private int coteGlisse = -1;

    public AireImmeuble(Point p1) {
        this.p1 = p1;
        this.p2 = p1;
        this.p3 = null;
        this.p4 = null;
        this.complete = false;
    }

    public void setP2(Point p2) { this.p2 = p2; }

    public void setP3(Point p3) {
        this.p3 = p3;
        this.p4 = new Point(
            p1.getX() + (p3.getX() - p2.getX()),
            p1.getY() + (p3.getY() - p2.getY())
        );
    }

    public void valider() { this.complete = true; }
    public boolean isComplete() { return complete; }

    public Point getP1() { return p1; }
    public Point getP2() { return p2; }
    public Point getP3() { return p3; }
    public Point getP4() { return p4; }

    /**
     * Retourne l'index du côté le plus proche du point (px, py)
     * si la distance est inférieure au seuil donné, sinon -1.
     * 0=p1-p2, 1=p2-p3, 2=p3-p4, 3=p4-p1
     */
    public int detecterCote(double px, double py, double seuil) {
        if (p3 == null || p4 == null) return -1;
        Point[][] cotes = {
            {p1, p2}, {p2, p3}, {p3, p4}, {p4, p1}
        };
        for (int i = 0; i < 4; i++) {
            if (distancePointSegment(px, py, cotes[i][0], cotes[i][1]) < seuil) {
                return i;
            }
        }
        return -1;
    }

    /** Distance d'un point à un segment */
    private double distancePointSegment(double px, double py, Point a, Point b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0) return Math.hypot(px - a.getX(), py - a.getY());
        double t = Math.max(0, Math.min(1,
            ((px - a.getX()) * dx + (py - a.getY()) * dy) / lenSq));
        return Math.hypot(px - (a.getX() + t * dx), py - (a.getY() + t * dy));
    }

    public void deplacerCote(int indexCote, double px, double py) {
        if (p3 == null || p4 == null) return;

        switch (indexCote) {
            case 0: { // Côté p1-p2 — se déplace dans la direction p1→p4
                double perpX = p4.getX() - p1.getX();
                double perpY = p4.getY() - p1.getY();
                double len = Math.sqrt(perpX * perpX + perpY * perpY);
                if (len == 0) break;
                perpX /= len; perpY /= len;
                // Projection du curseur sur l'axe perpendiculaire depuis p4
                double proj = (px - p4.getX()) * perpX + (py - p4.getY()) * perpY;
                p1 = new Point(p4.getX() + proj * perpX, p4.getY() + proj * perpY);
                p2 = new Point(p3.getX() + proj * perpX, p3.getY() + proj * perpY);
                break;
            }
            case 1: { // Côté p2-p3 — se déplace dans la direction p2→p1
                double perpX = p1.getX() - p2.getX();
                double perpY = p1.getY() - p2.getY();
                double len = Math.sqrt(perpX * perpX + perpY * perpY);
                if (len == 0) break;
                perpX /= len; perpY /= len;
                // Projection du curseur sur l'axe perpendiculaire depuis p1
                double proj = (px - p1.getX()) * perpX + (py - p1.getY()) * perpY;
                p2 = new Point(p1.getX() + proj * perpX, p1.getY() + proj * perpY);
                p3 = new Point(p4.getX() + proj * perpX, p4.getY() + proj * perpY);
                break;
            }
            case 2: { // Côté p3-p4 — se déplace dans la direction p4→p1
                double perpX = p1.getX() - p4.getX();
                double perpY = p1.getY() - p4.getY();
                double len = Math.sqrt(perpX * perpX + perpY * perpY);
                if (len == 0) break;
                perpX /= len; perpY /= len;
                // Projection du curseur sur l'axe perpendiculaire depuis p1
                double proj = (px - p1.getX()) * perpX + (py - p1.getY()) * perpY;
                p3 = new Point(p2.getX() + proj * perpX, p2.getY() + proj * perpY);
                p4 = new Point(p1.getX() + proj * perpX, p1.getY() + proj * perpY);
                break;
            }
            case 3: { // Côté p4-p1 — se déplace dans la direction p4→p3
                double perpX = p3.getX() - p4.getX();
                double perpY = p3.getY() - p4.getY();
                double len = Math.sqrt(perpX * perpX + perpY * perpY);
                if (len == 0) break;
                perpX /= len; perpY /= len;
                // Projection du curseur sur l'axe perpendiculaire depuis p3
                double proj = (px - p3.getX()) * perpX + (py - p3.getY()) * perpY;
                p4 = new Point(p3.getX() + proj * perpX, p3.getY() + proj * perpY);
                p1 = new Point(p2.getX() + proj * perpX, p2.getY() + proj * perpY);
                break;
            }
        }
    }

    public void setCoteGlisse(int index) { this.coteGlisse = index; }
    public int getCoteGlisse() { return coteGlisse; }

    @Override
    public Color getColor() { return couleurContour; }

    @Override
    public void setColor(Color color) { this.couleurContour = color; }

    @Override
    public void dessiner(GraphicsContext gc) {
        if (p1 == null) return;

        if (p3 != null && p4 != null) {
            double[] xs = { p1.getX(), p2.getX(), p3.getX(), p4.getX() };
            double[] ys = { p1.getY(), p2.getY(), p3.getY(), p4.getY() };

            // Remplissage semi-transparent
            gc.setFill(couleurRemplissage);
            gc.fillPolygon(xs, ys, 4);

            // Contour — chaque côté coloré différemment si survolé
            Point[][] cotes = {{p1,p2},{p2,p3},{p3,p4},{p4,p1}};
            for (int i = 0; i < 4; i++) {
                gc.setStroke(i == coteGlisse ? couleurContourSurvol : couleurContour);
                gc.setLineWidth(i == coteGlisse ? 0.15 : 0.08);
                gc.strokeLine(
                    cotes[i][0].getX(), cotes[i][0].getY(),
                    cotes[i][1].getX(), cotes[i][1].getY()
                );
            }

        } else {
            // Aperçu premier côté
            gc.setStroke(couleurContour);
            gc.setLineWidth(0.08);
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }

        // Coins
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