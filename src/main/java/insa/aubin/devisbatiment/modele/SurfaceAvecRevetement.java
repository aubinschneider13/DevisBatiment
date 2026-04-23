package insa.aubin.devisbatiment.modele;
import java.util.ArrayList;
import java.util.List;

public abstract class SurfaceAvecRevetement extends ElementDeConstruction{
    
    private List<Revetement> revetement;
    //private String idSurSurfaceAvecRevetement;

    public SurfaceAvecRevetement(String prefixe){
        super(prefixe);
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

    //Méthode calculerPrixRevetement() à implémenter !
    
    

}
