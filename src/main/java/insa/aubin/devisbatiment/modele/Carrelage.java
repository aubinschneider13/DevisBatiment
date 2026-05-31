package insa.aubin.devisbatiment.modele;

public class Carrelage extends Revetement {
    private String dimension;
    private String matiere;

    public Carrelage(String id, String designation, boolean pourMur, boolean pourSol, boolean pourPlafond, double prixUnitaire, String dimension, String matiere) {
        super(id, designation, pourMur, pourSol, pourPlafond, prixUnitaire);
        this.dimension = dimension;
        this.matiere = matiere;
    }

    public String getDimension() { return dimension; }
    public String getMatiere() { return matiere; }

    @Override
    public String toCSV() {
        // Le carrelage laisse couleur et finition vides, remplit dimension, laisse bois vide, et remplit matiere
        return super.toCSV() + ";;;" + dimension + ";;" + matiere;
    }
}