package insa.aubin.devisbatiment.modele;

public abstract class Ouverture extends ElementDeConstruction{
    private double largeur;
    private double hauteur;
    private double positionSurMur;

    public Ouverture(String prefixe, double largeur, double hauteur,  double positionSurMur) {
        super(prefixe);
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.positionSurMur = positionSurMur;
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

    public double getPositionSurMur() {
        return positionSurMur;
    }

    public void setPositionSurMur(double t) {
        this.positionSurMur = Math.max(0.0, Math.min(1.0, t));
    }
    
}