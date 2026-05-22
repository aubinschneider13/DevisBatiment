package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.Couloir;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.view.AppView;
import insa.aubin.devisbatiment.view.PieceView;
import javafx.stage.Stage;
import javafx.scene.input.KeyEvent;

import java.util.List;

public class ContexteCouloir implements Contexte {

    private static final List<String> BOUTONS = List.of(
            "navigation", "echelle", "mur", "porte", "fenetre"
    );

    private final Couloir couloir;
    private final AppView appView;
    private final AppControleur appControleur;
    private final Stage stage;
    private final GestionnaireSauvegarde gestionnaire;

    private PieceView pieceView;
    private PieceControleur pieceControleur;

    public ContexteCouloir(Couloir couloir, AppView appView,
                           AppControleur appControleur, Stage stage,
                           GestionnaireSauvegarde gestionnaire) {
        this.couloir       = couloir;
        this.appView       = appView;
        this.appControleur = appControleur;
        this.stage         = stage;
        this.gestionnaire  = gestionnaire;
    }

    @Override
    public void installer() {
        if (pieceView == null) {
            pieceView = new PieceView(stage, gestionnaire, couloir, appControleur.getAireImmeuble());
            pieceControleur = pieceView.getControleur();
        }
        appView.afficherPiece(pieceView);
        appView.setInstructions(
            "Vue de « " + couloir + " » — zone de circulation"
        );
    }

    @Override
    public void desinstaller() {
        if (pieceControleur != null) {
            pieceControleur.nettoyer();
        }
    }

    @Override
    public void onBtnNavigation() {
        if (pieceControleur != null)
            pieceControleur.changerEtat(PieceControleur.ETAT_RIEN);
    }

    @Override
    public void onBtnMur() {
        if (pieceControleur != null)
            pieceControleur.changerEtat(PieceControleur.ETAT_MUR);
    }

    @Override
    public void onBtnPorte() {
        if (pieceControleur != null)
            pieceControleur.changerEtat(PieceControleur.ETAT_PORTE);
    }

    @Override
    public void onBtnFenetre() {
        if (pieceControleur != null)
            pieceControleur.changerEtat(PieceControleur.ETAT_FENETRE);
    }

    @Override
    public void onBtnEchelle() {
        appControleur.onBtnEchelle();
    }

    @Override
    public List<String> getBoutonsVisibles() {
        return BOUTONS;
    }

    public Couloir getCouloir() { return couloir; }
    public PieceControleur getPieceControleur() { return pieceControleur; }

    @Override
    public void gererToucheClavier(KeyEvent e) {
        if (pieceControleur != null) {
            pieceControleur.gererToucheClavier(e);
        }
    }
}