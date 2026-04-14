/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package insa.aubin.devisbatiment;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

/*@author Gabriel tkt*/

public class Mur extends SurfaceAvecRevetement {
    
    private String idMur;
    private Point point1;
    private Point point2;
    private double hauteur;
    private List<Ouverture> listeOuvertures;

    public Mur(Point point1, Point point2, double hauteur) {
        super();
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
        
        this.idMur = "Mur" + formatter.format(new Date());
        this.point1 = point1;
        this.point2 = point2;
        this.hauteur = hauteur;
        this.listeOuvertures = new ArrayList<>();
    }
    
    public String getIdMur(){
        return idMur;
    }

    public void setIdMur(String idMur){
        this.idMur = idMur;
    }

    public Point getPoint1() {
        return point1;
    }

    public void setPoint1(Point point1) {
        this.point1 = point1;
    }

    public Point getPoint2() {
        return point2;
    }

    public void setPoint2(Point point2) {
        this.point2 = point2;
    }

    public double getHauteur(){
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
    
    public double calculerLongueur(){
        double dX = this.point2.getX() - this.point1.getX();
        double dY = this.point2.getY() - this.point1.getY();
        return (double) Math.sqrt(dX*dX + dY*dY);
    }

    //Méthode pour calculer la surface brute du mur (cad sans les ouvertures)
    @Override
    public double calculerSurface(){ 
        return calculerLongueur() * this.hauteur;
    }

    public double calculerSurfaceNette(){
        double surfaceBrute = this.calculerSurface();
        double surfaceOuverture = 0;
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
