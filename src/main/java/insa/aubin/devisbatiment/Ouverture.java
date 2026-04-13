package insa.aubin.devisbatiment;
/**
 *
 * Aubin
 */
public class Ouverture {
    private String idOuverture;
    private float largeur;
    private float hauteur;

    public Ouverture(String idOuverture, float largeur, float hauteur){
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

    public float getLargeur(){
        return largeur;
    }

    public void setLargeur(float largeur){
        this.largeur = largeur;
    }

    public float getHauteur(){
        return hauteur;
    }

    public void setHauteur(float hauteur){
        this.hauteur = hauteur;
    }
}
