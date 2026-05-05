package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;

public abstract class Batiment extends ElementDeConstruction {
    private String nomBatiment;
    private String typeBatiment;
    private Point point1;
    private Point point2;
    private Point point3;
    
    private ArrayList<Niveau> niveaux;

    public Batiment(String nomBatiment, String typeBatiment, Point point1, Point point2, Point point3) {
        super(typeBatiment);
        if (!typeBatiment.equals("Maison") && !typeBatiment.equals("Immeuble")) {
            throw new IllegalArgumentException("typeBatiment doit être 'Maison' ou 'Immeuble'");
        }
        this.nomBatiment = nomBatiment;
        this.typeBatiment = typeBatiment;
        
        if (!Point.sontOrthogonaux(point1, point2, point3)) {
            throw new IllegalArgumentException("Les points ne forment pas un angle droit en point2.");
        }
        
        this.point1 = point1;
        this.point2= point2;
        this.point3 = point3;
        
        
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