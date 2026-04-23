package insa.aubin.devisbatiment.modele;

public class Immeuble extends Batiment {

    public Immeuble(String nomBat, int nbNiveau) {
        super(nomBat, "Immeuble", nbNiveau);
    }

    @Override
    public String toString() {
        return "Immeuble [id=" + getId() + ", nom=" + getNomBatiment()
                + ", niveaux=" + getNbNiveaux() + "]";
    }
}
