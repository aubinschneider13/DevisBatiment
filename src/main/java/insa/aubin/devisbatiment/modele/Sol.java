package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Sol extends SurfaceAvecRevetement {
    
     public Sol(){
        super("Sol" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()));
    }

    @Override
    public double calculerSurface(){
     return 0.0;
    }
}