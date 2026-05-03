package insa.aubin.devisbatiment.modele;

public class Immeuble extends Batiment {
    public Immeuble(String nomBatiment) {
        super(nomBatiment, "Immeuble");
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