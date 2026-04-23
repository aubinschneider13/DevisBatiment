package insa.aubin.devisbatiment.modele;

public class Sol extends SurfaceAvecRevetement {

    private double surface;

    public Sol(double surface) {
        super("Sol");
        this.surface = surface;
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