package insa.aubin.devisbatiment;
import java.util.ArrayList;
import java.util.List;

public abstract class SurfaceAvecRevetement {
    private List<Revetement> revetement;

    public SurfaceAvecRevetement(){
        this.revetement = new ArrayList<>();
    }

    public abstract float calculerSurface();

}
