package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;

public class Appartement extends ElementDeConstruction {

    private int nbPieces;
    private float hauteurPlafond;
    private ArrayList<Piece> pieces;

    public Appartement(int nbPieces, float hauteurPlafond) {
        super("Appartement");
        this.nbPieces = nbPieces;
        this.hauteurPlafond = hauteurPlafond;
        this.pieces = new ArrayList<>();
    }

    public Piece ajouterPiece(List<Point> points) {
        Piece p = new Piece(points, this.hauteurPlafond);
        this.pieces.add(p);
        return p;
    }

    public int getNbPieces() { return nbPieces; }
    public void setNbPieces(int nbPieces) { this.nbPieces = nbPieces; }
    public float getHauteurPlafond() { return hauteurPlafond; }
    public ArrayList<Piece> getPieces() { return pieces; }

    @Override
    public String toCSV() {
        return "APPARTEMENT;" + getId() + ";" + nbPieces + ";" + hauteurPlafond;
    }

    @Override
    public String toString() {
        return "Appartement [id=" + getId() + ", nbPieces=" + nbPieces
                + ", hauteurPlafond=" + hauteurPlafond + "]";
    }
}
