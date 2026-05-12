package insa.aubin.devisbatiment.modele;

public class Immeuble extends Batiment {

    public Immeuble(String nomBatiment, AireImmeuble aire) {
        super(nomBatiment, "Immeuble", aire);
    }

    @Override
    public String toCSV() {
        return "IMMEUBLE;" + getId() + ";" + getNomBatiment() + ";" + getNbNiveaux();
    }

    @Override
    public String toString() {
        return "Immeuble [id=" + getId() + ", nom=" + getNomBatiment()
                + ", niveaux=" + getNbNiveaux() + "]";
    }
}