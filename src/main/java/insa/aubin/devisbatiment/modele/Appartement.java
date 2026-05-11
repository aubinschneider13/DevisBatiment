package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;


public class Appartement extends ElementDeConstruction implements Dessin {

    private double hauteurPlafond;
    private List<Piece> pieces;

    // --- Champs visuels ---
    // Polygone de la zone délimitée par les murs (coordonnées modèle)
    private List<Point> polygone;
    private Color couleurFond;
    private Color couleurContour;

    // Numérotation automatique (statique, partagée entre tous les niveaux)
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

    public Appartement(int nbPieces, double hauteurPlafond) {
        super("Appartement");
        this.hauteurPlafond = hauteurPlafond;
        this.pieces = new ArrayList<>();
        this.polygone = new ArrayList<>();

        // Attribution du numéro et de la couleur
        compteur++;
        this.numero = compteur;
        int idx = (numero - 1) % PALETTE_FOND.length;
        this.couleurFond    = PALETTE_FOND[idx];
        this.couleurContour = PALETTE_CONTOUR[idx];
    }

    /** Réinitialise le compteur (utile au chargement d'un projet). */
    public static void resetCompteur() { compteur = 0; }

    // --- Champs visuels : getters / setters ---

    public void setPolygone(List<Point> polygone) { this.polygone = polygone; }
    public List<Point> getPolygone()              { return polygone; }

    // --- Implémentation Dessin ---

    @Override
    public Color getColor() { return couleurContour; }

    @Override
    public void setColor(Color color) { this.couleurContour = color; }

    @Override
    public void dessiner(GraphicsContext gc) {
        if (polygone == null || polygone.size() < 3) return;

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

        // Label au barycentre du polygone
        // Le canvas applique un scale(1, -1) sur Y → on annule localement pour que
        // le texte reste lisible (sinon il serait affiché à l'envers)
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

    // --- Modèle métier inchangé ---

    public Piece ajouterPiece(List<Point> points) {
        Piece p = new Piece(points, this.hauteurPlafond);
        this.pieces.add(p);
        return p;
    }

    public int getNbPieces()                          { return pieces.size(); }
    public double getHauteurPlafond()                 { return hauteurPlafond; }
    public void setHauteurPlafond(double hauteurPlafond) { this.hauteurPlafond = hauteurPlafond; }
    public List<Piece> getPieces()                    { return pieces; }

    @Override
    public String toCSV() {
        return "APPARTEMENT;" + getId() + ";" + getNbPieces() + ";" + hauteurPlafond;
    }

    @Override
    public String toString() {
        return "Appartement " + numero;
    }
}