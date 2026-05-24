package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Ascenseur extends Tremie {

    public static double PRIX_FORFAITAIRE_ASCENSEUR = 40000.0;

    public Ascenseur(Point centre) {
        super("Ascenseur", centre, Color.web("#16A085"));
    }

    @Override
    protected void dessinerSymbole(GraphicsContext gc, double x, double y, boolean fantome) {
        gc.save();
        gc.setStroke(getColor());
        gc.setLineWidth(fantome ? 0.06 : 0.05);

        double minX = x - COTE / 2.0 + 0.35;
        double minY = y - COTE / 2.0 + 0.25;
        double w = COTE - 0.70;
        double h = COTE - 0.50;

        gc.strokeRect(minX, minY, w, h);
        gc.strokeLine(x, minY, x, minY + h);

        gc.setFill(getColor());
        double triY = y - 0.35;
        gc.fillPolygon(new double[]{x - 0.22, x + 0.22, x},
                new double[]{triY, triY, triY + 0.28}, 3);
        triY = y + 0.35;
        gc.fillPolygon(new double[]{x - 0.22, x + 0.22, x},
                new double[]{triY, triY, triY - 0.28}, 3);
        gc.restore();
    }

    @Override
    protected String getTypeCSV() {
        return "ASCENSEUR";
    }

    @Override
    public double getPrixForfaitaire() {
        return PRIX_FORFAITAIRE_ASCENSEUR;
    }

    @Override
    public String toString() {
        return "Ascenseur [id=" + getId() + ", centre=(" + getX() + ", " + getY() + ")]";
    }
}
