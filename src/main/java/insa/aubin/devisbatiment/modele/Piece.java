package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;
import insa.aubin.devisbatiment.modele.GeometrieUtils.MurOriente;

public class Piece extends ElementDeConstruction {

    private double hauteurPlafond;
    private List<Point> points;
    private List<Mur> murs;
    private List<MurOriente> mursOrientes;
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
        this.mursOrientes = new ArrayList<>(GeometrieUtils.ordonnerMurs(murs));
        this.murs = new ArrayList<>();
        for (MurOriente murOriente : this.mursOrientes) {
            this.murs.add(murOriente.mur());
        }
        this.cotesMurs = new ArrayList<>();
        this.usages         = new ArrayList<>();

        // Dériver les points depuis les murs ordonnés
        this.points = new ArrayList<>();
        for (MurOriente m : this.mursOrientes) {
            this.points.add(m.getPoint1());
        }

        // Pour chaque mur ordonné, déterminer quel côté (coteGauche ou coteDroit) fait face à la pièce
        for (MurOriente murOriente : this.mursOrientes) {
            Mur m = murOriente.mur();
            // Le mur 'm' est ordonné dans le sens du contour de la pièce.
            // On regarde si son côté gauche fait face à l'intérieur du polygone de la pièce.
            boolean gaucheInterieur = estCoteGaucheDansPiece(murOriente, this.points);
            
            // Le vrai mur d'origine sur le canevas (pour ne pas perdre les références)
            Mur vraiMur = m;
            
            if (murOriente.inverse()) {
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
     * Retourne les murs de contour ordonnés de la pièce.
     */
    public List<Mur> getMurs() {
        return murs;
    }

    public List<MurOriente> getMursOrientes() {
        return mursOrientes;
    }

    private boolean estCoteGaucheDansPiece(MurOriente mur, List<Point> pointsPiece) {
        if (pointsPiece == null || pointsPiece.size() < 3) return true;
        double mx = (mur.getPoint1().getX() + mur.getPoint2().getX()) / 2.0;
        double my = (mur.getPoint1().getY() + mur.getPoint2().getY()) / 2.0;
        double dx = mur.getPoint2().getX() - mur.getPoint1().getX();
        double dy = mur.getPoint2().getY() - mur.getPoint1().getY();
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return true;
        double tx = mx + 0.01 * (-dy / len);
        double ty = my + 0.01 * (dx / len);
        return GeometrieUtils.pointDansPolygone(tx, ty, pointsPiece);
    }
    
    public List<CoteMur> getCotesMurs() {
        return cotesMurs;
    }
    
    public List<Mur> construireMursAffichage() {
        List<Point> pts = getPoints();
        if (pts == null || pts.size() < 3) return getMurs();
        List<Mur> result = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            Point p1 = pts.get(i);
            Point p2 = pts.get((i + 1) % pts.size());
            Mur original = getMurs().stream()
                .filter(m -> GeometrieUtils.mursOntUnSupportCommun(m, new Mur(p1, p2, getHauteurPlafond())))
                .findFirst().orElse(null);

            // Si l'arête du polygone va en sens inverse de l'original,
            // on la retourne pour que copierPourMur ne flippe pas l'orientation
            Point mp1 = p1, mp2 = p2;
            if (original != null && !GeometrieUtils.memeSens(original, new Mur(p1, p2, getHauteurPlafond()))) {
                mp1 = p2;
                mp2 = p1;
            }

            Mur murAffichage = new Mur(mp1, mp2, original != null ? original.getHauteur() : getHauteurPlafond());
            if (original != null) {
                murAffichage.setOriginal(original);
                murAffichage.setTypeMur(original.getTypeMur());
                original.getListeOuvertures().forEach(o ->
                    OuvertureUtils.ajouterCopieSiAbsente(murAffichage, o, original));
                
                // Copier les revêtements pour l'affichage immédiat
                if (original.getCoteGauche().getRevetements() != null) {
                    murAffichage.getCoteGauche().getRevetements().clear();
                    murAffichage.getCoteGauche().getRevetements().addAll(original.getCoteGauche().getRevetements());
                }
                if (original.getCoteDroit().getRevetements() != null) {
                    murAffichage.getCoteDroit().getRevetements().clear();
                    murAffichage.getCoteDroit().getRevetements().addAll(original.getCoteDroit().getRevetements());
                }
            }
            result.add(murAffichage);
        }
        return result;
    }

    public List<Usage> getUsages()                 { return usages; }
    public Sol getSol()                            { return sol; }
    public Plafond getPlafond()                    { return plafond; }
    public static void resetCompteur()             { compteur = 0; }
    public static void setCompteur(int valeur)      { compteur = Math.max(0, valeur); }
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
