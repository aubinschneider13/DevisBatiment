package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Escalier extends Tremie {

    public static double PRIX_FORFAITAIRE_ESCALIER = 15000.0;

    public Escalier(Point centre) {
        super("Escalier", centre, Color.web("#8E44AD"));
    }

    @Override
    protected void dessinerSymbole(GraphicsContext gc, double x, double y, boolean fantome) {
        gc.save();
        gc.setStroke(getColor());
        gc.setLineWidth(fantome ? 0.05 : 0.04);

        double minX = x - COTE / 2.0 + 0.25;
        double minY = y - COTE / 2.0 + 0.25;
        double largeur = COTE - 0.5;
        double hauteurMarche = (COTE - 0.5) / 5.0;

        for (int i = 0; i <= 5; i++) {
            double yy = minY + i * hauteurMarche;
            gc.strokeLine(minX, yy, minX + largeur, yy);
        }
        gc.strokeLine(minX, minY, minX, minY + 5 * hauteurMarche);
        gc.strokeLine(minX + largeur, minY, minX + largeur, minY + 5 * hauteurMarche);
        gc.restore();
    }

    @Override
    protected String getTypeCSV() {
        return "ESCALIER";
    }

    @Override
    public double getPrixForfaitaire() {
        return PRIX_FORFAITAIRE_ESCALIER;
    }

    @Override
    public String toString() {
        return "Escalier [id=" + getId() + ", centre=(" + getX() + ", " + getY() + ")]";
    }
}
