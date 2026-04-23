package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;

public class Piece extends ElementDeConstruction {

    private float hauteurPlafond;
    private List<Point> points;
    private List<Mur> murs;
    private List<Usage> usages;
    private Plafond plafond;
    private Sol sol;

    public Piece(List<Point> points, float hauteurPlafond) {
        super("Piece");
        this.hauteurPlafond = hauteurPlafond;
        this.points = points;
        this.murs = new ArrayList<>();
        this.usages = new ArrayList<>();

        // surface calculée en premier car nécessaire pour Plafond et Sol
        float surface = calculerSurfaceTotale();
        this.plafond = new Plafond(surface);
        this.sol = new Sol(surface);

        for (int i = 0; i < points.size(); i++) {
            Point debut = points.get(i);
            Point fin = points.get((i + 1) % points.size());
            this.murs.add(new Mur(debut, fin, hauteurPlafond));
        }
    }

    public float calculerSurfaceTotale() {
        float surface = 0;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % n);
            surface += p1.getX() * p2.getY();
            surface -= p2.getX() * p1.getY();
        }
        return Math.abs(surface) / 2.0f;
    }

    public float calculerDevis() {
        float total = 0;
        for (Mur m : murs) {
            total += m.calculerPrixRevetement();
        }
        total += plafond.calculerPrixRevetement();
        total += sol.calculerPrixRevetement();
        return total;
    }

    public float getHauteurPlafond() {
        return hauteurPlafond;
    }
    public List<Point> getPoints() {
        return points;
    }
    
    public List<Mur> getMurs() {
        return murs;
    }
    
    public List<Usage> getUsages() {
        return usages;
    }
    
    public void ajouterUsage(Usage u) {
        if (u != null) usages.add(u);
    }
    
    public Plafond getPlafond() {
        return plafond;
    }
    
    public Sol getSol() {
        return sol;
    }

    @Override
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append("PIECE;").append(getId()).append(";").append(hauteurPlafond);
        for (Point p : points) {
            sb.append(";").append(p.getX()).append(";").append(p.getY());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Piece [id=" + getId() + ", hauteurPlafond=" + hauteurPlafond
                + ", nbMurs=" + murs.size() + "]";
    }
}