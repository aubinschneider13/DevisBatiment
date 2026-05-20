package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;

public class Couloir extends ElementDeConstruction implements Dessin {

    private List<Mur> mursDelimiteurs;
    private double hauteurPlafond;

    private static int compteur = 0;
    private final int numero;

    public Couloir(List<Mur> mursDelimiteurs, double hauteurPlafond) {
        super("Couloir");
        this.mursDelimiteurs = new ArrayList<>(mursDelimiteurs);
        this.hauteurPlafond  = hauteurPlafond;
        compteur++;
        this.numero = compteur;
    }

    public static void resetCompteur() { compteur = 0; }

    public List<Point> getPolygone() {
        List<Point> polygone = new ArrayList<>();
        for (Mur m : mursDelimiteurs) polygone.add(m.getPoint1());
        return polygone;
    }

    public double calculerSurface() {
        List<Point> poly = getPolygone();
        double aire = 0;
        int n = poly.size();
        for (int i = 0; i < n; i++) {
            Point a = poly.get(i);
            Point b = poly.get((i + 1) % n);
            aire += a.getX() * b.getY() - b.getX() * a.getY();
        }
        return Math.abs(aire) / 2.0;
    }

    public List<Mur> getMursDelimiteurs()          { return mursDelimiteurs; }
    public void setMursDelimiteurs(List<Mur> murs) { this.mursDelimiteurs = new ArrayList<>(murs); }
    public double getHauteurPlafond()              { return hauteurPlafond; }

    @Override
    public Color getColor() { return Color.web("#7f8c8d"); }

    @Override
    public void setColor(Color color) { }

    @Override
    public void dessiner(GraphicsContext gc) {
        List<Point> polygone = getPolygone();
        if (polygone.size() < 3) return;

        int n = polygone.size();
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = polygone.get(i).getX();
            ys[i] = polygone.get(i).getY();
        }

        // Fond gris hachuré pour distinguer du couloir
        gc.setFill(Color.web("#C4140A", 0.4));
        gc.fillPolygon(xs, ys, n);

        gc.setStroke(Color.web("#990202"));
        gc.setLineWidth(0.06);
        gc.setLineDashes(0.2, 0.1);
        gc.strokePolygon(xs, ys, n);
        gc.setLineDashes();

        // Label centré
        double cx = 0, cy = 0;
        for (Point p : polygone) { cx += p.getX(); cy += p.getY(); }
        cx /= n; cy /= n;

        gc.save();
        gc.scale(1, -1);
        gc.setFill(Color.web("#660404"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 0.3));
        gc.fillText(toString(), cx - 0.4, -cy + 0.15);
        gc.restore();
    }

    @Override
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(java.util.Locale.US,
            "COULOIR;%s;%d;%.2f;%.2f",
            getId(), numero, hauteurPlafond, calculerSurface()));
        for (Point p : getPolygone()) {
            sb.append(String.format(java.util.Locale.US, ";%.2f;%.2f", p.getX(), p.getY()));
        }
        return sb.toString();
    }

    @Override
    public String toString() { return "Couloir " + numero; }
}