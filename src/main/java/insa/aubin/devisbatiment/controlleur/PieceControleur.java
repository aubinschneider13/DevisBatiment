package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.PieceView;
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
    public static final int ETAT_RIEN = 0;
    public static final int ETAT_MUR = 30;
    public static final int ETAT_PORTE = 40;
    public static final int ETAT_FENETRE = 50;

    private PieceView vue;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;
    private int etat = ETAT_RIEN;
    private Point p1, p2;
    private Mur mur1, mur2;
    private int etapeRectangle;
    private Mur murSurvolé;

    public PieceControleur(PieceView vue, Stage stage, GestionnaireSauvegarde gestionnaire) {
        this.vue = vue;
        this.stage = stage;
        this.gestionnaire = gestionnaire;
    }

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

    public void retourDashboard() {
        DashBoardView dashBoardView = new DashBoardView();
        Scene dashScene = new Scene(dashBoardView);
        stage.setScene(dashScene);
        stage.setTitle("InsaBuilder - Tableau de bord");
        new DashBoardControleur(dashBoardView, stage, gestionnaire);
        mettreFenetrePleinEcran();
    }

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
        this.changerEtat(30);
        this.vue.getOptionsMurVue().setVisible(true);
    }

    public void clicDansZoneDeDessin(MouseEvent event) {
        Point pClic = this.posInModel(event.getX(), event.getY());
        //CAS MUR
        if (this.etat == ETAT_MUR) {
            genererDessinMur(pClic);
        }

        else if (this.etat == ETAT_PORTE) {
            Mur cible = trouverMurProche(pClic);
            if (cible != null) {
                double t = cible.calculerPositionSurMur(pClic);

                // ✅ Marge appliquée ici, où on connaît la largeur
                double marge = Porte.LARGEUR_PORTE / (2 * cible.calculerLongueur());
                t = Math.max(marge, Math.min(1.0 - marge, t));

                Porte nouvellePorte = new Porte(t);
                cible.ajouterOuverture(nouvellePorte);
            }
        }
        else if (this.etat == ETAT_FENETRE) {
            Mur cible = trouverMurProche(pClic);
            if (cible != null) {
                double t = cible.calculerPositionSurMur(pClic);

                // ✅ Même logique pour la fenêtre
                double marge = Fenetre.COTE_FENETRE / (2 * cible.calculerLongueur());
                t = Math.max(marge, Math.min(1.0 - marge, t));

                Fenetre nouvelleFenetre = new Fenetre(t);
                cible.ajouterOuverture(nouvelleFenetre);
            }
        }
    }

    public void genererDessinMur(Point pClic){
        boolean modRect = this.vue.getOptionsMurVue().estRectangulaire();
        if (modRect) {
            switch (etapeRectangle) {
                case 0:
                    this.p1 = pClic;
                    this.mur1 = new Mur(p1, p1);
                    this.vue.getCanvas().ajouterElement(this.mur1);
                    this.vue.setInstructions("Premier coin posé — Cliquez pour définir la longueur");
                    this.etapeRectangle = 1;
                    break;
                case 1:
                    this.p2 = pClic;
                    this.mur1.setPoint2(p2);
                    this.mur2 = new Mur(p2, p2);
                    this.vue.getCanvas().ajouterElement(this.mur2);
                    this.vue.setInstructions("Longueur définie — Cliquez pour définir la largeur");
                    this.etapeRectangle = 2;
                    break;
                case 2:
                    Point p3 = calculerPointOrthogonal(this.p2, pClic);
                    this.mur2.setPoint2(p3);
                    Point p4 = new Point(p1.getX() + (p3.getX() - p2.getX()), p1.getY() + (p3.getY() - p2.getY()));
                    this.vue.getCanvas().ajouterElement(new Mur(p3, p4));
                    this.vue.getCanvas().ajouterElement(new Mur(p4, p1));
                    this.vue.setInstructions("Rectangle créé — Cliquez pour un nouveau mur ou changez d'outil");
                    this.etapeRectangle = 0;
                    this.mur1 = null;
                    this.mur2 = null;
                    break;
            }
        } else {
            //Mode libre
            if (this.mur1 == null) {
                this.mur1 = new Mur(pClic, pClic);
                this.vue.getCanvas().ajouterElement(this.mur1);
                this.vue.setInstructions("Début du mur posé — Cliquez pour définir la fin");
            } else {
                this.mur1.setPoint2(pClic);
                this.mur1 = null;
                this.vue.setInstructions("Mur créé — Cliquez pour un nouveau mur ou changez d'outil");
            }
        }
    }

    public void mouseMovedDansZoneDessin(MouseEvent t) {
        Point pSouris = this.posInModel(t.getX(), t.getY());
        if (this.etat == ETAT_MUR) {
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
            // Dans PieceControleur.java - remplacer le bloc ETAT_PORTE/FENETRE

        } else if (this.etat == ETAT_PORTE || this.etat == ETAT_FENETRE) {

            this.murSurvolé = trouverMurProche(pSouris);

            // Créer le bon fantôme selon l'outil actif
            Fantome fantome = (this.etat == ETAT_PORTE)
                    ? new Porte()
                    : new Fenetre();

            if (this.murSurvolé != null) {
                // ✅ Mur détecté : ancrer et aligner sur le mur
                double angle = Math.toDegrees(Math.atan2(
                        murSurvolé.getPoint2().getY() - murSurvolé.getPoint1().getY(),
                        murSurvolé.getPoint2().getX() - murSurvolé.getPoint1().getX()
                ));

                // Projeter le curseur sur le mur (position d'accrochage)
                Point posAccrochage = projeterSurMur(pSouris, murSurvolé);

                this.vue.getCanvas().setFantome(
                        fantome,
                        posAccrochage.getX(),
                        posAccrochage.getY(),
                        angle,
                        true  // actif = vert
                );
            } else {
                // ❌ Pas de mur : fantôme libre suivant le curseur
                this.vue.getCanvas().setFantome(
                        fantome,
                        pSouris.getX(),
                        pSouris.getY(),
                        0,
                        false  // inactif = gris
                );
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
        double l2 = dx*dx + dy*dy;
        if (l2 == 0) return mur.getPoint1();

        double t = ((p.getX()-x1)*dx + (p.getY()-y1)*dy) / l2;

        // ✅ Marge dynamique selon la largeur de l'ouverture
        double largeur = (this.etat == ETAT_PORTE)
                ? Porte.LARGEUR_PORTE
                : Fenetre.COTE_FENETRE;
        double marge = largeur / (2 * mur.calculerLongueur());
        t = Math.max(marge, Math.min(1.0 - marge, t));

        return new Point(x1 + t*dx, y1 + t*dy);
    }

    public Point calculerPointOrthogonal(Point centre, Point cible) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double perpX = -dy;
        double perpY = dx;
        double ux = cible.getX() - centre.getX();
        double uy = cible.getY() - centre.getY();
        double scalaire = (ux * perpX + uy * perpY) / (perpX * perpX + perpY * perpY);
        return new Point(centre.getX() + scalaire * perpX, centre.getY() + scalaire * perpY);
    }

    /** Logique pour trouver le mur le plus proche du clic (30cm de tolérance) */
    private Mur trouverMurProche(Point p) {
        Mur plusProche = null;
        double distMin = 0.3;

        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur) {
                Mur m = (Mur) d;
                double dist = m.distanceA(p);
                if (dist < distMin) {
                    distMin = dist;
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
    public void initialiserAvecContourAppartement(List<Point> polygone) {
        if (polygone == null || polygone.size() < 3) return;

        // Créer un Dessin anonyme pour le contour
        Dessin contour = new Dessin() {
            @Override
            public void dessiner(GraphicsContext gc) {
                double[] xs = polygone.stream()
                        .mapToDouble(Point::getX).toArray();
                double[] ys = polygone.stream()
                        .mapToDouble(Point::getY).toArray();

                // Fond légèrement bleuté
                gc.setFill(Color.web("#4a90d9", 0.05));
                gc.fillPolygon(xs, ys, polygone.size());

                // Contour en pointillés gris
                gc.setStroke(Color.web("#999999", 0.8));
                gc.setLineWidth(0.05);
                gc.setLineDashes(0.2, 0.15);
                gc.strokePolygon(xs, ys, polygone.size());
                gc.setLineDashes(); // reset
            }

            @Override public Color getColor() { return Color.GRAY; }
            @Override public void setColor(Color c) { /* lecture seule */ }
        };

        // Insérer en premier (en dessous de tout)
        this.vue.getCanvas().getElements().add(0, contour);
        this.vue.getCanvas().redrawAll();
    }

    public void annulerConstruction() {
        if (this.mur1 != null) this.vue.getCanvas().getElements().remove(this.mur1);
        if (this.mur2 != null) this.vue.getCanvas().getElements().remove(this.mur2);
        this.etapeRectangle = 0;
        this.mur1 = null;
        this.mur2 = null;
        this.vue.redrawAll();
    }

    public void rafraichirNavigateur() {
        this.vue.getItemMurs().getChildren().clear();
        for (Dessin d : this.vue.getCanvas().getElements()) {
            if (d instanceof Mur) {
                Mur mur = (Mur) d;
                TreeItem<String> itemMur = new TreeItem<>(mur.toString());
                for (Ouverture o : mur.getListeOuvertures()) {
                    itemMur.getChildren().add(new TreeItem<>(o.toString()));
                }
                this.vue.getItemMurs().getChildren().add(itemMur);
            }
        }
    }
    
    public void btnNavigation(ActionEvent t) {
        this.changerEtat(0); // etat 0 = aucun outil, pan actif
        this.vue.getCanvas().setPanActif(true);
        this.vue.getOptionsMurVue().setVisible(false);
    }
    
    public void btnEchelle(ActionEvent t) {
        // Bascule la visibilité du panneau échelle
        boolean visible = !this.vue.getEchelleVue().isVisible();
        this.vue.getEchelleVue().setVisible(visible);

        if (visible) {
            // Écouter les changements d'échelle en temps réel
            this.vue.getEchelleVue().getGroupeEchelle().selectedToggleProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        double echelle = this.vue.getEchelleVue().getEchelleSelectionnee();
                        this.vue.getCanvas().setGridSize(echelle);
                    }
                }
            );
        }
    }
}