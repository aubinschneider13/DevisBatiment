package insa.aubin.devisbatiment.Model;
/**
 *
 * 
 */
public class Plafond extends SurfaceAvecRevetement {
    private String idPlafond;
    //private ArrayList<Revetement> revetement = new ArrayList<>();

    public Plafond(String idPlafond){
        super();
        this.idPlafond = idPlafond;
    }

    public String getIdPlafond(){
        return idPlafond;
    }

    public void setIdPlafond(String idPlafond){
        this.idPlafond = idPlafond;
    }

    @Override
    public double calculerSurface(){
     return 0.0;
    }
}
