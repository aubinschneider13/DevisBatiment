package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;

public class Niveau extends ElementDeConstruction {
    private double hauteurPlafond;
    private ArrayList<Appartement> appartements;

    public Niveau(double hauteurPlafond) {
        super("Niveau");
        this.hauteurPlafond = hauteurPlafond;
        this.appartements = new ArrayList<>();
    }

    public Appartement ajouterAppartement(int nbPieces) {
        Appartement a = new Appartement(nbPieces, this.hauteurPlafond);
        this.appartements.add(a);
        return a;
    }

    public int getNbAppartements() {
        return appartements.size();
    }

    public ArrayList<Appartement> getAppartements() {
        return appartements;
    }

    public double getHauteurPlafond() {
        return hauteurPlafond;
    }

    public void setHauteurPlafond(double hauteurPlafond) {
        this.hauteurPlafond = hauteurPlafond;
    }

    @Override
    public String toCSV() {
        return "NIVEAU;" + getId() + ";" + getNbAppartements() + ";" + hauteurPlafond;
    }

    @Override
    public String toString() {
        return "Niveau [id=" + getId() + ", nbAppartements=" + getNbAppartements()
                + ", hauteurPlafond=" + hauteurPlafond + "]";
    }
}