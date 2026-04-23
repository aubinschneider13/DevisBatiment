package insa.aubin.devisbatiment.modele;

public class Revetement extends ElementDeConstruction {

    private String typeRevetement;
    private float prixUnitaire;

    public Revetement(String typeRevetement, float prixUnitaire) {
        super("Revetement");
        this.typeRevetement = typeRevetement;
        this.prixUnitaire = prixUnitaire;
    }

    public float calculerPrixTotal(float surface) {
        return this.prixUnitaire * surface;
    }

    @Override
    public String toCSV() {
        return "REVETEMENT;" + getId() + ";" + typeRevetement + ";" + prixUnitaire;
    }

    @Override
    public String toString() {
        return "Revetement [id=" + getId() + ", type=" + typeRevetement + ", prixUnitaire=" + prixUnitaire + "]";
    }

    public String getTypeRevetement() {
        return typeRevetement;
    }
    
    public void setTypeRevetement(String typeRevetement) {
        this.typeRevetement = typeRevetement;
    }
    
    public float getPrixUnitaire() {
        return prixUnitaire;
    }
    
    public void setPrixUnitaire(float prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }
}