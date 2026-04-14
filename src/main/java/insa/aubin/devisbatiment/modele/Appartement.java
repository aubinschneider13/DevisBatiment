package insa.aubin.devisbatiment.modele;
/**
 *
 * @author Jeffrey Epstein
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Appartement {
    private String idAppart;
    private double nbPieces;
    ArrayList<Piece> pieces = new ArrayList<>();
    
    public Appartement(double nbPieces){
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
        
        this.idAppart = "Appartement" + formatter.format(new Date());
        this.nbPieces = nbPieces;
        this.pieces = new ArrayList<>();
    }

}
