package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class Mur extends SurfaceAvecRevetement implements Dessin {

    private Point point1;
    private Point point2;
    private float hauteur;
    private List<Ouverture> listeOuvertures;
    private Color color = Color.BLACK;

    public Mur(Point point1, Point point2, float hauteur) {
        super("Mur");
        this.point1 = point1;
        this.point2 = point2;
        this.hauteur = hauteur;
        this.listeOuvertures = new ArrayList<>();
    }

    public void ajouterOuverture(Ouverture o) {
        if (o != null) {
            this.listeOuvertures.add(o);
        }
    }

    public float calculerLongueur() {
        float dX = this.point2.getX() - this.point1.getX();
        float dY = this.point2.getY() - this.point1.getY();
        return (float) Math.sqrt(dX * dX + dY * dY);
    }

    @Override
    public float calculerSurface() {
        return calculerLongueur() * this.hauteur;
    }

    public float calculerSurfaceNette() {
        float surfaceBrute = this.calculerSurface();
        float surfaceOuvertures = 0;
        for (Ouverture o : listeOuvertures) {
            surfaceOuvertures += o.getLargeur() * o.getHauteur();
        }
        return surfaceBrute - surfaceOuvertures;
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
    
    public float getHauteur() {
        return hauteur;
    }
    
    public void setHauteur(float hauteur) {
        this.hauteur = hauteur; 
    }
    
    public List<Ouverture> getListeOuvertures() {
        return listeOuvertures; 
    }
    
    public void setListeOuvertures(List<Ouverture> listeOuvertures) {
        this.listeOuvertures = listeOuvertures;
    }

    @Override
    public Color getColor() { return color; }

    @Override
    public void setColor(Color color) { this.color = color; }

    @Override
    public void dessiner(GraphicsContext gc) {
        gc.setStroke(this.color);
        gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());
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
        return "Mur [id=" + getId() + "]"
                + "(" + point1.getX() + "," + point1.getY() + ")"
                + "(" + point2.getX() + "," + point2.getY() + ")";
    }
}