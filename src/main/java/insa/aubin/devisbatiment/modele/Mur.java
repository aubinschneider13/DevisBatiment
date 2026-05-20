package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class Mur extends SurfaceAvecRevetement implements Dessin {

    private Point point1;
    private Point point2;
    private double hauteur;
    private List<Ouverture> listeOuvertures;
    public enum TypeMur { NORMAL, EXTERIEUR, ADJ_COULOIR }
    private TypeMur typeMur = TypeMur.NORMAL;
    private Color color;
    private static int compteurMurs = 0;
    private int numeroUnique;

    public Mur(Point point1, Point point2, double hauteur) {
        super("Mur");
        this.point1 = point1;
        this.point2 = point2;
        this.hauteur = hauteur;
        this.listeOuvertures = new ArrayList<>();
        this.color = couleurPourType(TypeMur.NORMAL);
        compteurMurs++;
        this.numeroUnique = compteurMurs;
    }

    public Mur(Point point1, Point point2){
        this(point1, point2, 2.5);
    }

    //Méthode pour ajouter une ouverture dans la liste
    public void ajouterOuverture(Ouverture o) {
        if (o == null) return;
        if (o instanceof Fenetre && typeMur != TypeMur.EXTERIEUR) {
            return; // fenêtre uniquement sur mur extérieur
        }
        if (o instanceof Porte && typeMur == TypeMur.EXTERIEUR) {
            return; // pas de porte sur mur extérieur
        }
        this.listeOuvertures.add(o);
    }

    private Color couleurPourType(TypeMur type) {
        return switch (type) {
            case NORMAL      -> Color.web("#1a1a1a");       // noir
            case EXTERIEUR   -> Color.web("#7733b8");       // violet très foncé
            case ADJ_COULOIR -> Color.web("#4e0a0a");       // rouge très foncé
        };
    }

    public void setTypeMur(TypeMur type) {
        this.typeMur = type;
        this.color = couleurPourType(type);
    }

    public double calculerLongueur(){
        double dX = this.point2.getX() - this.point1.getX();
        double dY = this.point2.getY() - this.point1.getY();
        return Math.sqrt(dX*dX + dY*dY);
    }

    //Méthode pour calculer la surface brute du mur (cad sans les ouvertures)
    @Override
    public double calculerSurface(){
        return calculerLongueur() * this.hauteur;
    }

    public double calculerSurfaceNette(){
        double surfaceBrute = this.calculerSurface();
        double surfaceOuverture = 0;
        if (listeOuvertures != null) {
            for (Ouverture o : listeOuvertures) {
                if (o != null) {
                    surfaceOuverture += o.getLargeur() * o.getHauteur();
                }
            }
        }
        return Math.max(0.0, surfaceBrute - surfaceOuverture);
    }

    // Dans Mur.java — surcharger calculerPrixRevetement()
    @Override
    public double calculerPrixRevetement() {
        double total = 0;
        if (getRevetements() != null) {
            for (Revetement r : getRevetements()) {
                if (r != null) {
                    total += (float) r.calculerPrixTotal(calculerSurfaceNette());
                }
            }
        }
        return total;
    }

    /** Calcule la distance la plus courte entre un point et ce segment de mur */
    public double distanceA(Point p) {
        double x1 = point1.getX(), y1 = point1.getY();
        double x2 = point2.getX(), y2 = point2.getY();
        double px = p.getX(), py = p.getY();

        double l2 = Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);
        if (l2 == 0) return Math.sqrt(Math.pow(px - x1, 2) + Math.pow(py - y1, 2));

        double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
        t = Math.max(0, Math.min(1, t));

        return Math.sqrt(Math.pow(px - (x1 + t * (x2 - x1)), 2) + Math.pow(py - (y1 + t * (y2 - y1)), 2));
    }

    /**
     * Calcule la valeur t ∈ [0,1] correspondant à la projection
     * d'un point P sur ce segment de mur.
     * Retourne la position relative : 0 = point1, 1 = point2.
     */
    public double calculerPositionSurMur(Point p) {
        double x1 = point1.getX(), y1 = point1.getY();
        double x2 = point2.getX(), y2 = point2.getY();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double l2 = dx*dx + dy*dy;

        if (l2 == 0) return 0.5;

        double t = ((p.getX()-x1)*dx + (p.getY()-y1)*dy) / l2;

        return Math.max(0.0, Math.min(1.0, t));
    }

    /**
     * Calcule les coordonnées XY d'un point à la position t sur le mur.
     */
    public Point getPointSurMur(double t) {
        double x = point1.getX() + t * (point2.getX() - point1.getX());
        double y = point1.getY() + t * (point2.getY() - point1.getY());
        return new Point(x, y);
    }

    //Getters et Setters
    public Point getPoint1() { return point1; }
    public void setPoint1(Point point1) { this.point1 = point1; }
    public Point getPoint2() { return point2; }
    public void setPoint2(Point point2) { this.point2 = point2; }
    public double getHauteur(){ return hauteur; }
    public void setHauteur(double hauteur){ this.hauteur = hauteur; }
    public List<Ouverture> getListeOuvertures(){ return listeOuvertures; }
    public void setListeOuvertures(List<Ouverture> listeOuvertures){ this.listeOuvertures = listeOuvertures; }
    public TypeMur getTypeMur() { return typeMur; }

    //Méthodes pour éléments graphiques
    @Override
    public Color getColor() { return color; }

    @Override
    public void setColor(Color color) { this.color = color; }

    @Override
    public void dessiner(GraphicsContext gc) {
        // --- Retour visuel selon la présence d'un revêtement ---
        boolean aUnRevetement = (this.getRevetements() != null && !this.getRevetements().isEmpty());

        if (aUnRevetement) {
            gc.setStroke(Color.web("#2ecc71")); // Vert pour indiquer qu'un revêtement est posé
            gc.setLineWidth(0.15); // Un peu plus épais
        } else {
            gc.setStroke(this.color); // Couleur normale selon le type (noir/violet)
            gc.setLineWidth(0.1);
        }
        // -----------------------------------------------------------------

        // Dessin du mur
        gc.strokeLine(
                point1.getX(), point1.getY(),
                point2.getX(), point2.getY()
        );

        double angle = Math.toDegrees(Math.atan2(
                point2.getY() - point1.getY(),
                point2.getX() - point1.getX()
        ));

        // Chaque ouverture à SA position réelle
        for (Ouverture o : listeOuvertures) {
            gc.save();

            // Position exacte selon positionSurMur
            Point pos = getPointSurMur(o.getPositionSurMur());
            gc.translate(pos.getX(), pos.getY());
            gc.rotate(angle);

            if (o instanceof Porte) {
                dessinerSymbolePorte(gc, o.getLargeur());
            } else if (o instanceof Fenetre) {
                dessinerSymboleFenetre(gc, o.getLargeur());
            }

            gc.restore();
        }

        // Coins du mur
        double rayon = 0.08;
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(0.02);
        gc.fillOval(point1.getX()-rayon, point1.getY()-rayon, rayon*2, rayon*2);
        gc.strokeOval(point1.getX()-rayon, point1.getY()-rayon, rayon*2, rayon*2);
        gc.fillOval(point2.getX()-rayon, point2.getY()-rayon, rayon*2, rayon*2);
        gc.strokeOval(point2.getX()-rayon, point2.getY()-rayon, rayon*2, rayon*2);
    }

    private void dessinerSymbolePorte(GraphicsContext gc, double largeur) {
        double l = largeur;

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.15);
        gc.strokeLine(-l/2, 0, l/2, 0);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.05);
        gc.strokeLine(-l/2, -0.08, -l/2, 0.08);
        gc.strokeLine( l/2, -0.08,  l/2, 0.08);

        gc.setStroke(Color.web("#8B4513")); // marron
        gc.setLineWidth(0.04);
        gc.setLineDashes(0.05, 0.05);
        gc.strokeArc(-l/2, -l, l, l, 270, 90, javafx.scene.shape.ArcType.OPEN);
        gc.setLineDashes(0); // reset

        gc.setStroke(Color.web("#8B4513"));
        gc.setLineWidth(0.05);
        gc.strokeLine(-l/2, 0, -l/2, -l);
    }

    private void dessinerSymboleFenetre(GraphicsContext gc, double largeur) {
        double l = largeur;
        double ep = 0.06; // épaisseur du vitrage

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.15);
        gc.strokeLine(-l/2, 0, l/2, 0);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.05);
        gc.strokeLine(-l/2, -0.08, -l/2, 0.08);
        gc.strokeLine( l/2, -0.08,  l/2, 0.08);

        gc.setStroke(Color.DEEPSKYBLUE);
        gc.setLineWidth(0.04);
        gc.strokeLine(-l/2, -ep, l/2, -ep);
        gc.strokeLine(-l/2,  ep, l/2,  ep);
    }

    @Override
    public String toCSV() {
        String base = String.format(java.util.Locale.US,
            "MUR;%s;%.2f;%.2f;%.2f;%.2f;%.2f",
            super.getId(),
            point1.getX(), point1.getY(),
            point2.getX(), point2.getY(),
            hauteur);
        if (getRevetements() != null && !getRevetements().isEmpty()) {
            return base + ";" + getRevetements().get(0).getId();
        }
        return base + ";VIDE";
    }

    @Override
    public String toString() {
        return String.format("Mur n°%d (%.2f m)", numeroUnique, calculerLongueur());
    }
}