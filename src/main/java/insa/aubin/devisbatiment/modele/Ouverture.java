package insa.aubin.devisbatiment.modele;

public abstract class Ouverture extends ElementDeConstruction {

    private float largeur;
    private float hauteur;

    public Ouverture(String prefixe, float largeur, float hauteur) {
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

    public float getLargeur() {
        return largeur;
    }
    
    public float getHauteur() {
        return hauteur;
    } 
}