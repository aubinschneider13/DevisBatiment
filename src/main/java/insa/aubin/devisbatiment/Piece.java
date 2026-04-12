package insa.aubin.devisbatiment;
import java.util.ArrayList;
public class Piece {
    private String idPiece;
    private ArrayList<Usage> usage = new ArrayList<>();
    private ArrayList<Mur> mur = new ArrayList<>();
    private ArrayList<Plafond> plafond = new ArrayList<>();
    private ArrayList<Sol> sol = new ArrayList<>();
    private ArrayList<Revetement> revetements = new ArrayList<>();

    public Piece(String idPiece) {
        this.idPiece = idPiece;
        this.usage = new ArrayList<>();
        this.mur = new ArrayList<>();
        this.plafond = new ArrayList<>();
        this.sol = new ArrayList<>();
        this.revetements = new ArrayList<>();
    }

    public String getIdPiece() {
        return idPiece;
    }

    public void setIdPiece(String idPiece) {
        this.idPiece = idPiece;
    }

    public ArrayList<Usage> getUsage() {
        return usage;
    }

    public void setUsage(ArrayList<Usage> usage) {
        this.usage = usage;
    }

    public ArrayList<Mur> getMur() {
        return mur;
    }

    public void setMur(ArrayList<Mur> mur) {
        this.mur = mur;
    }

    public ArrayList<Plafond> getPlafond() {
        return plafond;
    }

    public void setPlafond(ArrayList<Plafond> plafond) {
        this.plafond = plafond;
    }

    public ArrayList<Sol> getSol() {
        return sol;
    }

    public void setSol(ArrayList<Sol> sol) {
        this.sol = sol;
    }

    public ArrayList<Revetement> getRevetements() {
        return revetements;
    }

    public void setRevetements(ArrayList<Revetement> revetements) {
        this.revetements = revetements;
    } 
}
