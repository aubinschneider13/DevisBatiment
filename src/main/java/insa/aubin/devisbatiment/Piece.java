package insa.aubin.devisbatiment;
import java.util.*;
public class Piece {
    private String idPiece;
    private ArrayList<Usage> usage = new ArrayList<>();
    private ArrayList<Mur> mur = new ArrayList<>();
    private ArratList<Plafond> plafond = new ArrayList<>();
    private ArrayList<Sol> sol = new ArrayList<>();
    private ArrayList<Revetement> revetements = new ArrayList<>();
}
