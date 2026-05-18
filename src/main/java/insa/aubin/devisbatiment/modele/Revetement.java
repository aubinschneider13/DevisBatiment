package insa.aubin.devisbatiment.modele;

public class Revetement extends ElementDeConstruction {
    private String designation;
    private boolean pourMur;
    private boolean pourSol;
    private boolean pourPlafond;
    private double prixUnitaire;

    public Revetement(String idFixe, String designation, boolean pourMur, boolean pourSol, boolean pourPlafond, double prixUnitaire){
        super("Revetement");
        super.setId(idFixe);
        this.designation = designation;
        this.pourMur = pourMur;
        this.pourSol = pourSol;
        this.pourPlafond = pourPlafond;
        this.prixUnitaire = prixUnitaire;
    }

    public double calculerPrixTotal(double surface) {
        return this.prixUnitaire * surface;
    }

    @Override
    public String toCSV() {
        int pMur = pourMur ? 1 : 0;
        int pSol = pourSol ? 1 : 0;
        int pPlaf = pourPlafond ? 1 : 0;
        return "REVETEMENT;" + getId() + ";" + designation + ";" + pMur + ";" + pSol + ";" + pPlaf + ";" + prixUnitaire;
    }

    @Override
    public String toString() {
        return "Revetement [id=" + getId() + ", type=" + designation + ", prixUnitaire=" + prixUnitaire + "]";
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public boolean isPourMur() {
        return pourMur;
    }

    public boolean isPourSol() {
        return pourSol;
    }

    public boolean isPourPlafond() {
        return pourPlafond;
    }

    public double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }
}