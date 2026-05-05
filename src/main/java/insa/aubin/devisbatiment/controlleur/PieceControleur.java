package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.Dessin;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.modele.Mur;
import insa.aubin.devisbatiment.modele.Point;
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
    private PieceView vue;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;
    private int etat;
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
        this.vue.getCanvas().setPanActif(nouvelEtat == 0);
    }

    /**
     * Convertit les coordonnées de la souris en coordonnées du modèle
     * en prenant en compte le zoom et le magnétisme de la grille.
     */
    public Point posInModel(double x, double y) {
        // Utilise snapToGrid qui gère à la fois la conversion pixels→modèle
        // et le magnétisme sur la grille, avec Y inversé
        Point2D p = this.vue.getCanvas().snapToGrid(x, y);
        return new Point(p.getX(), p.getY());
    }

    public void btnMur(ActionEvent t) {
        this.changerEtat(30);
        this.vue.getOptionsMurVue().setVisible(true);
    }

    public void clicDansZoneDeDessin(MouseEvent t) {
        if (this.etat == 30) {
            // Utilise la méthode corrigée avec Snap et Zoom
            Point pClic = this.posInModel(t.getX(), t.getY());
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
                if (this.mur1 == null) {
                    this.mur1 = new Mur(pClic, pClic);
                    this.vue.getCanvas().ajouterElement(this.mur1);
                } else {
                    this.mur1.setPoint2(pClic);
                    this.mur1 = null;
                }
            }
            this.rafraichirNavigateur();
            this.vue.redrawAll();
        }
    }

    public void mouseMovedDansZoneDessin(MouseEvent t) {
        if (this.etat == 30) {
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
                TreeItem<String> item = new TreeItem<>(mur.toString());
                this.vue.getItemMurs().getChildren().add(item);
            }
        }
    }
    
    public void btnNavigation(ActionEvent t) {
        this.changerEtat(0); // etat 0 = aucun outil, pan actif
        this.vue.getCanvas().setPanActif(true);
        this.vue.getOptionsMurVue().setVisible(false);
    }
}