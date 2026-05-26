package insa.aubin.devisbatiment.modele;

public class Maison extends Batiment {

    public Maison(String nomBatiment, AireImmeuble aire) {
        super(nomBatiment, "Maison", aire);
    }

    @Override
    public String toCSV() {
        return String.format(java.util.Locale.US,
            "MAISON;%s;%s;%d;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f",
            getId(), getNomBatiment(), getNbNiveaux(),
            getPoint1().getX(), getPoint1().getY(),
            getPoint2().getX(), getPoint2().getY(),
            getPoint3().getX(), getPoint3().getY(),
            getPoint4().getX(), getPoint4().getY()
        );
    }

    @Override
    public String toString() {
        return "Maison [id=" + getId() + ", nom=" + getNomBatiment()
                + ", niveaux=" + getNbNiveaux() + "]";
    }
}
