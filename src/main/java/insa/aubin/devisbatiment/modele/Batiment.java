package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;

public abstract class Batiment extends ElementDeConstruction {
    private String nomBatiment;
    private String typeBatiment;
    private ArrayList<Niveau> niveaux;

    public Batiment(String nomBatiment, String typeBatiment) {
        super(typeBatiment);
        if (!typeBatiment.equals("Maison") && !typeBatiment.equals("Immeuble")) {
            throw new IllegalArgumentException("typeBatiment doit être 'Maison' ou 'Immeuble'");
        }
        this.nomBatiment = nomBatiment;
        this.typeBatiment = typeBatiment;
        this.niveaux = new ArrayList<>();
    }

    public Niveau ajouterNiveau(double hauteurPlafond) {
        Niveau n = new Niveau(hauteurPlafond);
        this.niveaux.add(n);
        return n;
    }

    public String getNomBatiment() {
        return nomBatiment;
    }

    public void setNomBatiment(String nomBatiment) {
        this.nomBatiment = nomBatiment;
    }

    public String getTypeBatiment() {
        return typeBatiment;
    }

    public void setTypeBatiment(String typeBatiment) {
        this.typeBatiment = typeBatiment;
    }

    public int getNbNiveaux() {
        return niveaux.size();
    }

    public ArrayList<Niveau> getNiveaux() {
        return niveaux;
    }

    @Override
    public String toCSV() {
        return getId() + ";" + nomBatiment + ";" + typeBatiment + ";" + getNbNiveaux();
    }

    @Override
    public String toString() {
        return typeBatiment + " [id=" + getId() + ", nom=" + nomBatiment + ", niveaux=" + getNbNiveaux() + "]";
    }
}