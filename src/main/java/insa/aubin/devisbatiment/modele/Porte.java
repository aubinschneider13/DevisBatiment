package insa.aubin.devisbatiment.modele;

public class Porte extends Ouverture {
    private static final double LARGEUR_PORTE = 0.90;
    private static final double HAUTEUR_PORTE = 2.10;

    public Porte() {
        super("Porte", LARGEUR_PORTE, HAUTEUR_PORTE);
    }

    @Override
    public String toCSV() {
        return "PORTE;" + getId() + ";" + LARGEUR_PORTE + ";" + HAUTEUR_PORTE;
    }

    @Override
    public String toString() {
        return "Porte [id=" + getId() + ", largeur=" + LARGEUR_PORTE + ", hauteur=" + HAUTEUR_PORTE + "]";
    }
}