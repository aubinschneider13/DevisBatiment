package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.Dessin;
import insa.aubin.devisbatiment.modele.Mur;
import insa.aubin.devisbatiment.modele.Point;
import insa.aubin.devisbatiment.view.PieceView;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

public class PieceVueControleur {
    private PieceView vue;
    private int etat;
    //private Point pointDebut = null;
    private Point p1, p2;
    //private Mur murEnCours;
    private Mur mur1, mur2;
    private int etapeRectangle;


    public PieceVueControleur(PieceView vue) {
        this.vue = vue;
    }

    public void changerEtat(int nouvelEtat){
        if (nouvelEtat == 30){

        }
        this.etat = nouvelEtat;
    }

    public Point posInModel(double x, double y) {
        Transform modelVersVue = this.vue.getCanvas().getTransform();
        Point2D pointTrans; //position du pt en pixel
        try {
            pointTrans = modelVersVue.inverseTransform(x, y); //convertit les pixels en données exploitables
        } catch (NonInvertibleTransformException ex) {
            throw new Error(ex);
        }
        Point pointClic = new Point(pointTrans.getX(), pointTrans.getY()); //on stocke les coordonnées exploitables

        return pointClic;
    }

    public void btnMur(ActionEvent t){
        this.changerEtat(30);
        this.vue.getOptionsMurVue().setVisible(true);
    }

    public void clicDansZoneDeDessin(MouseEvent t){
        if (this.etat == 30){
            Point pClic = this.posInModel(t.getX(), t.getY());
            boolean modRect = this.vue.getOptionsMurVue().estRectangulaire();

            if (modRect){
                switch (etapeRectangle) {
                    case 0: //premier clic
                        this.p1 = pClic;
                        this.mur1 = new Mur(p1, p1);
                        this.vue.getCanvas().ajouterElement(this.mur1);
                        this.etapeRectangle = 1;
                        break;
                    case 1: //deuxième clic : on crée le mur
                        this.p2 = pClic;
                        this.mur1.setPoint2(p2);
                        this.mur2 = new Mur(p2, p2);
                        this.vue.getCanvas().ajouterElement(this.mur2);
                        this.etapeRectangle = 2;
                        break;
                    case 2: //troisème clic : on crée la pièce
                        Point p3 = calculerPointOrthogonal(this.p2, pClic);
                        this.mur2.setPoint2(p3);
                        Point p4 = new Point(p1.getX() + (p3.getX() - p2.getX()),  p1.getY() + (p3.getY() - p2.getY()));
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

            /*if (this.murEnCours == null){
                //Premier clic
                this.murEnCours = new Mur(pClic, pClic);
                this.vue.getCanvas().ajouterElement(murEnCours);
                System.out.println("Debut du mur : " + pClic);
            }
            else {
                //Deuxième clic
                this.murEnCours.setPoint2(pClic);
                this.murEnCours = null; //on remet le premier point à null pour recommencer à dessiner un autre mur
                this.vue.redrawAll();
                System.out.println("Mur termine");
            }*/

        }
    }

    public void mouseMovedDansZoneDessin(MouseEvent t){
        if (this.etat == 30){
            Point pSouris = this.posInModel(t.getX(), t.getY());
            boolean modRect = this.vue.getOptionsMurVue().estRectangulaire();
            if (modRect){
                if (etapeRectangle == 1){
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

    public Point calculerPointOrthogonal(Point centre, Point cible){
        //Vecteur du mur
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();

        //Vecteur perpendiculaire
        double perpX = -dy;
        double perpY = dx;

        //On projette le clic de la souris sur cet axe perpendiculaire passant par p2
        double ux = cible.getX() - centre.getX();
        double uy = cible.getY() - centre.getY();

        //Produit scalaire pour trouver la longueur de la projection
        double scalaire = (ux * perpX + uy * perpY) / (perpX * perpX + perpY * perpY);

        return new Point(centre.getX() + scalaire * perpX, centre.getY() + scalaire * perpY);
    }

    public void annulerConstruction(){
        if (this.mur1 != null) {
            this.vue.getCanvas().getElements().remove(this.mur1);
        }
        if (this.mur2 != null) {
            this.vue.getCanvas().getElements().remove(this.mur2);
        }

        this.etapeRectangle = 0;
        this.mur1 = null;
        this.mur2 = null;
        System.out.println("Construction annulée par l'utilisateur.");
    }

    public void rafraichirNavigateur(){
        System.out.println("Nb éléments dans le canvas : " + this.vue.getCanvas().getElements().size());
        this.vue.getItemMurs().getChildren().clear();
        for (Dessin d : this.vue.getCanvas().getElements()) {
            if (d instanceof Mur) {
                System.out.println("Mur trouvé, ajout au TreeView...");
                Mur mur = (Mur) d;
                TreeItem<String> item = new TreeItem<>(mur.toString());
                this.vue.getItemMurs().getChildren().add(item);
            }
        }
    }
}
