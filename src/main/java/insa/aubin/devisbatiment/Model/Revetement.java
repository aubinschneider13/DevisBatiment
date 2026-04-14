package insa.aubin.devisbatiment.Model;

/**
 *
 * @author Aubin
 */
public class Revetement {
    private String idRevetement;
    private String typeRevetement;
    private double prixUnitaire;

    public Revetement(String idRevetement, String typeRevetement, double prixUnitaire){
        this.idRevetement = idRevetement;
        this.typeRevetement = typeRevetement;
        this.prixUnitaire = prixUnitaire;
    }

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
