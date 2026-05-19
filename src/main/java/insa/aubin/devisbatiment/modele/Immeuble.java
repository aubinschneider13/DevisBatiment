package insa.aubin.devisbatiment.modele;

public class Immeuble extends Batiment {

    public Immeuble(String nomBatiment, AireImmeuble aire) {
        super(nomBatiment, "Immeuble", aire);
    }

    @Override
    public String toCSV() {
        return String.format(java.util.Locale.US,
            "IMMEUBLE;%s;%s;%d;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f",
            getId(), getNomBatiment(), getNbNiveaux(),
            getPoint1().getX(), getPoint1().getY(),
            getPoint2().getX(), getPoint2().getY(),
            getPoint3().getX(), getPoint3().getY(),
            getPoint4().getX(), getPoint4().getY()
        );
    }

    @Override
    public String toString() {
        return "Immeuble [id=" + getId() + ", nom=" + getNomBatiment()
                + ", niveaux=" + getNbNiveaux() + "]";
    }
}