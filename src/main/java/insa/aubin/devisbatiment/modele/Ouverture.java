package insa.aubin.devisbatiment.modele;

public abstract class Ouverture {
    private String idOuverture;
    private double largeur;
    private double hauteur;

    public Ouverture(String idOuverture, double largeur, double hauteur){
        this.idOuverture = idOuverture;
        this.largeur = largeur;
        this.hauteur = hauteur;
    }
    
    public abstract String toCSV();
    
    //Getters and Setters

    public String getIdOuverture() {
        return idOuverture;
    }

    public double getLargeur() {
        return largeur;
    }

    public void setLargeur(double largeur) {
        this.largeur = largeur;
    }

    public double getHauteur() {
        return hauteur;
    }

    public void setHauteur(double hauteur) {
        this.hauteur = hauteur;
    }
    
}