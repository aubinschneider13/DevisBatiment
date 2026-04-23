package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;

public class Niveau extends ElementDeConstruction {

    private double nbAppartements;
    private double hauteurPlafond;
    private ArrayList<Appartement> appartements;

    public Niveau(double nbAppartements, double hauteurPlafond) {
        super("Niveau");
        this.nbAppartements = nbAppartements;
        this.hauteurPlafond = hauteurPlafond;
        this.appartements = new ArrayList<>();
    }
    
    //Getters and Setters

    public Appartement ajouterAppartement(int nbPieces) {
        Appartement a = new Appartement(nbPieces, this.hauteurPlafond);
        this.appartements.add(a);
        return a;
    }

    public double getNbAppartements() {
        return nbAppartements;
    }

    public void setNbAppartements(double nbAppartements) {
        this.nbAppartements = nbAppartements;
    }

    public double getHauteurPlafond() {
        return hauteurPlafond;
    }

    public void setHauteurPlafond(double hauteurPlafond) {
        this.hauteurPlafond = hauteurPlafond;
    }

    public ArrayList<Appartement> getAppartements() {
        return appartements;
    }

    @Override
    public String toCSV() {
        return "NIVEAU;" + getId() + ";" + nbAppartements + ";" + hauteurPlafond;
    }

    @Override
    public String toString() {
        return "Niveau [id=" + getId() + ", nbAppartements=" + nbAppartements
                + ", hauteurPlafond=" + hauteurPlafond + "]";
    }
}