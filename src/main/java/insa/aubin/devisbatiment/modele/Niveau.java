package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;

public class Niveau extends ElementDeConstruction {

    private int nbAppartements;
    private float hauteurPlafond;
    private ArrayList<Appartement> appartements;

    public Niveau(int nbAppartements, float hauteurPlafond) {
        super("Niveau");
        this.nbAppartements = nbAppartements;
        this.hauteurPlafond = hauteurPlafond;
        this.appartements = new ArrayList<>();
    }

    public Appartement ajouterAppartement(int nbPieces) {
        Appartement a = new Appartement(nbPieces, this.hauteurPlafond);
        this.appartements.add(a);
        return a;
    }

    public int getNbAppartements() {
        return nbAppartements;
    }
    
    public void setNbAppartements(int nbAppartements) {
        this.nbAppartements = nbAppartements;
    }
    
    public float getHauteurPlafond() {
        return hauteurPlafond;
    }
    
    public void setHauteurPlafond(float hauteurPlafond) {
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