package insa.aubin.devisbatiment.modele;

public class Isolant extends Revetement {
    private TypeIsolant typeIsolant;
    private double epaisseur; // en cm

    public Isolant(String id, String designation, boolean pourMur, boolean pourSol, boolean pourPlafond, double prixUnitaire, TypeIsolant typeIsolant, double epaisseur) {
        super(id, designation, pourMur, pourSol, pourPlafond, prixUnitaire);
        this.typeIsolant = typeIsolant;
        this.epaisseur = epaisseur;
    }

    public TypeIsolant getTypeIsolant() {
        return typeIsolant;
    }

    public double getEpaisseur() {
        return epaisseur;
    }

    public void setTypeIsolant(TypeIsolant typeIsolant) {
        this.typeIsolant = typeIsolant;
    }

    public void setEpaisseur(double epaisseur) {
        this.epaisseur = epaisseur;
    }

    @Override
    public String toCSV() {
        // Pour être compatible avec le CSV à 12 colonnes, on stocke typeIsolant dans la colonne 'couleur'
        // et l'épaisseur dans la colonne 'finition', en laissant le reste vide.
        return super.toCSV() + ";" + typeIsolant.name() + ";" + epaisseur + ";;;";
    }

    @Override
    public String toString() {
        return "Isolant [id=" + getId() + ", type=" + getDesignation() + ", typeIsolant=" + typeIsolant.getLibelle() + ", epaisseur=" + epaisseur + "cm, prixUnitaire=" + getPrixUnitaire() + "]";
    }
}
