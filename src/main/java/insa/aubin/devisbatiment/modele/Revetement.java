package insa.aubin.devisbatiment.modele;

public class Revetement extends ElementDeConstruction {
    private String typeRevetement;
    private double prixUnitaire;

    public Revetement(String typeRevetement, double prixUnitaire){
        super("Revetement");
        this.typeRevetement = typeRevetement;
        this.prixUnitaire = prixUnitaire;
    }

    public double calculerPrixTotal(double surface) {
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

    public double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }
}