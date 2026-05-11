package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.PieceView;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.application.Platform;

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

    public void clicDansZoneDeDessin(MouseEvent t) {
        Point pClic = this.posInModel(t.getX(), t.getY());
        //CAS MUR
        if (this.etat == ETAT_MUR) {
            genererDessinMur(pClic);
        }
        //CAS PORTE
        else if (this.etat == ETAT_PORTE) {
            Mur cible = trouverMurProche(pClic);
            if (cible != null) {
                    Porte nouvellePorte = new Porte();
                    cible.ajouterOuverture(nouvellePorte);
            }
        }
        //CAS FENETRE
        else if (this.etat == ETAT_FENETRE) {
            Mur cible = trouverMurProche(pClic);
            if (cible != null) {
                Fenetre nouvelleFenetre = new Fenetre();
                cible.ajouterOuverture(nouvelleFenetre);
            }
        }
        this.rafraichirNavigateur();
        this.vue.redrawAll();
    }

    public void genererDessinMur(Point pClic){
        boolean modRect = this.vue.getOptionsMurVue().estRectangulaire();
        if (modRect) {
            switch (etapeRectangle) {
                case 0:
                    this.p1 = pClic;
                    this.mur1 = new Mur(p1, p1);
                    this.vue.getCanvas().ajouterElement(this.mur1);
                    this.etapeRectangle = 1;
                    break;
                case 1:
                    this.p2 = pClic;
                    this.mur1.setPoint2(p2);
                    this.mur2 = new Mur(p2, p2);
                    this.vue.getCanvas().ajouterElement(this.mur2);
                    this.etapeRectangle = 2;
                    break;
                case 2:
                    Point p3 = calculerPointOrthogonal(this.p2, pClic);
                    this.mur2.setPoint2(p3);
                    Point p4 = new Point(p1.getX() + (p3.getX() - p2.getX()), p1.getY() + (p3.getY() - p2.getY()));
                    this.vue.getCanvas().ajouterElement(new Mur(p3, p4));
                    this.vue.getCanvas().ajouterElement(new Mur(p4, p1));
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
            } else {
                this.mur1.setPoint2(pClic);
                this.mur1 = null;
            }
        }
    }

    public void mouseMovedDansZoneDessin(MouseEvent t) {
        if (this.etat == ETAT_MUR) {
            Point pSouris = this.posInModel(t.getX(), t.getY());
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
        }
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