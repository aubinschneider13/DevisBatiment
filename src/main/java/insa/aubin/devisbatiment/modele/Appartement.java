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

    // Palette tournante pour distinguer visuellement les appartements
    private static final Color[] PALETTE_FOND = {
        Color.web("#e74c3c", 0.18), // rouge
        Color.web("#2ecc71", 0.18), // vert
        Color.web("#f39c12", 0.18), // orange
        Color.web("#9b59b6", 0.18), // violet
        Color.web("#1abc9c", 0.18), // turquoise
    };
    private static final Color[] PALETTE_CONTOUR = {
        Color.web("#c0392b"),
        Color.web("#27ae60"),
        Color.web("#e67e22"),
        Color.web("#8e44ad"),
        Color.web("#16a085"),
    };

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
        int idx = (numero - 1) % PALETTE_FOND.length;
        this.couleurFond    = PALETTE_FOND[idx];
        this.couleurContour = PALETTE_CONTOUR[idx];
    }

    /** Réinitialise le compteur (utile au chargement d'un projet). */
    public static void resetCompteur() { compteur = 0; }

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

    // =========================================================================
    // GESTION DES PIÈCES
    // =========================================================================

    /**
     * Ajoute une pièce à l'appartement, définie par ses points de contour.
     * La hauteur sous plafond de l'appartement est transmise à la pièce.
     */
    public Piece ajouterPiece(List<Point> points) {
        Piece p = new Piece(points, this.hauteurPlafond);
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
    public List<Piece> getPieces()                     { return pieces; }
    public int getNbPieces()                           { return pieces.size(); }
    public double getHauteurPlafond()                  { return hauteurPlafond; }
    public void setHauteurPlafond(double h)            { this.hauteurPlafond = h; }

    // =========================================================================
    // SÉRIALISATION
    // =========================================================================

    @Override
    public String toCSV() {
        return "APPARTEMENT;" + getId() + ";" + getNbPieces() + ";" + hauteurPlafond;
    }

    @Override
    public String toString() {
        return "Appartement " + numero;
    }
}