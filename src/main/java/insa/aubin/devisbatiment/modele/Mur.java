package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class Mur extends SurfaceAvecRevetement implements Dessin {
    
    private Point point1;
    private Point point2;
    private double hauteur;
    private List<Ouverture> listeOuvertures;
    private Color color = Color.BLACK;
    private static int compteurMurs = 0;
    private int numeroUnique;

    public Mur(Point point1, Point point2, double hauteur) {
        super("Mur");
        this.point1 = point1;
        this.point2 = point2;
        this.hauteur = hauteur;
        this.listeOuvertures = new ArrayList<>();

        compteurMurs++;
        this.numeroUnique = compteurMurs;
    }

    public Mur(Point point1, Point point2){
        this(point1, point2, 2.5);
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
        return Math.sqrt(dX*dX + dY*dY);
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

    /** Calcule la distance la plus courte entre un point et ce segment de mur */
    public double distanceA(Point p) {
        double x1 = point1.getX(), y1 = point1.getY();
        double x2 = point2.getX(), y2 = point2.getY();
        double px = p.getX(), py = p.getY();

        double l2 = Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);
        if (l2 == 0) return Math.sqrt(Math.pow(px - x1, 2) + Math.pow(py - y1, 2));

        double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
        t = Math.max(0, Math.min(1, t));

        return Math.sqrt(Math.pow(px - (x1 + t * (x2 - x1)), 2) + Math.pow(py - (y1 + t * (y2 - y1)), 2));
    }

    //Getters et Setters

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

    public void setHauteur(double hauteur){
        this.hauteur = hauteur;
    }

    public List<Ouverture> getListeOuvertures(){
        return listeOuvertures;
    }

    public void setListeOuvertures(List<Ouverture> listeOuvertures){
        this.listeOuvertures = listeOuvertures;
    }

    //Méthodes pour éléments graphiques
    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public void dessiner(GraphicsContext gc) {
        // Dessin du mur
        gc.setStroke(this.color);
        gc.setLineWidth(0.1);
        gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());

        // Dessin des ouvertures sur le mur (petits traits colorés)
        for(Ouverture o : listeOuvertures) {
            gc.setLineWidth(0.15);
            gc.setStroke(o instanceof Porte ? Color.BROWN : Color.DEEPSKYBLUE);

            // On dessine l'ouverture au milieu du mur pour simplifier
            double midX = (point1.getX() + point2.getX()) / 2;
            double midY = (point1.getY() + point2.getY()) / 2;
            gc.strokeLine(midX - 0.2, midY, midX + 0.2, midY);
        }

        // Les coins
        double rayon = 0.08;
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(0.02);
        gc.fillOval(point1.getX() - rayon, point1.getY() - rayon, rayon * 2, rayon * 2);
        gc.strokeOval(point1.getX() - rayon, point1.getY() - rayon, rayon * 2, rayon * 2);
        gc.fillOval(point2.getX() - rayon, point2.getY() - rayon, rayon * 2, rayon * 2);
        gc.strokeOval(point2.getX() - rayon, point2.getY() - rayon, rayon * 2, rayon * 2);
    }

    @Override
    public String toCSV() {
        return "MUR;" + super.toCSV()
                + ";" + point1.getX() + ";" + point1.getY()
                + ";" + point2.getX() + ";" + point2.getY()
                + ";" + hauteur;
    }

    @Override
    public String toString() {
        return String.format("Mur n°%d (%.2f m)", numeroUnique, calculerLongueur());
    }
}