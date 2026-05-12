package insa.aubin.devisbatiment.modele;

public class Maison extends Batiment {

    public Maison(String nomBatiment, AireImmeuble aire) {
        super(nomBatiment, "Maison", aire);
    }

    @Override
    public String toCSV() {
        return "MAISON;" + getId() + ";" + getNomBatiment() + ";" + getNbNiveaux();
    }

    @Override
    public String toString() {
        return "Maison [id=" + getId() + ", nom=" + getNomBatiment()
                + ", niveaux=" + getNbNiveaux() + "]";
    }
}