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
        int pMur = isPourMur() ? 1 : 0;
        int pSol = isPourSol() ? 1 : 0;
        int pPlaf = isPourPlafond() ? 1 : 0;

        // On génère la base : Classe;id;designation;pourMur;pourSol;pourPlafond;prix
        // Note : par défaut, la classe est "Revetement"
        String base = this.getClass().getSimpleName() + ";"
                + getId() + ";"
                + getDesignation() + ";"
                + pMur + ";" + pSol + ";" + pPlaf + ";"
                + getPrixUnitaire();

        // Si c'est juste un Revetement générique, on ajoute 5 points-virgules pour les colonnes vides
        if (this.getClass() == Revetement.class) {
            return base + ";;;;;";
        }
        return base;
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