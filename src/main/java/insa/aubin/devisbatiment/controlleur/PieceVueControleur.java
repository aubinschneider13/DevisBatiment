package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.Mur;
import insa.aubin.devisbatiment.modele.Point;
import insa.aubin.devisbatiment.view.PieceView;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

public class PieceVueControleur {
    private PieceView vue;
    private int etat;
    private Point pointDebut = null;
    private Mur murEnCours;

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
    }

    public void clicDansZoneDeDessin(MouseEvent t){
        System.out.println("Clic détecté !");
        if (this.etat == 30){
            System.out.println("Mode MUR actif");
            Point p = this.posInModel(t.getX(), t.getY());

            if (this.murEnCours == null){
                //Premier clic
                this.murEnCours = new Mur(p, p);
                this.vue.getCanvas().ajouterElement(murEnCours);
                System.out.println("Debut du mur : " + p);
            }
            else {
                //Deuxième clic
                this.murEnCours.setPoint2(p);
                this.murEnCours = null; //on remet le premier point à null pour recommencer à dessiner un autre mur
                this.vue.redrawAll();
                System.out.println("Mur termine");
            }

        }
    }

    public void mouseMovedDansZoneDessin(MouseEvent t){
        if (this.etat == 30){
            this.murEnCours.setPoint2(this.posInModel(t.getX(), t.getY()));
            this.vue.redrawAll();
        }
    }
}
