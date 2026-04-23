package insa.aubin.devisbatiment.modele;

public class Sol extends SurfaceAvecRevetement {

    private float surface;

    public Sol(float surface) {
        super("Sol");
        this.surface = surface;
    }

    @Override
    public float calculerSurface() {
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