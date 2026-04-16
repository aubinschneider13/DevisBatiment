package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Revetement {
    private String idRevetement;
    private String typeRevetement;
    private double prixUnitaire;

    public Revetement(String typeRevetement, double prixUnitaire){
        this.idRevetement = "Revetement" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date());
        this.typeRevetement = typeRevetement;
        this.prixUnitaire = prixUnitaire;
    }
    
    public String toCSV() {
        return "REVETEMENT;" + idRevetement + ";" + typeRevetement + ";" + prixUnitaire;
    }
    
    // Getters et Setters
    
    public String getIdRevetement() {
        return idRevetement;
    }

    public void setIdRevetement(String idRevetement) {
        this.idRevetement = idRevetement;
    }

    public String getTypeRevetement() {
        return typeRevetement;
    }

    public void setTypeRevetement(String typeRevetement) {
        this.typeRevetement = typeRevetement;
    }

    public double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }



}
