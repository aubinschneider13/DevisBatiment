package insa.aubin.devisbatiment.modele;

public class Immeuble extends Batiment {
    public Immeuble(String nomBatiment, Point point1, Point point2, Point point3) {
        super(nomBatiment, "Immeuble", point1, point2, point3);
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