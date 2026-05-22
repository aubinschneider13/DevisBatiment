package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;

public class Appartement extends ElementDeConstruction implements Dessin {

    // --- Modèle métier ---
    private List<Mur> mursDelimiteurs; // Murs qui ferment le périmètre de l'appartement
    private List<Piece> pieces;        // Pièces à l'intérieur de l'appartement
    private double hauteurPlafond;

    // --- Champs visuels ---
    private Color couleurFond;
    private Color couleurContour;

    // Numérotation automatique partagée entre tous les niveaux
    private static int compteur = 0;
    private final int numero;
    
    /**
     * Crée un appartement à partir de ses murs délimiteurs et d'une hauteur sous plafond.
     * Le polygone visuel est dérivé directement des murs — pas besoin de le passer séparément.
     *
     * @param mursDelimiteurs murs formant le périmètre fermé de l'appartement
     * @param hauteurPlafond  hauteur sous plafond en mètres
     */
    public Appartement(List<Mur> mursDelimiteurs, double hauteurPlafond) {
        super("Appartement");
        this.mursDelimiteurs = new ArrayList<>(mursDelimiteurs);
        this.hauteurPlafond  = hauteurPlafond;
        this.pieces          = new ArrayList<>();

        compteur++;
        this.numero = compteur;
        this.couleurFond    = PaletteVisuelle.fondAppartement(numero);
        this.couleurContour = PaletteVisuelle.contourAppartement(numero);
    }

    /** Réinitialise le compteur (utile au chargement d'un projet). */
    public static void resetCompteur() { compteur = 0; }
    public static void setCompteur(int valeur) { compteur = Math.max(0, valeur); }

    // =========================================================================
    // DÉRIVATION DU POLYGONE DEPUIS LES MURS
    // =========================================================================

    /**
     * Dérive le polygone visuel depuis les murs délimiteurs.
     * Retourne la liste ordonnée des sommets en parcourant les murs
     * dans l'ordre de leur chaînage (point2 d'un mur = point1 du suivant).
     *
     * Le polygone est recalculé à chaque appel pour rester cohérent
     * si les murs sont modifiés (déplacement de sommet, etc.).
     */
    public List<Point> getPolygone() {
        List<Point> polygone = new ArrayList<>();
        for (Mur m : mursDelimiteurs) {
            polygone.add(m.getPoint1());
        }
        return polygone;
    }

    /**
     * Calcule la surface au sol de l'appartement (formule du lacet).
     */
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


    /**
     * Calcule le devis total de l'appartement :
     * somme des devis de toutes ses pièces plus le prix des menuiseries portées par ses murs.
     */
    public double calculerDevis() {
        double total = 0;
        for (Piece p : pieces) {
            total += p.calculerDevis();
        }
        for (Mur m : getMurs()) {
            total += m.calculerPrixMenuiseries();
        }
        return total;
    }

    // =========================================================================
    // GESTION DES PIÈCES
    // =========================================================================

    /**
     * Ajoute une pièce à l'appartement, définie par ses points de contour.
     * La hauteur sous plafond de l'appartement est transmise à la pièce.
     */
    public Piece ajouterPiece(List<Mur> murs) {
        Piece p = new Piece(murs, this.hauteurPlafond);
        this.pieces.add(p);
        return p;
    }

    public void supprimerPiece(Piece p) { this.pieces.remove(p); }

    // =========================================================================
    // IMPLÉMENTATION DESSIN
    // =========================================================================

    @Override
    public Color getColor() { return couleurContour; }

    @Override
    public void setColor(Color color) { this.couleurContour = color; }

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

        // Fond coloré semi-transparent
        gc.setFill(couleurFond);
        gc.fillPolygon(xs, ys, n);

        // Contour mis en évidence
        gc.setStroke(couleurContour);
        gc.setLineWidth(0.06);
        gc.strokePolygon(xs, ys, n);

        // Label centré au barycentre
        // Le canvas applique scale(1,-1) sur Y → on annule localement pour que
        // le texte s'affiche à l'endroit
        double cx = 0, cy = 0;
        for (Point p : polygone) { cx += p.getX(); cy += p.getY(); }
        cx /= n;
        cy /= n;

        gc.save();
        gc.scale(1, -1); // annule l'inversion Y du canvas
        gc.setFill(couleurContour);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 0.3));
        gc.fillText(toString(), cx - 0.5, -cy + 0.15);
        gc.restore();
    }

    // =========================================================================
    // GETTERS / SETTERS
    // =========================================================================

    public List<Mur> getMursDelimiteurs()              { return mursDelimiteurs; }
    public void setMursDelimiteurs(List<Mur> murs)     { this.mursDelimiteurs = new ArrayList<>(murs); }
    /**
     * Rassemble tous les murs de l'appartement :
     * les murs délimiteurs extérieurs ET toutes les cloisons intérieures des pièces.
     * Utile pour la détection et la sélection globale des revêtements.
     */
    private boolean estMurDejaPresent(List<Mur> liste, Mur murTest) {
        if (murTest == null) return false;
        double x1 = murTest.getPoint1().getX();
        double y1 = murTest.getPoint1().getY();
        double x2 = murTest.getPoint2().getX();
        double y2 = murTest.getPoint2().getY();
        
        for (Mur m : liste) {
            if (m == null) continue;
            double mx1 = m.getPoint1().getX();
            double my1 = m.getPoint1().getY();
            double mx2 = m.getPoint2().getX();
            double my2 = m.getPoint2().getY();
            
            // Sens 1: p1 -> p1 et p2 -> p2
            boolean sens1 = Math.abs(x1 - mx1) < 1e-2 && Math.abs(y1 - my1) < 1e-2 &&
                            Math.abs(x2 - mx2) < 1e-2 && Math.abs(y2 - my2) < 1e-2;
            // Sens 2: p1 -> p2 et p2 -> p1
            boolean sens2 = Math.abs(x1 - mx2) < 1e-2 && Math.abs(y1 - my2) < 1e-2 &&
                            Math.abs(x2 - mx1) < 1e-2 && Math.abs(y2 - my1) < 1e-2;
            
            if (sens1 || sens2) {
                return true;
            }
        }
        return false;
    }

    public List<Mur> getMurs() {
        List<Mur> tousLesMurs = new ArrayList<>();
        if (this.mursDelimiteurs != null) {
            for (Mur m : this.mursDelimiteurs) {
                if (!estMurDejaPresent(tousLesMurs, m)) {
                    tousLesMurs.add(m);
                }
            }
        }
        for (Piece p : this.pieces) {
            if (p.getMurs() != null) {
                for (Mur m : p.getMurs()) {
                    // Évite les doublons si un mur est partagé
                    if (!estMurDejaPresent(tousLesMurs, m)) {
                        tousLesMurs.add(m);
                    }
                }
            }
        }
        return tousLesMurs;
    }

    public List<Piece> getPieces()                     { return pieces; }
    public int getNbPieces()                           { return pieces.size(); }
    public double getHauteurPlafond()                  { return hauteurPlafond; }
    public void setHauteurPlafond(double h)            { this.hauteurPlafond = h; }
    public int getNumero()                             { return numero; }

    // =========================================================================
    // SÉRIALISATION
    // =========================================================================

    @Override
    public String toCSV() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format(java.util.Locale.US,
        "APPARTEMENT;%s;%d;%d;%.2f;%.2f",
        getId(), numero, getNbPieces(), hauteurPlafond, calculerSurface()));
    for (Point p : getPolygone()) {
        sb.append(String.format(java.util.Locale.US, ";%.2f;%.2f", p.getX(), p.getY()));
    }
    return sb.toString();
}

    @Override
    public String toString() {
        return "Appartement " + numero;
    }
}
