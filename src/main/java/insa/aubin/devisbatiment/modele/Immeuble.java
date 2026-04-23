package insa.aubin.devisbatiment.modele;

public class Immeuble extends Batiment {

    public Immeuble(String nomBat, int nbNiveau) {
        super(nomBat, "Immeuble", nbNiveau);
    }
    
    @Override
    public String toCSV() {
        return "IMMEUBLE" + super.getId() + ";" + super.getNomBatiment() + ";" + "IMMEUBLE" + ";" + super.getNbNiveaux();
    }
    
    @Override
    public String toString() {
        return "Immeuble [id=" + getId() + ", nom=" + getNomBatiment()
                + ", niveaux=" + getNbNiveaux() + "]";
    }
}
