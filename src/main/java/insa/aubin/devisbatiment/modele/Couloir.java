package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;

public class Couloir extends ElementDeConstruction {
    
    private List<List<Mur>> zonesDelimiteurs;
    private double hauteurPlafond;
    private static int compteur = 0;
    private final int numero;

    public Couloir(double hauteurPlafond) {
        super("Couloir");
        this.zonesDelimiteurs = new ArrayList<>();
        this.hauteurPlafond = hauteurPlafond;
        compteur++;
        this.numero = compteur;
    }

    public void ajouterZone(List<Mur> murs) {
        zonesDelimiteurs.add(new ArrayList<>(murs));
    }

    public List<List<Point>> getPolygones() {
        List<List<Point>> polygones = new ArrayList<>();
        for (List<Mur> zone : zonesDelimiteurs) {
            List<Point> polygone = new ArrayList<>();
            for (Mur m : zone) polygone.add(m.getPoint1());
            polygones.add(polygone);
        }
        return polygones;
    }

    public List<Point> getPolygone() {
        if (zonesDelimiteurs.isEmpty()) return new ArrayList<>();
        return getPolygones().get(0);
    }

    public List<List<Mur>> getZonesDelimiteurs() { return zonesDelimiteurs; }

    public double getHauteurPlafond() { return hauteurPlafond; }

    @Override
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(java.util.Locale.US,
            "COULOIR;%s;%d;%.2f", getId(), numero, hauteurPlafond));
        for (Point p : getPolygone()) {
            sb.append(String.format(java.util.Locale.US, ";%.2f;%.2f", p.getX(), p.getY()));
        }
        return sb.toString();
    }

    @Override
    public String toString() { return "Couloir " + numero; }
}