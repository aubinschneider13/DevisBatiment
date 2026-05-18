// Parquet.java
package insa.aubin.devisbatiment.modele;

public class Parquet extends Revetement {
    private String essenceBois;
    private String finition;

    public Parquet(String id, String designation, boolean pourMur, boolean pourSol, boolean pourPlafond, double prixUnitaire, String essenceBois, String finition) {
        super(id, designation, pourMur, pourSol, pourPlafond, prixUnitaire);
        this.essenceBois = essenceBois;
        this.finition = finition;
    }

    public String getEssenceBois() { return essenceBois; }
    public String getFinition() { return finition; }

    @Override
    public String toCSV() {
        // Le parquet laisse couleur vide, remplit finition, laisse dimension vide, remplit bois, et laisse matiere vide
        return super.toCSV() + ";;" + finition + ";;" + essenceBois + ";";
    }
}