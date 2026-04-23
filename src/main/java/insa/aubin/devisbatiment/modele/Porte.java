package insa.aubin.devisbatiment.modele;

public class Porte extends Ouverture {

    private static final float LARGEUR_PORTE = 0.90f;
    private static final float HAUTEUR_PORTE = 2.10f;

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