package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;

public class Appartement extends ElementDeConstruction {
    private double hauteurPlafond;
    private List<Piece> pieces;

    public Appartement(int nbPieces, double hauteurPlafond) {
        super("Appartement");
        this.hauteurPlafond = hauteurPlafond;
        this.pieces = new ArrayList<>();
    }

    public Piece ajouterPiece(List<Point> points) {
        Piece p = new Piece(points, this.hauteurPlafond);
        this.pieces.add(p);
        return p;
    }

    public int getNbPieces() {
        return pieces.size();
    }

    public double getHauteurPlafond() {
        return hauteurPlafond;
    }

    public void setHauteurPlafond(double hauteurPlafond) {
        this.hauteurPlafond = hauteurPlafond;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    @Override
    public String toCSV() {
        return "APPARTEMENT;" + getId() + ";" + getNbPieces() + ";" + hauteurPlafond;
    }

    @Override
    public String toString() {
        return "Appartement [id=" + getId() + ", nbPieces=" + getNbPieces()
                + ", hauteurPlafond=" + hauteurPlafond + "]";
    }
}