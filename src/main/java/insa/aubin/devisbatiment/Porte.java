package insa.aubin.devisbatiment;

public class Porte extends Ouverture {
    private static final float LARGEUR_PORTE = 0.90f;
    private static final float HAUTEUR_PORTE = 2.10f;

    public Porte(String idPorte){
        super(idPorte, LARGEUR_PORTE, HAUTEUR_PORTE);
    }
}
