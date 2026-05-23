package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class Mur extends ElementDeConstruction implements Dessin {

    private Point point1;
    private Point point2;
    private double hauteur;
    private List<Ouverture> listeOuvertures;
    public enum TypeMur { NORMAL, EXTERIEUR, ADJ_COULOIR }
    private TypeMur typeMur = TypeMur.NORMAL;
    private Color color;
    private static int compteurMurs = 0;
    private int numeroUnique;
    private boolean estDelimiteur = false;
    

    // Gestion des deux faces du mur
    private final CoteMur coteGauche;
    private final CoteMur coteDroit;
    private Mur original;

    public Mur getOriginal() {
        return original != null ? original : this;
    }

    public void setOriginal(Mur original) {
        this.original = original;
    }

    public Mur(Point point1, Point point2, double hauteur) {
        super("Mur");
        this.point1 = point1;
        this.point2 = point2;
        this.hauteur = hauteur;
        this.listeOuvertures = new ArrayList<>();
        this.color = couleurPourType(TypeMur.NORMAL);
        compteurMurs++;
        this.numeroUnique = compteurMurs;

        // Initialisation des deux côtés du mur
        this.coteGauche = new CoteMur(this);
        this.coteDroit = new CoteMur(this);
    }

    public Mur(Point point1, Point point2){
        this(point1, point2, 2.5);
    }

    public CoteMur getCoteGauche() {
        return coteGauche;
    }

    public CoteMur getCoteDroit() {
        return coteDroit;
    }

    // Méthode pour ajouter une ouverture dans la liste
    public void ajouterOuverture(Ouverture o) {
        if (o == null) return;

        // Anti-doublon strict basé sur la position
        for (Ouverture existante : this.listeOuvertures) {
            if (Math.abs(existante.getPositionSurMur() - o.getPositionSurMur()) < 0.02) {
                return;
            }
        }

        if (o instanceof Fenetre && typeMur != TypeMur.EXTERIEUR) {
            return; // fenêtre uniquement sur mur extérieur
        }

        if (o instanceof Porte && typeMur == TypeMur.EXTERIEUR) {
            return; // pas de porte sur mur extérieur
        }

        if (o instanceof Porte && estDelimiteur && typeMur != TypeMur.ADJ_COULOIR) {
            return; // pas de porte sur mur délimiteur non adjacent au couloir
        }

        this.listeOuvertures.add(o);
    }

    private Color couleurPourType(TypeMur type) {
        return switch (type) {
            case NORMAL      -> Color.web("#1a1a1a");       // noir
            case EXTERIEUR   -> Color.web("#7733b8");       // violet très foncé
            case ADJ_COULOIR -> Color.web("#E65757");       // rouge très foncé
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

    // Surface brute du mur (pour compatibilité / calculs d'échelle)
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

    public double calculerPrixMenuiseries() {
        double total = 0.0;
        if (listeOuvertures != null) {
            for (Ouverture o : listeOuvertures) {
                if (o != null) {
                    total += o.getPrixForfaitaire();
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

    // Getters et Setters
    public Point getPoint1() { return point1; }
    public void setPoint1(Point point1) { this.point1 = point1; }
    public Point getPoint2() { return point2; }
    public void setPoint2(Point point2) { this.point2 = point2; }
    public double getHauteur(){ return hauteur; }
    public void setHauteur(double hauteur){ this.hauteur = hauteur; }
    public List<Ouverture> getListeOuvertures(){ return listeOuvertures; }
    public void setListeOuvertures(List<Ouverture> listeOuvertures){ this.listeOuvertures = listeOuvertures; }
    public TypeMur getTypeMur() { return typeMur; }

    @Override
    public Color getColor() { return color; }

    @Override
    public void setColor(Color color) { this.color = color; }

    @Override
    public void dessiner(GraphicsContext gc) {
        double x1 = point1.getX(), y1 = point1.getY();
        double x2 = point2.getX(), y2 = point2.getY();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);

        // Vecteur normal unitaire (gauche)
        double nx = 0, ny = 0;
        if (len > 0) {
            nx = -dy / len;
            ny = dx / len;
        }

        double offset = 0.08; // 8 cm d'écart

        // Dessiner le côté gauche
        gc.save();
        boolean aRevG = coteGauche.getRevetements() != null && !coteGauche.getRevetements().isEmpty();
        if (aRevG) {
            gc.setStroke(Color.web("#2ecc71")); // Vert pour revêtement posé à gauche
            gc.setLineWidth(0.08);
        } else {
            gc.setStroke(this.color);
            gc.setLineWidth(0.03);
        }
        gc.strokeLine(x1 + offset * nx, y1 + offset * ny, x2 + offset * nx, y2 + offset * ny);
        gc.restore();

        // Dessiner le côté droit (offset inversé)
        gc.save();
        boolean aRevD = coteDroit.getRevetements() != null && !coteDroit.getRevetements().isEmpty();
        if (aRevD) {
            gc.setStroke(Color.web("#3498db")); // Bleu pour revêtement posé à droite
            gc.setLineWidth(0.08);
        } else {
            gc.setStroke(this.color);
            gc.setLineWidth(0.03);
        }
        gc.strokeLine(x1 - offset * nx, y1 - offset * ny, x2 - offset * nx, y2 - offset * ny);
        gc.restore();

        // Dessiner l'axe central en pointillés discrets
        gc.save();
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(0.02);
        gc.setLineDashes(0.05, 0.05);
        gc.strokeLine(x1, y1, x2, y2);
        gc.restore();

        // Dessiner les ouvertures sur l'axe central
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        for (Ouverture o : listeOuvertures) {
            gc.save();
            Point pos = getPointSurMur(o.getPositionSurMur());
            gc.translate(pos.getX(), pos.getY());
            gc.rotate(angle);

            if (o instanceof Porte p) {
                dessinerSymbolePorte(gc, p.getLargeur(), p.isOuvertureInversee());
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
        gc.fillOval(x1 - rayon, y1 - rayon, rayon * 2, rayon * 2);
        gc.strokeOval(x1 - rayon, y1 - rayon, rayon * 2, rayon * 2);
        gc.fillOval(x2 - rayon, y2 - rayon, rayon * 2, rayon * 2);
        gc.strokeOval(x2 - rayon, y2 - rayon, rayon * 2, rayon * 2);
    }

    private void dessinerSymbolePorte(GraphicsContext gc, double largeur, boolean inversee) {
        double l = largeur;

        // 1. Ouverture dans le mur
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.15);
        gc.strokeLine(-l / 2, 0, l / 2, 0);

        // 2. Jambages
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.05);
        gc.strokeLine(-l / 2, -0.08, -l / 2, 0.08);
        gc.strokeLine( l / 2, -0.08,  l / 2, 0.08);

        // 3. Vantail & Arc de débattement (sensibles à l'inversion)
        gc.save();
        if (inversee) {
            gc.scale(1, -1);
        }

        // Vantail
        gc.setStroke(Color.web("#8B4513"));
        gc.setLineWidth(0.06);
        gc.strokeLine(-l / 2, 0, -l / 2, -l);

        // Arc de débattement
        gc.setLineWidth(0.04);
        gc.setLineDashes(0.06, 0.04);
        gc.strokeArc(-l / 2 - l, -l, l * 2, l * 2, 0, 90, javafx.scene.shape.ArcType.OPEN);
        gc.setLineDashes(0);
        gc.restore();
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
        
        String idGauche = (coteGauche.getRevetements() != null && !coteGauche.getRevetements().isEmpty()) 
                ? coteGauche.getRevetements().get(0).getId() : "VIDE";
        String idDroit = (coteDroit.getRevetements() != null && !coteDroit.getRevetements().isEmpty()) 
                ? coteDroit.getRevetements().get(0).getId() : "VIDE";

        StringBuilder ouverturesCsv = new StringBuilder();
        int nbOuvertures = listeOuvertures != null ? listeOuvertures.size() : 0;
        ouverturesCsv.append(";OUVERTURES;").append(nbOuvertures);
        if (listeOuvertures != null) {
            for (Ouverture o : listeOuvertures) {
                if (o instanceof Porte p) {
                    ouverturesCsv.append(String.format(java.util.Locale.US,
                            ";PORTE;%.4f;%d",
                            p.getPositionSurMur(),
                            p.isOuvertureInversee() ? 1 : 0));
                } else if (o instanceof Fenetre) {
                    ouverturesCsv.append(String.format(java.util.Locale.US,
                            ";FENETRE;%.4f;0",
                            o.getPositionSurMur()));
                }
            }
        }

        return base + ";" + idGauche + ";" + idDroit + ouverturesCsv;
    }

    @Override
    public String toString() {
        return String.format("Mur n°%d (%.2f m)", numeroUnique, calculerLongueur());
    }
    
    public boolean isEstDelimiteur() { return estDelimiteur; }
    public void setEstDelimiteur(boolean estDelimiteur) { this.estDelimiteur = estDelimiteur; }
}
