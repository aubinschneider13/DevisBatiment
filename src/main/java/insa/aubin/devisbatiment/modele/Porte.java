package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Porte extends Ouverture implements Fantome {
    public static final double LARGEUR_PORTE = 0.90;
    public static final double HAUTEUR_PORTE = 2.10;

    private int orientation = -1;

    public boolean isOuvertureInversee() {
        return orientation > 0;
    }

    public void setOuvertureInversee(boolean ouvertureInversee) {
        this.orientation = ouvertureInversee ? 1 : -1;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation >= 0 ? 1 : -1;
    }

    public Porte(double positionSurMur) {
        super("Porte", LARGEUR_PORTE, HAUTEUR_PORTE, positionSurMur);
    }

    public Porte(){
        this(0.5);
    }

    @Override
    public String toCSV() {
        return "PORTE;" + getId() + ";" + LARGEUR_PORTE + ";" + HAUTEUR_PORTE + ";" + (isOuvertureInversee() ? "1" : "0");
    }

    @Override
    public double getPrixForfaitaire() {
        return 150.0;
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

        double opacite = actif ? 0.85 : 0.4;
        gc.setGlobalAlpha(opacite);

        Color couleurPrincipale = actif
                ? Color.web("#27AE60")
                : Color.web("#888888");

        double l = getLargeur();

        // 1. Ouverture dans le mur (efface le trait du mur)
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.15);
        gc.strokeLine(-l / 2, 0, l / 2, 0);

        // 2. Jambages (les deux montants verticaux)
        gc.setStroke(couleurPrincipale);
        gc.setLineWidth(0.05);
        gc.strokeLine(-l / 2, -0.08, -l / 2, 0.08);
        gc.strokeLine( l / 2, -0.08,  l / 2, 0.08);

        // 3. Vantail & Arc de débattement (sensibles à l'inversion)
        gc.save();
        if (orientation > 0) {
            gc.scale(1, -1);
        }

        // Vantail : part du jambage gauche, descend perpendiculairement
        gc.setLineWidth(0.06);
        gc.strokeLine(-l / 2, 0, -l / 2, -l);

        // Arc de débattement : du bout du vantail jusqu'au jambage droit
        gc.setLineWidth(0.04);
        gc.setLineDashes(0.06, 0.04);
        gc.strokeArc(-l / 2 - l, -l, l * 2, l * 2, 0, 90, javafx.scene.shape.ArcType.OPEN);
        gc.setLineDashes(0);
        gc.restore();

        gc.setGlobalAlpha(1.0);
        gc.restore();
    }
}
