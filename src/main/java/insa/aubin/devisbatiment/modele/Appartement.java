package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Appartement extends ElementDeConstruction {

    private int nbPieces;
    private double hauteurPlafond;
    private List<Piece> pieces;

    public Appartement(int nbPieces, double hauteurPlafond) {
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
    public double getHauteurPlafond() { return hauteurPlafond; }
    public List<Piece> getPieces() { return pieces; }

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
