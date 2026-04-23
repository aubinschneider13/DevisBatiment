package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Appartement {
    private String idAppart;
    private double nbPieces;
    ArrayList<Piece> pieces = new ArrayList<>();
    
    public Appartement(double nbPieces){
        this.idAppart = "Appartement" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date());
        this.nbPieces = nbPieces;
        this.pieces = new ArrayList<>();
    }

}
