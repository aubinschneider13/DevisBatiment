package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;

public class Piece extends ElementDeConstruction {

    private double hauteurPlafond;
    private List<Point> points;
    private List<CoteMur> cotesMurs;
    private List<Usage> usages;
    private Sol sol;
    private Plafond plafond;
    private static int compteur = 0;
    private final int numero;

    // =========================================================================
    // CONSTRUCTEURS
    // =========================================================================

    /**
     * Constructeur principal — reçoit les murs existants (déjà dessinés).
     * Les points sont dérivés des murs pour éviter la duplication.
     */
    public Piece(List<Mur> murs, double hauteurPlafond) {
        super("Piece");
        compteur++;
        this.numero = compteur;
        this.hauteurPlafond = hauteurPlafond;
        this.cotesMurs = new ArrayList<>();
        this.usages         = new ArrayList<>();

        // Dériver les points depuis les murs ordonnés
        this.points = new ArrayList<>();
        for (Mur m : murs) {
            this.points.add(m.getPoint1());
        }

        // Pour chaque mur ordonné, déterminer quel côté (coteGauche ou coteDroit) fait face à la pièce
        for (Mur m : murs) {
            // Le mur 'm' est ordonné dans le sens du contour de la pièce.
            // On regarde si son côté gauche fait face à l'intérieur du polygone de la pièce.
            boolean gaucheInterieur = GeometrieUtils.estCoteGaucheDansPiece(m, this.points);
            
            // Le vrai mur d'origine sur le canevas (pour ne pas perdre les références)
            Mur vraiMur = m.getOriginal();
            
            if (m != vraiMur) {
                // 'm' est inversé par ordonnerMurs.
                // Donc le côté gauche de 'm' correspond au côté droit de 'vraiMur', et inversement.
                if (gaucheInterieur) {
                    this.cotesMurs.add(vraiMur.getCoteDroit());
                } else {
                    this.cotesMurs.add(vraiMur.getCoteGauche());
                }
            } else {
                // 'm' n'est pas inversé.
                if (gaucheInterieur) {
                    this.cotesMurs.add(vraiMur.getCoteGauche());
                } else {
                    this.cotesMurs.add(vraiMur.getCoteDroit());
                }
            }
        }

        double surface   = calculerSurfaceTotale();
        this.sol         = new Sol(surface);
        this.sol.setPolygonePiece(this.points);
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
     * somme des prix des revêtements de tous les CoteMur + sol + plafond.
     */
    public double calculerDevis() {
        double total = 0;
        for (CoteMur cm : cotesMurs) {
            total += cm.calculerPrixRevetement();
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
    
    /**
     * Retourne les murs parents ordonnés pour compatibilité.
     */
    public List<Mur> getMurs() {
        List<Mur> parentMurs = new ArrayList<>();
        for (CoteMur cm : cotesMurs) {
            parentMurs.add(cm.getMurParent());
        }
        return parentMurs;
    }
    
    public List<CoteMur> getCotesMurs() {
        return cotesMurs;
    }

    public List<Usage> getUsages()                 { return usages; }
    public Sol getSol()                            { return sol; }
    public Plafond getPlafond()                    { return plafond; }
    public static void resetCompteur()             { compteur = 0; }
    public int getNumero()                         { return numero; }

    // =========================================================================
    // SÉRIALISATION
    // =========================================================================

    @Override
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(java.util.Locale.US,
            "PIECE;%s;%d;%.2f;%.2f",
            getId(), numero, calculerSurfaceTotale(), hauteurPlafond));
        for (Point p : points) {
            sb.append(String.format(java.util.Locale.US, ";%.2f;%.2f", p.getX(), p.getY()));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Pièce " + numero
             + String.format(" (%.1f m²)", calculerSurfaceTotale());
    }
}