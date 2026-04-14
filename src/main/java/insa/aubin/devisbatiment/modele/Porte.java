package insa.aubin.devisbatiment.modele;

public class Porte extends Ouverture {
    private static final double LARGEUR_PORTE = 0.90f;
    private static final double HAUTEUR_PORTE = 2.10f;

    public Porte(String idPorte){
        super(idPorte, LARGEUR_PORTE, HAUTEUR_PORTE);
    }
}
