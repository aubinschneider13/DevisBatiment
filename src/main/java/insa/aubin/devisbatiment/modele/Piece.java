package insa.aubin.devisbatiment.modele;

/**
 *
 * @author Jeffrey Epstein
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Piece {
    private String idPiece;
    private List<Mur> mur = new ArrayList<>();
    private List<Point> points = new ArrayList<>();
    
    private List<Usage> usage = new ArrayList<>();
    private List<Plafond> plafond = new ArrayList<>();
    private List<Sol> sol = new ArrayList<>();
    private List<Revetement> revetements = new ArrayList<>();
    
    //à modifier plus tard
    private double hauteurSousPlafond = 12.2;
    
    public Piece(List<Point> points) {
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
        
        this.idPiece = "Piece" + formatter.format(new Date());
        this.points = points;
        
        this.usage = new ArrayList<>();
        this.mur = new ArrayList<>();
        this.plafond = new ArrayList<>();
        this.sol = new ArrayList<>();
        this.revetements = new ArrayList<>();
        
        for (int i = 0; i < points.size(); i++) {
            Point debut = points.get(i);
            Point fin = points.get((i + 1) % points.size()); 
            this.mur.add(new Mur(debut, fin, hauteurSousPlafond));
        }
    }

    public String getIdPiece() {
        return idPiece;
    }

    public void setIdPiece(String idPiece) {
        this.idPiece = idPiece;
    }

    public List<Usage> getUsage() {
        return usage;
    }

    public void setUsage(ArrayList<Usage> usage) {
        this.usage = usage;
    }

    public List<Mur> getMur() {
        return mur;
    }

    public void setMur(List<Mur> mur) {
        this.mur = mur;
    }

    public List<Plafond> getPlafond() {
        return plafond;
    }

    public void setPlafond(List<Plafond> plafond) {
        this.plafond = plafond;
    }

    public List<Sol> getSol() {
        return sol;
    }

    public void setSol(List<Sol> sol) {
        this.sol = sol;
    }

    public List<Revetement> getRevetements() {
        return revetements;
    }

    public void setRevetements(List<Revetement> revetements) {
        this.revetements = revetements;
    } 
}