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

    public void changerEtat(int nouvelEtat) {
        this.etat = nouvelEtat;
    }

    public Point posInModel(float x, float y) {
        Transform modelVersVue = this.vue.getCanvas().getLocalToSceneTransform();
        Point2D pointTrans;
        try {
            pointTrans = modelVersVue.inverseTransform(x, y);
        } catch (NonInvertibleTransformException ex) {
            throw new Error(ex);
        }
        return new Point((float) pointTrans.getX(), (float) pointTrans.getY());
    }

    public void btnMur(ActionEvent t) {
        this.changerEtat(30);
    }

    public void clicDansZoneDeDessin(MouseEvent t) {
        System.out.println("Clic détecté !");
        if (this.etat == 30) {
            System.out.println("Mode MUR actif");
            Point pointClic = this.posInModel((float) t.getX(), (float) t.getY());
            System.out.println("Point créé à : " + pointClic.getX() + ", " + pointClic.getY());
            this.vue.getCanvas().ajouterElement(pointClic);
        }
    }
}