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
            "navigation", "echelle", "mur", "porte", "fenetre", "escalier", "ascenseur"
    );

    private final Couloir couloir;
    private final AppView appView;
    private final AppControleur appControleur;
    private final NiveauControleur niveauControleur;
    private final Stage stage;
    private final GestionnaireSauvegarde gestionnaire;

    private PieceView pieceView;
    private PieceControleur pieceControleur;

    public ContexteCouloir(Couloir couloir, AppView appView,
                           AppControleur appControleur, Stage stage,
                           GestionnaireSauvegarde gestionnaire) {
        this(couloir, appView, appControleur, stage, gestionnaire, null);
    }

    public ContexteCouloir(Couloir couloir, AppView appView,
                           AppControleur appControleur, Stage stage,
                           GestionnaireSauvegarde gestionnaire,
                           NiveauControleur niveauControleur) {
        this.couloir       = couloir;
        this.appView       = appView;
        this.appControleur = appControleur;
        this.stage         = stage;
        this.gestionnaire  = gestionnaire;
        this.niveauControleur = niveauControleur;
    }

    @Override
    public void installer() {
        if (pieceView == null) {
            pieceView = new PieceView(stage, gestionnaire, couloir,
                    appControleur.getAireImmeuble(),
                    niveauControleur != null ? niveauControleur.getNiveau().getTremies() : List.of());
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
            pieceControleur.annulerConstruction();
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
    public void onBtnEscalier() {
        if (niveauControleur != null) {
            appControleur.activerPlacementTremieDepuisCouloir(niveauControleur, true);
        }
    }

    @Override
    public void onBtnAscenseur() {
        if (niveauControleur != null) {
            appControleur.activerPlacementTremieDepuisCouloir(niveauControleur, false);
        }
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
