package insa.aubin.devisbatiment.modele;
import java.util.ArrayList;
import java.util.List;

public abstract class SurfaceAvecRevetement extends ElementDeConstruction {

    private List<Revetement> revetements;

    public SurfaceAvecRevetement(String prefixe) {
        super(prefixe);
        this.revetements = new ArrayList<>();
    }

    public List<Revetement> getRevetements() {
        return revetements;
    }

    public abstract double calculerSurface();

    public abstract boolean estCompatibleAvec(Revetement r);

    public void ajouterRevetement(Revetement r) {
        if (r != null && estCompatibleAvec(r)) {
            this.revetements.add(r);
        }
    }

    public double calculerPrixRevetement() {
        double total = 0;
        for (Revetement r : revetements) {
            total += r.calculerPrixTotal(calculerSurface());
        }
        return total;
    }

    @Override
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(getId());
        for (Revetement r : revetements) {
            sb.append(";").append(r.getId());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "SurfaceAvecRevetement [id=" + getId() + ", revetements=" + revetements + "]";
    }
}