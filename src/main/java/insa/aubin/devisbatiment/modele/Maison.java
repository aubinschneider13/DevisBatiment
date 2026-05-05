package insa.aubin.devisbatiment.modele;

public class Maison extends Batiment {
    public Maison(String nomBatiment, Point point1, Point point2, Point point3) {
        super(nomBatiment, "Maison", point1, point2, point3);
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