package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Porte extends Ouverture {
    private static final double LARGEUR_PORTE = 0.90f;
    private static final double HAUTEUR_PORTE = 2.10f;

     public Porte() {
        super("Porte" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()), LARGEUR_PORTE, HAUTEUR_PORTE);
    }
    
    @Override
    public String toCSV() {
        return "PORTE;" + LARGEUR_PORTE + ";" + HAUTEUR_PORTE;
    }
}
