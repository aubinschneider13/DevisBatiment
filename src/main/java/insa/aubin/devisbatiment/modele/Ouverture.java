package insa.aubin.devisbatiment.modele;

public abstract class Ouverture extends ElementDeConstruction{
    private double largeur;
    private double hauteur;

    public Ouverture(String prefixe, double largeur, double hauteur){
        super(prefixe);
        this.largeur = largeur;
        this.hauteur = hauteur;
    }

    @Override
    public String toCSV() {
        return getId() + ";" + largeur + ";" + hauteur;
    }

    @Override
    public String toString() {
        return "Ouverture [id=" + getId() + ", largeur=" + largeur + ", hauteur=" + hauteur + "]";
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