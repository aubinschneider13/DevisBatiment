package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Fenetre extends Ouverture {
    private static final double COTE_FENETRE = 1.20;

    public Fenetre() {
        super("Fenetre" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()), COTE_FENETRE, COTE_FENETRE);
    }

    @Override
    public String toCSV() {
        return "FENETRE;" + getId() + ";" + COTE_FENETRE + ";" + COTE_FENETRE;
    }

    @Override
    public String toString() {
        return "Fenetre [id=" + getId() + ", largeur=" + COTE_FENETRE + ", hauteur=" + COTE_FENETRE + "]";
    }
   
}
