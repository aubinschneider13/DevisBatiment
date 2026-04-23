package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Fenetre extends Ouverture {
    private static final double COTE_FENETRE = 1.20;

    public Fenetre() {
        super("Fenetre" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()), COTE_FENETRE, COTE_FENETRE);
    }

    public String toCSV() {
        return "FENETRE;" + COTE_FENETRE + ";" + COTE_FENETRE;
    }
   
}
