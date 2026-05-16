package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.PieceView;
import java.util.ArrayList;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.application.Platform;

import java.util.List;

public class PieceControleur {

    // CONSTANTES D'ÉTAT
    public static final int ETAT_RIEN    = 0;
    public static final int ETAT_MUR     = 30;
    public static final int ETAT_PORTE   = 40;
    public static final int ETAT_FENETRE = 50;

    private PieceView vue;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;
    private int etat = ETAT_RIEN;
    private Point p1, p2;
    private Mur mur1, mur2;
    private int etapeRectangle;
    private Mur murSurvolé;
    private List<Point> polygoneAppartement = null;

    public PieceControleur(PieceView vue, Stage stage,
                           GestionnaireSauvegarde gestionnaire) {
        this.vue          = vue;
        this.stage        = stage;
        this.gestionnaire = gestionnaire;
    }

    // =========================================================================
    // NAVIGATION — conservées pour compatibilité, mais désormais gérées par
    // AppControleur. Ces méthodes peuvent rester sans nuire.
    // =========================================================================

    public void mettreFenetrePleinEcran() {
        Platform.runLater(() -> {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setResizable(true);
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        });
    }

    /**
     * Retour au dashboard.
     * ✅ Dans la nouvelle architecture, c'est AppControleur.retourDashboard()
     * qui est appelé via le bouton Retour de ToolBarView. Cette méthode est
     * conservée pour compatibilité ascendante uniquement.
     */
    public void retourDashboard() {
        DashBoardView dashBoardView = new DashBoardView();
        Scene dashScene = new Scene(dashBoardView);
        stage.setScene(dashScene);
        stage.setTitle("InsaBuilder - Tableau de bord");
        new DashBoardControleur(dashBoardView, stage, gestionnaire);
        mettreFenetrePleinEcran();
    }

    // =========================================================================
    // GESTION DES ÉTATS / OUTILS
    // =========================================================================

    public void changerEtat(int nouvelEtat) {
        this.etat = nouvelEtat;
        this.vue.getCanvas().setPanActif(this.etat == ETAT_RIEN);
        this.vue.getOptionsMurVue().setVisible(this.etat == ETAT_MUR);

        // ✅ Message contextuel selon l'outil actif
        switch (nouvelEtat) {
            case ETAT_RIEN ->
                this.vue.setInstructions(
                    "Navigation active — Clic droit ou molette pour déplacer la vue"
                );
            case ETAT_MUR -> {
                if (this.vue.getOptionsMurVue().estRectangulaire()) {
                    this.vue.setInstructions(
                        "Mode rectangle — Cliquez pour définir le premier coin"
                    );
                } else {
                    this.vue.setInstructions(
                        "Mode libre — Cliquez pour définir le début du mur"
                    );
                }
            }
            case ETAT_PORTE ->
                this.vue.setInstructions(
                    "Survolez un mur puis cliquez pour insérer une porte — Échap pour annuler"
                );
            case ETAT_FENETRE ->
                this.vue.setInstructions(
                    "Survolez un mur puis cliquez pour insérer une fenêtre — Échap pour annuler"
                );
        }
    }

    /**
     * Convertit les coordonnées de la souris en coordonnées du modèle
     * en prenant en compte le zoom et le magnétisme de la grille.
     */
    public Point posInModel(double x, double y) {
        Point2D p = this.vue.getCanvas().snapToGrid(x, y);
        return new Point(p.getX(), p.getY());
    }

    public void btnMur(ActionEvent t) {
        this.changerEtat(ETAT_MUR);
        this.vue.getOptionsMurVue().setVisible(true);
    }

    // =========================================================================
    // CLICS ET MOUVEMENTS SOURIS
    // =========================================================================

    public void clicDansZoneDeDessin(MouseEvent event) {
        Point pClic = this.posInModel(event.getX(), event.getY());

        if (!estDansAppartement(pClic.getX(), pClic.getY())) return;

        if (this.etat == ETAT_MUR) {
            genererDessinMur(pClic);
        } else if (this.etat == ETAT_PORTE) {
            Mur cible = trouverMurProche(pClic);
            if (cible != null) {
                double t = cible.calculerPositionSurMur(pClic);
                double marge = Porte.LARGEUR_PORTE / (2 * cible.calculerLongueur());
                t = Math.max(marge, Math.min(1.0 - marge, t));
                Porte nouvellePorte = new Porte(t);
                cible.ajouterOuverture(nouvellePorte);
            }
        } else if (this.etat == ETAT_FENETRE) {
            Mur cible = trouverMurProche(pClic);
            if (cible != null) {
                double t = cible.calculerPositionSurMur(pClic);
                double marge = Fenetre.COTE_FENETRE / (2 * cible.calculerLongueur());
                t = Math.max(marge, Math.min(1.0 - marge, t));
                Fenetre nouvelleFenetre = new Fenetre(t);
                cible.ajouterOuverture(nouvelleFenetre);
            }
        }
    }

    public void genererDessinMur(Point pClic) {
        boolean modRect = this.vue.getOptionsMurVue().estRectangulaire();
        if (modRect) {
            switch (etapeRectangle) {
                case 0:
                    this.p1   = pClic;
                    this.mur1 = new Mur(p1, p1);
                    this.mur1.setTypeMur(Mur.TypeMur.NORMAL);
                    this.vue.getCanvas().ajouterElement(this.mur1);
                    this.vue.setInstructions(
                        "Premier coin posé — Cliquez pour définir la longueur");
                    this.etapeRectangle = 1;
                    break;
                case 1:
                    this.p2   = pClic;
                    this.mur1.setPoint2(p2);
                    this.mur2 = new Mur(p2, p2);
                    this.mur2.setTypeMur(Mur.TypeMur.NORMAL);
                    this.vue.getCanvas().ajouterElement(this.mur2);
                    this.vue.setInstructions(
                        "Longueur définie — Cliquez pour définir la largeur");
                    this.etapeRectangle = 2;
                    break;
                case 2:
                    Point p3 = calculerPointOrthogonal(this.p2, pClic);
                    this.mur2.setPoint2(p3);
                    Point p4 = new Point(
                        p1.getX() + (p3.getX() - p2.getX()),
                        p1.getY() + (p3.getY() - p2.getY())
                    );
                    Mur mur3 = new Mur(p3, p4);
                    mur3.setTypeMur(Mur.TypeMur.NORMAL);
                    this.vue.getCanvas().ajouterElement(mur3);
                    Mur mur4 = new Mur(p4, p1);
                    mur4.setTypeMur(Mur.TypeMur.NORMAL);
                    this.vue.getCanvas().ajouterElement(mur4);
                    this.vue.setInstructions(
                        "Rectangle créé — Cliquez pour un nouveau mur ou changez d'outil");
                    this.etapeRectangle = 0;
                    this.mur1 = null;
                    this.mur2 = null;
                    break;
            }
        } else {
            // Mode libre
            if (this.mur1 == null) {
                this.mur1 = new Mur(pClic, pClic);
                this.vue.getCanvas().ajouterElement(this.mur1);
                this.vue.setInstructions(
                    "Début du mur posé — Cliquez pour définir la fin");
            } else {
                this.mur1.setPoint2(pClic);
                this.mur1 = null;
                this.vue.setInstructions(
                    "Mur créé — Cliquez pour un nouveau mur ou changez d'outil");
            }
        }
    }

    public void mouseMovedDansZoneDessin(MouseEvent t) {
        Point pSouris = this.posInModel(t.getX(), t.getY());

        if (this.etat == ETAT_MUR) {
            // ✅ Guard restriction appartement
            if (polygoneAppartement != null
                    && !estDansAppartement(pSouris.getX(), pSouris.getY())) {
                return;
            }
            boolean modRect = this.vue.getOptionsMurVue().estRectangulaire();
            if (modRect) {
                if (etapeRectangle == 1) {
                    this.mur1.setPoint2(pSouris);
                } else if (etapeRectangle == 2) {
                    Point p3Contraint = calculerPointOrthogonal(this.p2, pSouris);
                    this.mur2.setPoint2(p3Contraint);
                }
            } else {
                if (this.mur1 != null) {
                    this.mur1.setPoint2(pSouris);
                }
            }
            this.vue.redrawAll();

        } else if (this.etat == ETAT_PORTE || this.etat == ETAT_FENETRE) {
            this.murSurvolé = trouverMurProche(pSouris);
            Fantome fantome = (this.etat == ETAT_PORTE) ? new Porte() : new Fenetre();
            if (this.murSurvolé != null) {
                // ✅ Fenêtre : actif seulement sur mur extérieur
                // ✅ Porte : actif seulement sur mur non extérieur
                boolean compatible = (this.etat == ETAT_FENETRE)
                    ? this.murSurvolé.getTypeMur() == Mur.TypeMur.EXTERIEUR
                    : this.murSurvolé.getTypeMur() != Mur.TypeMur.EXTERIEUR;
                double angle = Math.toDegrees(Math.atan2(murSurvolé.getPoint2().getY() - murSurvolé.getPoint1().getY(),
                murSurvolé.getPoint2().getX() - murSurvolé.getPoint1().getX()));
                Point posAccrochage = projeterSurMur(pSouris, murSurvolé);
                this.vue.getCanvas().setFantome(fantome, posAccrochage.getX(),
                        posAccrochage.getY(), angle, compatible);
            } else {
                this.vue.getCanvas().setFantome(fantome, pSouris.getX(),
                        pSouris.getY(), 0, false);
            }
            this.vue.redrawAll();
        }
    }

    /**
     * Projette orthogonalement un point sur le segment du mur.
     * Retourne le point d'accrochage sur le mur.
     */
    private Point projeterSurMur(Point p, Mur mur) {
        double x1 = mur.getPoint1().getX(), y1 = mur.getPoint1().getY();
        double x2 = mur.getPoint2().getX(), y2 = mur.getPoint2().getY();

        double dx = x2 - x1, dy = y2 - y1;
        double l2 = dx * dx + dy * dy;
        if (l2 == 0) return mur.getPoint1();

        double t = ((p.getX() - x1) * dx + (p.getY() - y1) * dy) / l2;

        // ✅ Marge dynamique selon la largeur de l'ouverture
        double largeur = (this.etat == ETAT_PORTE)
                ? Porte.LARGEUR_PORTE
                : Fenetre.COTE_FENETRE;
        double marge = largeur / (2 * mur.calculerLongueur());
        t = Math.max(marge, Math.min(1.0 - marge, t));

        return new Point(x1 + t * dx, y1 + t * dy);
    }

    public Point calculerPointOrthogonal(Point centre, Point cible) {
        double dx       = p2.getX() - p1.getX();
        double dy       = p2.getY() - p1.getY();
        double perpX    = -dy;
        double perpY    = dx;
        double ux       = cible.getX() - centre.getX();
        double uy       = cible.getY() - centre.getY();
        double scalaire = (ux * perpX + uy * perpY) / (perpX * perpX + perpY * perpY);
        return new Point(
            centre.getX() + scalaire * perpX,
            centre.getY() + scalaire * perpY
        );
    }

    /** Logique pour trouver le mur le plus proche du clic (30 cm de tolérance). */
    private Mur trouverMurProche(Point p) {
        Mur plusProche = null;
        double distMin = 0.3;

        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur) {
                Mur m    = (Mur) d;
                double dist = m.distanceA(p);
                if (dist < distMin) {
                    distMin   = dist;
                    plusProche = m;
                }
            }
        }
        return plusProche;
    }

    /**
     * Dessine le contour de l'appartement en fond du canvas.
     * Lecture seule — non interactif, juste une référence visuelle.
     */
    public void initialiserAvecContourAppartement(List<Point> polygone,
                                               List<Mur> mursDelimiteurs,
                                               AireImmeuble aire) {
        this.polygoneAppartement = polygone;
        if (polygone == null || polygone.size() < 3) return;

        // Fond bleuté
        Dessin fond = new Dessin() {
            @Override
            public void dessiner(GraphicsContext gc) {
                double[] xs = polygone.stream().mapToDouble(Point::getX).toArray();
                double[] ys = polygone.stream().mapToDouble(Point::getY).toArray();
                gc.setFill(Color.web("#4a90d9", 0.05));
                gc.fillPolygon(xs, ys, polygone.size());
            }
            @Override public Color getColor() { return Color.TRANSPARENT; }
            @Override public void setColor(Color c) { }
        };
        this.vue.getCanvas().getElements().add(0, fond);

        // Récupérer les 4 côtés de l'aire
        List<double[]> cotesBatiment = new ArrayList<>();
        if (aire != null && aire.isComplete()) {
            Point p1 = aire.getP1(), p2 = aire.getP2();
            Point p3 = aire.getP3(), p4 = aire.getP4();
            cotesBatiment.add(new double[]{p1.getX(), p1.getY(), p2.getX(), p2.getY()});
            cotesBatiment.add(new double[]{p2.getX(), p2.getY(), p3.getX(), p3.getY()});
            cotesBatiment.add(new double[]{p3.getX(), p3.getY(), p4.getX(), p4.getY()});
            cotesBatiment.add(new double[]{p4.getX(), p4.getY(), p1.getX(), p1.getY()});
        }

        for (Mur mur : mursDelimiteurs) {
            // ✅ Extérieur si le mur est inclus dans un côté du bâtiment
            boolean exterieur = false;
            for (double[] cote : cotesBatiment) {
                if (murInclsDansCote(mur, cote)) {
                    exterieur = true;
                    break;
                }
            }
            mur.setTypeMur(exterieur ? Mur.TypeMur.EXTERIEUR : Mur.TypeMur.NORMAL);
            this.vue.getCanvas().getElements().add(mur);
        }

        this.vue.getCanvas().redrawAll();
        // ... listener centrage inchangé
    }

    // ✅ Vérifie si un mur est colinéaire et inclus dans un côté du bâtiment
    private boolean murInclsDansCote(Mur mur, double[] cote) {
        double TOL = 1e-6;
        double ax = cote[0], ay = cote[1], bx = cote[2], by = cote[3];
        double dx = bx - ax, dy = by - ay;
        double len2 = dx*dx + dy*dy;
        if (len2 < TOL) return false;

        // Vérifier que les deux extrémités du mur sont sur la droite du côté
        double[] pts = {
            mur.getPoint1().getX(), mur.getPoint1().getY(),
            mur.getPoint2().getX(), mur.getPoint2().getY()
        };

        for (int i = 0; i < 4; i += 2) {
            double px = pts[i], py = pts[i+1];
            // Distance à la droite portant le côté
            double cross = (px - ax) * dy - (py - ay) * dx;
            if (Math.abs(cross) / Math.sqrt(len2) > TOL) return false;
            // Vérifier que t ∈ [0,1] (point dans le segment)
            double t = ((px - ax)*dx + (py - ay)*dy) / len2;
            if (t < -TOL || t > 1 + TOL) return false;
        }
        return true;
    }
    
    private boolean estDansAppartement(double px, double py) {
        if (polygoneAppartement == null) return true;
        return pointDansPolygone(px, py) || pointSurContour(px, py);
    }
    
    private boolean pointDansPolygone(double px, double py) {
        int n = polygoneAppartement.size(), intersections = 0;
        for (int i = 0; i < n; i++) {
            Point a = polygoneAppartement.get(i);
            Point b = polygoneAppartement.get((i + 1) % n);
            if ((a.getY() <= py && b.getY() > py) || (b.getY() <= py && a.getY() > py)) {
                double xInter = a.getX() + (py - a.getY()) / (b.getY() - a.getY()) * (b.getX() - a.getX());
                if (px < xInter) intersections++;
            }
        }
        return (intersections % 2) == 1;
    }
    
    private boolean pointSurContour(double px, double py) {
        if (polygoneAppartement == null) return false;
        int n = polygoneAppartement.size();
        for (int i = 0; i < n; i++) {
            Point a = polygoneAppartement.get(i);
            Point b = polygoneAppartement.get((i + 1) % n);
            double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
            double len2 = dx*dx + dy*dy;
            if (len2 < 1e-10) continue;
            double t = ((px - a.getX())*dx + (py - a.getY())*dy) / len2;
            if (t < -1e-6 || t > 1+1e-6) continue;
            double cx = a.getX() + t*dx, cy = a.getY() + t*dy;
            if (Math.hypot(px-cx, py-cy) < 1e-6) return true;
        }
        return false;
    }

    public void annulerConstruction() {
        if (this.mur1 != null) this.vue.getCanvas().getElements().remove(this.mur1);
        if (this.mur2 != null) this.vue.getCanvas().getElements().remove(this.mur2);
        this.etapeRectangle = 0;
        this.mur1 = null;
        this.mur2 = null;
        this.vue.redrawAll();
    }

    /**
     * Rafraîchit le navigateur de modèle (murs + ouvertures).
     *
     * ✅ CORRIGÉ : dans la nouvelle architecture, PieceView ne contient plus
     * son propre TreeView (getItemMurs() a été supprimé). Le navigateur est
     * désormais le NavigateurView commun d'AppView.
     * Cette méthode est donc un no-op en attendant que AppControleur expose
     * un moyen de peupler la section "Murs" du NavigateurView, ou que ce
     * besoin soit traité dans une prochaine itération.
     *
     * Si tu veux réactiver le rafraîchissement, il faudra passer un
     * TreeItem<String> cible en paramètre depuis AppControleur :
     * <pre>
     *   public void rafraichirNavigateur(TreeItem<String> itemCible) {
     *       itemCible.getChildren().clear();
     *       for (Dessin d : vue.getCanvas().getElements()) { ... }
     *   }
     * </pre>
     */
    public void rafraichirNavigateur() {
        // ✅ No-op : getItemMurs() supprimé de PieceView (plus de TreeView embarqué).
        // Le rafraîchissement du navigateur est désormais de la responsabilité
        // d'AppControleur. Voir le Javadoc ci-dessus pour la migration complète.
    }

    // =========================================================================
    // BOUTONS TOOLBAR — conservés pour compatibilité, délégués par ContextePiece
    // =========================================================================

    public void btnNavigation(ActionEvent t) {
        this.changerEtat(ETAT_RIEN);
        this.vue.getCanvas().setPanActif(true);
        this.vue.getOptionsMurVue().setVisible(false);
    }

    public void btnEchelle(ActionEvent t) {
        // ✅ Dans la nouvelle architecture, l'échelle est gérée par AppControleur.
        // Cette méthode est conservée pour compatibilité si PieceView est utilisée
        // de façon autonome (tests, ancienne scène).
        boolean visible = !this.vue.getEchelleVue().isVisible();
        this.vue.getEchelleVue().setVisible(visible);

        if (visible) {
            // Écouter les changements d'échelle en temps réel
            this.vue.getEchelleVue().getGroupeEchelle()
                    .selectedToggleProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        double echelle = this.vue.getEchelleVue()
                                                 .getEchelleSelectionnee();
                        this.vue.getCanvas().setGridSize(echelle);
                    }
                }
            );
        }
    }
}