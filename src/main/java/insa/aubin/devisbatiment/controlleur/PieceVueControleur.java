package insa.aubin.devisbatiment.controlleur;

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
            Point pointClic = this.posInModel(t.getX(), t.getY());
            System.out.println("Point créé à : " + pointClic.getX() + ", " + pointClic.getY());
            this.vue.getCanvas().ajouterElement(pointClic);
            //this.vue.redrawAll();
        }
    }
}
