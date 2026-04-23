package insa.aubin.devisbatiment.modele;

public class Plafond extends SurfaceAvecRevetement {

    private float surface;

    public Plafond(float surface) {
        super("Plafond");
        this.surface = surface;
    }

    @Override
    public float calculerSurface() {
        return surface;
    }

    @Override
    public String toCSV() {
        return "PLAFOND;" + super.toCSV() + ";" + surface;
    }

    @Override
    public String toString() {
        return "Plafond [id=" + getId() + ", surface=" + surface + "]";
    }
}