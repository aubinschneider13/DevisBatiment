package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Fenetre extends Ouverture implements Fantome {
    public static final double COTE_FENETRE = 1.20;

    public Fenetre(double positionSurMur) {
        super("Fenetre", COTE_FENETRE, COTE_FENETRE, positionSurMur);
    }

    public Fenetre() {
        this(0.5);
    }

    @Override
    public String toCSV() {
        return "FENETRE;" + getId() + ";" + COTE_FENETRE + ";" + COTE_FENETRE;
    }

    @Override
    public String toString() {
        return "Fenetre [id=" + getId() + ", largeur=" + COTE_FENETRE + ", hauteur=" + COTE_FENETRE + "]";
    }

    @Override
    public void dessinerFantome(GraphicsContext gc,
                                double x, double y,
                                double angle, boolean actif) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(angle);

        double opacite = actif ? 0.85 : 0.4;
        gc.setGlobalAlpha(opacite);

        Color couleur = actif
                ? Color.web("#2980B9")   // bleu = accrochage actif
                : Color.web("#888888");  // gris = survol libre

        double l = getLargeur();
        double ep = 0.06;

        // 1. Ouverture
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.15);
        gc.strokeLine(-l/2, 0, l/2, 0);

        // 2. Encadrements
        gc.setStroke(couleur);
        gc.setLineWidth(0.05);
        gc.strokeLine(-l/2, -0.08, -l/2, 0.08);
        gc.strokeLine( l/2, -0.08,  l/2, 0.08);

        // 3. Double vitrage
        gc.setStroke(couleur);
        gc.setLineWidth(0.04);
        gc.strokeLine(-l/2, -ep, l/2, -ep);
        gc.strokeLine(-l/2,  ep, l/2,  ep);

        gc.setGlobalAlpha(1.0);
        gc.restore();
    }
}
