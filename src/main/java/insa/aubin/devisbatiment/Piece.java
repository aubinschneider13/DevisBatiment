package insa.aubin.devisbatiment;
import java.util.ArrayList;
import java.util.List;
public class Piece {
    private String idPiece;
    private List<Usage> usage = new ArrayList<>();
    private List<Mur> mur = new ArrayList<>();
    private List<Plafond> plafond = new ArrayList<>();
    private List<Sol> sol = new ArrayList<>();
    private List<Revetement> revetements = new ArrayList<>();

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
