package insa.aubin.devisbatiment.modele;
/**
 *
 * @author Jeffrey Epstein
 */
public class Ouverture {
    private String idOuverture;
    private double largeur;
    private double hauteur;

    public Ouverture(String idOuverture, double largeur, double hauteur){
        this.idOuverture = idOuverture;
        this.largeur = largeur;
        this.hauteur = hauteur;
    }

    public String getIdOuverture(){
        return idOuverture;
    }

    public void setIdOuverture(String idOuverture){
        this.idOuverture = idOuverture;
    }

    public double getLargeur(){
        return largeur;
    }

    public void setLargeur(double largeur){
        this.largeur = largeur;
    }

    public double getHauteur(){
        return hauteur;
    }

    public void setHauteur(double hauteur){
        this.hauteur = hauteur;
    }
}
