package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Locale;

public abstract class Tremie extends ElementDeConstruction implements Dessin, Fantome {

    public static final double COTE = 2.0;

    private final Point centre;
    private Color color;

    protected Tremie(String prefixe, Point centre, Color color) {
        super(prefixe);
        this.centre = centre;
        this.color = color;
    }



    public double getX() {
        return centre.getX();
    }

    public double getY() {
        return centre.getY();
    }

    public double getMinX() {
        return getX() - COTE / 2.0;
    }

    public double getMinY() {
        return getY() - COTE / 2.0;
    }

    public double getMaxX() {
        return getX() + COTE / 2.0;
    }

    public double getMaxY() {
        return getY() + COTE / 2.0;
    }

    public void recopierIdDepuis(Tremie source) {
        if (source != null) {
            setId(source.getId());
        }
    }

    @Override
    public void dessiner(GraphicsContext gc) {
        dessinerCarre(gc, getX(), getY(), 1.0, true);
        dessinerSymbole(gc, getX(), getY(), false);
    }

    @Override
    public void dessinerFantome(GraphicsContext gc, double x, double y,
                                double angle, boolean actif) {
        gc.save();
        gc.setGlobalAlpha(actif ? 0.75 : 0.35);
        dessinerCarre(gc, x, y, actif ? 0.75 : 0.35, actif);
        dessinerSymbole(gc, x, y, true);
        gc.restore();
    }

    protected void dessinerCarre(GraphicsContext gc, double x, double y,
                                 double opacite, boolean contourFort) {
        double minX = x - COTE / 2.0;
        double minY = y - COTE / 2.0;

        gc.save();
        gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.16 * opacite));
        gc.fillRect(minX, minY, COTE, COTE);
        gc.setStroke(color);
        gc.setLineWidth(contourFort ? 0.06 : 0.04);
        gc.setLineDashes(0.18, 0.10);
        gc.strokeRect(minX, minY, COTE, COTE);
        gc.setLineDashes(0);
        gc.restore();
    }

    protected abstract void dessinerSymbole(GraphicsContext gc, double x, double y, boolean fantome);

    protected abstract String getTypeCSV();

    public abstract double getPrixForfaitaire();

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public String toCSV() {
        return String.format(Locale.US, "TREMIE;%s;%s;%.2f;%.2f;%.2f",
                getId(), getTypeCSV(), getX(), getY(), COTE);
    }
}
