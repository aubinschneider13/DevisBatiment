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

    /**
     * Calcule la valeur t ∈ [0,1] correspondant à la projection
     * d'un point P sur ce segment de mur.
     * Retourne la position relative : 0 = point1, 1 = point2.
     */
    public double calculerPositionSurMur(Point p) {
        double x1 = point1.getX(), y1 = point1.getY();
        double x2 = point2.getX(), y2 = point2.getY();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double l2 = dx*dx + dy*dy;

        if (l2 == 0) return 0.5;

        double t = ((p.getX()-x1)*dx + (p.getY()-y1)*dy) / l2;

        // Simple clamp entre 0 et 1
        return Math.max(0.0, Math.min(1.0, t));
    }

    /**
     * Calcule les coordonnées XY d'un point à la position t sur le mur.
     */
    public Point getPointSurMur(double t) {
        double x = point1.getX() + t * (point2.getX() - point1.getX());
        double y = point1.getY() + t * (point2.getY() - point1.getY());
        return new Point(x, y);
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
        gc.strokeLine(
                point1.getX(), point1.getY(),
                point2.getX(), point2.getY()
        );

        double angle = Math.toDegrees(Math.atan2(
                point2.getY() - point1.getY(),
                point2.getX() - point1.getX()
        ));

        // ✅ Chaque ouverture à SA position réelle
        for (Ouverture o : listeOuvertures) {
            gc.save();

            // Position exacte selon positionSurMur
            Point pos = getPointSurMur(o.getPositionSurMur());
            gc.translate(pos.getX(), pos.getY());
            gc.rotate(angle);

            if (o instanceof Porte) {
                dessinerSymbolePorte(gc, o.getLargeur());
            } else if (o instanceof Fenetre) {
                dessinerSymboleFenetre(gc, o.getLargeur());
            }

            gc.restore();
        }

        // Coins du mur
        double rayon = 0.08;
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(0.02);
        gc.fillOval(point1.getX()-rayon, point1.getY()-rayon, rayon*2, rayon*2);
        gc.strokeOval(point1.getX()-rayon, point1.getY()-rayon, rayon*2, rayon*2);
        gc.fillOval(point2.getX()-rayon, point2.getY()-rayon, rayon*2, rayon*2);
        gc.strokeOval(point2.getX()-rayon, point2.getY()-rayon, rayon*2, rayon*2);
    }

    /**
     * Symbole architectural d'une porte :
     * - Ouverture dans le mur (trait blanc)
     * - Arc de cercle montrant le débattement
     */
    private void dessinerSymbolePorte(GraphicsContext gc, double largeur) {
        double l = largeur;

        // 1. Effacer le mur là où est la porte (simulé par trait blanc épais)
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.15);
        gc.strokeLine(-l/2, 0, l/2, 0);

        // 2. Encadrements de la porte (petits tirets aux extrémités)
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.05);
        gc.strokeLine(-l/2, -0.08, -l/2, 0.08);
        gc.strokeLine( l/2, -0.08,  l/2, 0.08);

        // 3. Arc de débattement (quart de cercle)
        gc.setStroke(Color.web("#8B4513")); // marron
        gc.setLineWidth(0.04);
        gc.setLineDashes(0.05, 0.05);
        gc.strokeArc(-l/2, -l, l, l, 270, 90, javafx.scene.shape.ArcType.OPEN);
        gc.setLineDashes(0); // reset

        // 4. Vantail de la porte (trait plein)
        gc.setStroke(Color.web("#8B4513"));
        gc.setLineWidth(0.05);
        gc.strokeLine(-l/2, 0, -l/2, -l);
    }

    /**
     * Symbole architectural d'une fenêtre :
     * - Ouverture dans le mur
     * - Double trait représentant le vitrage
     */
    private void dessinerSymboleFenetre(GraphicsContext gc, double largeur) {
        double l = largeur;
        double ep = 0.06; // épaisseur du vitrage

        // 1. Effacer le mur (trait blanc)
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.15);
        gc.strokeLine(-l/2, 0, l/2, 0);

        // 2. Encadrements
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.05);
        gc.strokeLine(-l/2, -0.08, -l/2, 0.08);
        gc.strokeLine( l/2, -0.08,  l/2, 0.08);

        // 3. Double trait du vitrage
        gc.setStroke(Color.DEEPSKYBLUE);
        gc.setLineWidth(0.04);
        gc.strokeLine(-l/2, -ep, l/2, -ep);
        gc.strokeLine(-l/2,  ep, l/2,  ep);
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