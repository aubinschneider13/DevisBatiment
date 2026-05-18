package insa.aubin.devisbatiment.modele;

import java.util.List;

public class Sol extends SurfaceAvecRevetement {

    private double surface;
    private List<Point> polygonePiece = null;

    public Sol(double surface) {
        super("Sol");
        this.surface = surface;
    }

    public void setSurface(double surface) {
        this.surface = surface;
    }

    public void setPolygonePiece(List<Point> polygone) {
        this.polygonePiece = polygone;
    }

    public List<Point> getPolygonePiece() {
        return polygonePiece;
    }

    @Override
    public double calculerSurface() {
        return surface;
    }

    @Override
    public String toCSV() {
        return "SOL;" + super.toCSV() + ";" + surface;
    }

    @Override
    public String toString() {
        return "Sol [id=" + getId() + ", surface=" + surface + "]";
    }
}