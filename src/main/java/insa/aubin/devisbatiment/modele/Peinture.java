// Peinture.java
package insa.aubin.devisbatiment.modele;

public class Peinture extends Revetement {
    private String couleur;
    private String finition;

    public Peinture(String id, String designation, boolean pourMur, boolean pourSol, boolean pourPlafond, double prixUnitaire, String couleur, String finition) {
        super(id, designation, pourMur, pourSol, pourPlafond, prixUnitaire);
        this.couleur = couleur;
        this.finition = finition;
    }

    public String getCouleur() {
        return couleur;
    }

    public String getFinition() {
        return finition;
    }

    @Override
    public String toCSV() {
        // La peinture utilise les 2 premières colonnes (couleur, finition) et laisse les 3 autres vides
        return super.toCSV() + ";" + couleur + ";" + finition + ";;;";
    }

}