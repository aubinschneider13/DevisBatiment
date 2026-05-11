package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Porte extends Ouverture implements Fantome {
    public static final double LARGEUR_PORTE = 0.90;
    public static final double HAUTEUR_PORTE = 2.10;

    public Porte(double positionSurMur) {
        super("Porte", LARGEUR_PORTE, HAUTEUR_PORTE, positionSurMur);
    }

    public Porte(){
        this(0.5);
    }

    @Override
    public String toCSV() {
        return "PORTE;" + getId() + ";" + LARGEUR_PORTE + ";" + HAUTEUR_PORTE;
    }

    @Override
    public String toString() {
        return "Porte [id=" + getId() + ", largeur=" + LARGEUR_PORTE + ", hauteur=" + HAUTEUR_PORTE + "]";
    }

    @Override
    public void dessinerFantome(GraphicsContext gc,
                                double x, double y,
                                double angle, boolean actif) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(angle);

        // Transparence selon l'état
        double opacite = actif ? 0.85 : 0.4;
        gc.setGlobalAlpha(opacite);

        // Couleur selon l'état
        Color couleurPrincipale = actif
                ? Color.web("#27AE60")   // vert = accrochage actif
                : Color.web("#888888");  // gris = survol libre

        double l = getLargeur();

        // 1. Ouverture dans le mur
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.15);
        gc.strokeLine(-l/2, 0, l/2, 0);

        // 2. Encadrements
        gc.setStroke(couleurPrincipale);
        gc.setLineWidth(0.05);
        gc.strokeLine(-l/2, -0.08, -l/2, 0.08);
        gc.strokeLine( l/2, -0.08,  l/2, 0.08);

        // 3. Arc de débattement en pointillés
        gc.setStroke(couleurPrincipale);
        gc.setLineWidth(0.04);
        gc.setLineDashes(0.05, 0.05);
        gc.strokeArc(-l/2, -l, l, l, 270, 90, javafx.scene.shape.ArcType.OPEN);
        gc.setLineDashes(0);

        // 4. Vantail
        gc.setStroke(couleurPrincipale);
        gc.setLineWidth(0.05);
        gc.strokeLine(-l/2, 0, -l/2, -l);

        gc.setGlobalAlpha(1.0); // reset opacité
        gc.restore();
    }
}