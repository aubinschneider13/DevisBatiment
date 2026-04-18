package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Immeuble extends Batiment{
    private String idAppartement;

    public Immeuble(String nomBat, String typeBat, double nbNiveau){
        super(nomBat, typeBat, nbNiveau);
        this.idAppartement = "Appartement" +  new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date());
    }

    @Override
    public void dessiner() {

    }
}
