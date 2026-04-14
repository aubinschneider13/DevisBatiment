package insa.aubin.devisbatiment.Model;
import java.util.ArrayList;
import java.util.List;

public abstract class SurfaceAvecRevetement {
    private List<Revetement> revetement;

    public SurfaceAvecRevetement(){
        this.revetement = new ArrayList<>();
    }

    public List<Revetement> getRevetement(){
        return revetement;
    }

    public abstract double calculerSurface();

    public void ajouterRevetement(Revetement r){
        if(r != null){
            this.revetement.add(r);
        }
    }

}
