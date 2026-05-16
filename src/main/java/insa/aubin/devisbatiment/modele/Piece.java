package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;

public class Piece extends ElementDeConstruction {

    private double hauteurPlafond;
    private List<Point> points;
    private List<Mur> murs;
    private List<Usage> usages;
    private Sol sol;
    private Plafond plafond;

    // =========================================================================
    // CONSTRUCTEURS
    // =========================================================================

    /**
     * Constructeur principal — reçoit les murs existants (déjà dessinés).
     * Les points sont dérivés des murs pour éviter la duplication.
     */
    public Piece(List<Mur> murs, double hauteurPlafond) {
        super("Piece");
        this.hauteurPlafond = hauteurPlafond;
        this.murs           = new ArrayList<>(murs);
        this.usages         = new ArrayList<>();

        // Dériver les points depuis les murs ordonnés
        this.points = new ArrayList<>();
        for (Mur m : this.murs) {
            this.points.add(m.getPoint1());
        }

        double surface   = calculerSurfaceTotale();
        this.sol         = new Sol(surface);
        this.plafond     = new Plafond(surface);
    }

    // =========================================================================
    // CALCULS
    // =========================================================================

    /**
     * Calcule la surface au sol via la formule du lacet (Shoelace).
     * Recalculée à chaque appel pour rester cohérente si les points changent.
     */
    public double calculerSurfaceTotale() {
        double surface = 0;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % n);
            surface += p1.getX() * p2.getY();
            surface -= p2.getX() * p1.getY();
        }
        return Math.abs(surface) / 2.0;
    }

    /**
     * Calcule le devis de la pièce :
     * somme des prix des revêtements de tous les murs + sol + plafond.
     */
    public double calculerDevis() {
        double total = 0;
        for (Mur m : murs) {
            total += m.calculerPrixRevetement();
        }
        total += sol.calculerPrixRevetement();
        total += plafond.calculerPrixRevetement();
        return total;
    }

    // =========================================================================
    // USAGES
    // =========================================================================

    public void ajouterUsage(Usage u) {
        if (u != null) usages.add(u);
    }

    // =========================================================================
    // GETTERS / SETTERS
    // =========================================================================

    public double getHauteurPlafond()              { return hauteurPlafond; }
    public void setHauteurPlafond(double h)        { this.hauteurPlafond = h; }
    public List<Point> getPoints()                 { return points; }
    public List<Mur> getMurs()                     { return murs; }
    public List<Usage> getUsages()                 { return usages; }
    public Sol getSol()                            { return sol; }
    public Plafond getPlafond()                    { return plafond; }

    // =========================================================================
    // SÉRIALISATION
    // =========================================================================

    @Override
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append("PIECE;")
                .append(getId()).append(";")
                .append(hauteurPlafond);
        for (Point p : points) {
            sb.append(";").append(p.getX())
                    .append(";").append(p.getY());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Piece [id=" + getId()
                + ", hauteurPlafond=" + hauteurPlafond
                + ", nbMurs=" + murs.size()
                + ", surface=" + String.format("%.2f", calculerSurfaceTotale()) + " m²]";
    }
}