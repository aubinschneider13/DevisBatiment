package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un mur physique dans la structure du bâtiment.
 * <p>
 * Un mur hérite d'{@link ElementDeConstruction} et implémente {@link Dessin} pour 
 * s'afficher sur le canvas interactif. Il est défini par :
 * <ul>
 *   <li>Deux points géométriques en 2D ({@link Point}) marquant les extrémités de son axe central.</li>
 *   <li>Une hauteur sous plafond.</li>
 *   <li>Une liste d'ouvertures ({@link Ouverture}) telles que des portes et des fenêtres.</li>
 *   <li>Un type de mur ({@link TypeMur}) qui régit son apparence visuelle et les contraintes de pose d'ouvertures.</li>
 *   <li>Une structure biface représentée par deux instances indépendantes de {@link CoteMur} (Côté Gauche et Côté Droit) 
 *       afin d'accueillir des isolants et revêtements distincts.</li>
 * </ul>
 * </p>
 * 
 * @see ElementDeConstruction
 * @see Dessin
 * @see CoteMur
 * @see Ouverture
 */
public class Mur extends ElementDeConstruction implements Dessin {

    /** Le point initial (départ) du mur. */
    private Point point1;
    /** Le point final (fin) du mur. */
    private Point point2;
    /** La hauteur sous plafond de ce mur (en mètres). */
    private double hauteur;
    /** La liste des menuiseries posées sur ce mur (portes, fenêtres). */
    private List<Ouverture> listeOuvertures;

    /**
     * Types de murs gérés par l'application :
     * <ul>
     *   <li>{@link #NORMAL} : Cloison intérieure standard séparant des pièces ou logements.</li>
     *   <li>{@link #EXTERIEUR} : Façade extérieure de l'immeuble.</li>
     *   <li>{@link #ADJ_COULOIR} : Mur délimiteur en contact avec un couloir commun.</li>
     * </ul>
     */
    public enum TypeMur { NORMAL, EXTERIEUR, ADJ_COULOIR }

    /** Le type actuel du mur. Règle la couleur et la compatibilité des menuiseries et isolants. */
    private TypeMur typeMur = TypeMur.NORMAL;
    /** La couleur utilisée pour le tracé graphique de l'axe de ce mur. */
    private Color color;
    /** Compteur global statique pour l'attribution des identifiants uniques de murs. */
    private static int compteurMurs = 0;
    /** Identifiant séquentiel unique du mur pour les affichages et devis. */
    private int numeroUnique;
    /** Indique si le mur est un mur délimiteur d'appartement. */
    private boolean estDelimiteur = false;

    // Gestion des deux faces du mur
    /** Face gauche du mur. */
    private final CoteMur coteGauche;
    /** Face droite du mur. */
    private final CoteMur coteDroit;
    /** Lien vers le mur parent d'origine si cette instance est une copie graphique transitoire. */
    private Mur original;

    /**
     * Retourne le mur original dont est issu ce clone graphique (pour les vues isolées ou synchronisées).
     * Si aucune copie n'a été spécifiée, retourne l'instance actuelle.
     * 
     * @return Le mur d'origine de type {@link Mur}.
     */
    public Mur getOriginal() {
        return original != null ? original : this;
    }

    /**
     * Définit le mur d'origine parent associé à ce clone graphique.
     * 
     * @param original Le mur physique original.
     */
    public void setOriginal(Mur original) {
        this.original = original;
    }

    /**
     * Construit un mur complet avec extrémités géométriques et hauteur spécifiée.
     * Initialise automatiquement les deux faces (gauche et droite) associées à ce mur parent.
     * 
     * @param point1  Point géométrique initial du mur.
     * @param point2  Point géométrique final du mur.
     * @param hauteur Hauteur physique du mur (en mètres).
     */
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

    /**
     * Construit un mur avec une hauteur par défaut sous plafond fixée à 2.50 mètres.
     * 
     * @param point1 Point géométrique initial du mur.
     * @param point2 Point géométrique final du mur.
     */
    public Mur(Point point1, Point point2){
        this(point1, point2, 2.5);
    }

    /**
     * Retourne la face gauche associée à ce mur.
     * 
     * @return L'instance {@link CoteMur} gauche.
     */
    public CoteMur getCoteGauche() {
        return coteGauche;
    }

    /**
     * Retourne la face droite associée à ce mur.
     * 
     * @return L'instance {@link CoteMur} droite.
     */
    public CoteMur getCoteDroit() {
        return coteDroit;
    }

    /**
     * Insère une ouverture (menuiserie) sur le mur à la position relative donnée, 
     * sous réserve de la validation des règles de compatibilité métier.
     * <p>
     * <b>Règles de compatibilité strictes :</b>
     * <ul>
     *   <li>Anti-doublon basé sur une tolérance de distance relative de 2% (deux ouvertures ne peuvent pas se chevaucher).</li>
     *   <li>Les fenêtres ne sont autorisées <b>que</b> sur les façades extérieures ({@link TypeMur#EXTERIEUR}).</li>
     *   <li>Les portes ne sont <b>pas</b> autorisées sur les façades extérieures.</li>
     *   <li>Les portes posées sur des murs délimiteurs d'appartement ne sont tolérées <b>que</b> si 
     *       le mur est adjacent au couloir ({@link TypeMur#ADJ_COULOIR}), assurant ainsi une issue réglementaire.</li>
     * </ul>
     * </p>
     * 
     * @param o L'ouverture de type {@link Ouverture} (Porte ou Fenetre) à ajouter.
     */
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

    /**
     * Attribue une couleur d'affichage distinctive selon la nature du mur.
     * 
     * @param type Le type du mur à caractériser.
     * @return Une instance {@link Color} associée au type.
     */
    private Color couleurPourType(TypeMur type) {
        return switch (type) {
            case NORMAL      -> Color.web("#1a1a1a");       // noir
            case EXTERIEUR   -> Color.web("#7733b8");       // violet très foncé
            case ADJ_COULOIR -> Color.web("#E65757");       // rouge très foncé
        };
    }

    /**
     * Modifie le type du mur et met à jour automatiquement sa couleur d'affichage.
     * 
     * @param type Le nouveau type du mur.
     */
    public void setTypeMur(TypeMur type) {
        this.typeMur = type;
        this.color = couleurPourType(type);
    }

    /**
     * Calcule la longueur linéaire du mur en deux dimensions.
     * 
     * @return La distance euclidienne entre le point 1 et le point 2 (en mètres).
     */
    public double calculerLongueur(){
        double dX = this.point2.getX() - this.point1.getX();
        double dY = this.point2.getY() - this.point1.getY();
        return Math.sqrt(dX*dX + dY*dY);
    }

    /**
     * Calcule la surface brute verticale du mur (sans déduction d'ouvertures).
     * 
     * @return La surface brute en mètres carrés (m²).
     */
    public double calculerSurface(){
        return calculerLongueur() * this.hauteur;
    }

    /**
     * Calcule la surface nette verticale du mur en déduisant rigoureusement 
     * la surface occupée par toutes les menuiseries (portes et fenêtres).
     * <p>
     * <b>Sécurisation :</b> Si la somme des surfaces des ouvertures dépasse 
     * accidentellement la surface brute du mur, le calcul est verrouillé à 
     * {@code 0.0 m²} (via {@link Math#max}) pour prévenir tout devis négatif erroné.
     * </p>
     * 
     * @return La surface nette utile en mètres carrés (m²).
     */
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

    /**
     * Calcule le coût forfaitaire global lié à l'achat et à la pose de toutes 
     * les ouvertures (portes et fenêtres) installées sur ce mur.
     * 
     * @return Le coût cumulé forfaitaire en euros (€).
     */


    /**
     * Calcule le prix global des revêtements appliqués sur ce mur, en cumulant 
     * le coût des matériaux posés sur la face gauche et la face droite.
     * 
     * @return Le coût total des revêtements en euros (€).
     */
    public double calculerPrixRevetement() {
        return (coteGauche != null ? coteGauche.calculerPrixRevetement() : 0.0)
             + (coteDroit != null ? coteDroit.calculerPrixRevetement() : 0.0);
    }

    /**
     * Calcule la distance orthogonale minimale (euclidienne) entre un point donné et le segment du mur.
     * Utile pour la détection fine des sélections utilisateur et le snapping géométrique.
     * 
     * @param p Le point de référence (ex: clic de souris).
     * @return La distance minimale en mètres.
     */
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
     * Calcule le paramètre de projection normalisé {@code t} d'un point {@code P} 
     * projeté orthogonalement sur le segment de mur.
     * 
     * @param p Le point à projeter.
     * @return La valeur réelle {@code t} clampée dans l'intervalle {@code [0.0, 1.0]} 
     *         représentant la position relative sur le mur (0 = point1, 1 = point2).
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
     * Détermine les coordonnées absolues en deux dimensions correspondant au paramètre {@code t} sur le mur.
     * 
     * @param t Position relative sur le mur, idéalement dans l'intervalle {@code [0.0, 1.0]}.
     * @return Le {@link Point} 2D correspondant.
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

    /**
     * Effectue le tracé graphique complet du mur et de ses détails sur le canvas interactif.
     * <p>
     * Cette méthode restitue visuellement :
     * <ul>
     *   <li>L'épaisseur et le décalage (offset) de chaque face (gauche/droite) par rapport à l'axe.</li>
     *   <li>Le rendu de couleur spécifique pour les isolants posés (Orange) ou finitions (Vert à gauche, Bleu à droite).</li>
     *   <li>L'axe central en pointillés gris discrets.</li>
     *   <li>Les symboles des portes (vantail + arc de débattement orientable) et des fenêtres (double vitrage bleu ciel).</li>
     *   <li>Les poignées d'extrémité représentées par des ronds blancs cerclés de bleu pour l'édition géométrique.</li>
     * </ul>
     * </p>
     * 
     * @param gc Le contexte graphique {@link GraphicsContext} de JavaFX.
     */
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
            if (coteGauche.getRevetements().get(0) instanceof Isolant) {
                gc.setStroke(Color.web("#d35400")); // Orange pour isolant posé à gauche
            } else {
                gc.setStroke(Color.web("#2ecc71")); // Vert pour revêtement posé à gauche
            }
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
            if (coteDroit.getRevetements().get(0) instanceof Isolant) {
                gc.setStroke(Color.web("#d35400")); // Orange pour isolant posé à droite
            } else {
                gc.setStroke(Color.web("#3498db")); // Bleu pour revêtement posé à droite
            }
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

    /** Dessine le vantail et l'arc de débattement de la porte en fonction de son orientation. */
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

    /** Dessine le symbole de la fenêtre (vitrage intérieur et montants latéraux). */
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

    /**
     * Sérialise ce mur et l'ensemble de ses composants (revêtements, menuiseries) 
     * en une ligne brute au format CSV compatible avec le {@link GestionnaireSauvegarde}.
     * <p>
     * <b>Format CSV généré :</b>
     * <pre>
     * MUR;[ID];[P1_X];[P1_Y];[P2_X];[P2_Y];[HAUTEUR];[IDS_REVETEMENTS_GAUCHE];[IDS_REVETEMENTS_DROIT];OUVERTURES;[NB_OUVERTURES];[TYPE_1];[POS_1];[INV_1]...
     * </pre>
     * </p>
     * 
     * @return La chaîne sérialisée.
     */
    @Override
    public String toCSV() {
        String base = String.format(java.util.Locale.US,
            "MUR;%s;%.2f;%.2f;%.2f;%.2f;%.2f",
            super.getId(),
            point1.getX(), point1.getY(),
            point2.getX(), point2.getY(),
            hauteur);
        
        String idGauche = "VIDE";
        if (coteGauche.getRevetements() != null && !coteGauche.getRevetements().isEmpty()) {
            java.util.List<String> ids = coteGauche.getRevetements().stream().map(Revetement::getId).toList();
            idGauche = String.join(",", ids);
        }

        String idDroit = "VIDE";
        if (coteDroit.getRevetements() != null && !coteDroit.getRevetements().isEmpty()) {
            java.util.List<String> ids = coteDroit.getRevetements().stream().map(Revetement::getId).toList();
            idDroit = String.join(",", ids);
        }

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

    /**
     * Retourne une description concise de l'instance du mur pour les listes de l'IHM.
     * 
     * @return Chaîne descriptive incluant le numéro unique et la longueur du mur.
     */
    @Override
    public String toString() {
        return String.format("Mur n°%d (%.2f m)", numeroUnique, calculerLongueur());
    }
    
    public boolean isEstDelimiteur() { return estDelimiteur; }
    public void setEstDelimiteur(boolean estDelimiteur) { this.estDelimiteur = estDelimiteur; }
}
