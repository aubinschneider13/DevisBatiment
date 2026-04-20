package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Maison extends Batiment {
    private String idMaison;
    public Maison(String nomBat, String typeBat, double nbNiveau) {
        super(nomBat, typeBat, nbNiveau);
        this.idMaison = "Maison" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date());
    }
}
