package insa.aubin.devisbatiment;

public class Fenetre extends Ouverture {
    private static final double COTE_FENETRE = 1.20f;  //Par convention, les nomes des constantes s'écrivent comme : UPPER_SNAKE_CASE

    public Fenetre(String idFenetre){
        super(idFenetre, COTE_FENETRE, COTE_FENETRE);
    }
}
