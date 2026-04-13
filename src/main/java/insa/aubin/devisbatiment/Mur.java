/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package insa.aubin.devisbatiment;
import java.util.ArrayList;
import java.util.List;

/*@author Gabriel The Rizzler*/

public class Mur extends SurfaceAvecRevetement {
    
    private String idMur;
    private float xDebut;
    private float yDebut;
    private float xFin;
    private float yFin;
    private float hauteur;
    private List<Ouverture> listeOuvertures;

    public Mur(String idMur, float xDebut, float yDebut, float xFin, float yFin, float hauteur) {
        super();
        this.idMur = idMur;
        this.xDebut = xDebut;
        this.yDebut = yDebut;
        this.xFin = xFin;
        this.yFin = yFin;
        this.hauteur = hauteur;
        this.listeOuvertures = new ArrayList<>();
    }
    
    public String getIdMur(){
        return idMur;
    }

    public void setIdMur(String idMur){
        this.idMur = idMur;
    }

    public float getxDebut() {
        return xDebut;
    }

    public void setxDebut(float xDebut) {
        this.xDebut = xDebut;
    }

    public float getyDebut() {
        return yDebut;
    }

    public void setyDebut(float yDebut) {
        this.yDebut = yDebut;
    }

    public float getxFin() {
        return xFin;
    }

    public void setxFin(float xFin) {
        this.xFin = xFin;
    }
    
    public float getyFin() {
        return yFin;
    }
    
    public void setyFin(float yFin) {
        this.yFin = yFin;
    }

    public float getHauteur(){
        return hauteur;
    }

    public void setHauteur(float hauteur){
        this.hauteur = hauteur;
    }

    public List<Ouverture> getListeOuvertures(){
        return listeOuvertures;
    }

    public void setListeOuvertures(List<Ouverture> listeOuvertures){
        this.listeOuvertures = listeOuvertures;
    }

    //Méthode pour ajouter une ouverture dans la liste
    public void ajouterOuverture(Ouverture o){
        if(o != null){
            this.listeOuvertures.add(o);
        }
    }
    
    public float calculerLongueur(){
        float dX = this.xFin - this.xDebut;
        float dY = this.yFin - this.yDebut;
        return (float) Math.sqrt(dX*dX + dY*dY);
    }

    //Méthode pour calculer la surface brute du mur (cad sans les ouvertures)
    @Override
    public float calculerSurface(){ 
        return calculerLongueur() * this.hauteur;
    }

    public float calculerSurfaceNette(){
        float surfaceBrute = this.calculerSurface();
        float surfaceOuverture = 0;
        for (Ouverture o : listeOuvertures) {
            surfaceOuverture += o.getLargeur() * o.getHauteur();
        }
        return surfaceBrute - surfaceOuverture;
    }

    /*public Mur(){
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
        
        this.idMur = "Mur" + formatter.format(new Date());
    }*/
}
