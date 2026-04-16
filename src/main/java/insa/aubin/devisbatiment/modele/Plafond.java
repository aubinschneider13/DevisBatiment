package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Plafond extends SurfaceAvecRevetement {

    public Plafond(){
        super("Plafond" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()));
    }

    @Override
    public double calculerSurface(){
     return 0.0;
    }
}
