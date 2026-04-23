package insa.aubin.devisbatiment.modele;

public class Maison extends Batiment {

    public Maison(String nomBatiment) {
        super(nomBatiment, "Maison", 1);
    }
    
    @Override
    public String toCSV() {
        return "MAISON" + super.getId() + ";" + super.getNomBatiment() + ";" + "MAISON" + ";" + super.getNbNiveaux();
    }
   
    @Override
    public String toString() {
        return "Maison [id=" + getId() + ", nom=" + getNomBatiment()
                + ", niveaux=" + getNbNiveaux() + "]";
    }
}