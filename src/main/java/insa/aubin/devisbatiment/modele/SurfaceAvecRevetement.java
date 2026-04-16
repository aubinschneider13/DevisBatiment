package insa.aubin.devisbatiment.modele;
import java.util.ArrayList;
import java.util.List;

public abstract class SurfaceAvecRevetement {
    
    private List<Revetement> revetement;
    private String idSurSurfaceAvecRevetement;

    public SurfaceAvecRevetement(String idSurSurfaceAvecRevetement){
        this.idSurSurfaceAvecRevetement = idSurSurfaceAvecRevetement;
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

    public String getIdSurSurfaceAvecRevetement() {
        return idSurSurfaceAvecRevetement;
    }

    public void setIdSurSurfaceAvecRevetement(String idSurSurfaceAvecRevetement) {
        this.idSurSurfaceAvecRevetement = idSurSurfaceAvecRevetement;
    }
    
    

}
