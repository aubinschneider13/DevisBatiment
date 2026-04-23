package insa.aubin.devisbatiment.modele;

public class Maison extends Batiment {

    public Maison(String nomBatiment) {
        super(nomBatiment, "Maison", 1);
    }

    @Override
    public String toString() {
        return "Maison [id=" + getId() + ", nom=" + getNomBatiment()
                + ", niveaux=" + getNbNiveaux() + "]";
    }
}