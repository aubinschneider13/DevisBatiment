package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Niveau {
    private String idNiveau;
    private double nbAppartements;
    private double hauteurPlafond;
    ArrayList<Appartement> appartements = new ArrayList<>();
     
    public Niveau(double nbApparements, float hauteurPlafond){
        
        this.idNiveau = "Niveau" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date());
        this.nbAppartements = nbAppartements;
        this.hauteurPlafond = hauteurPlafond;
        this.appartements = new ArrayList<>();
    }
    
    //Getters and Setters

    public String getIdNiveau() {
        return idNiveau;
    }

    public double getNbAppartements() {
        return nbAppartements;
    }

    public void setNbAppartements(double nbAppartements) {
        this.nbAppartements = nbAppartements;
    }

    public double getHauteurPlafond() {
        return hauteurPlafond;
    }

    public void setHauteurPlafond(double hauteurPlafond) {
        this.hauteurPlafond = hauteurPlafond;
    }
}
