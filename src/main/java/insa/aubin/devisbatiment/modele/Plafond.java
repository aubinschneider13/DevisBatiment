package insa.aubin.devisbatiment.modele;

public class Plafond extends SurfaceAvecRevetement {
    private double surface;

    public Plafond(double surface){
        super("Plafond");
        this.surface = surface;
    }

    @Override
    public double calculerSurface(){
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
