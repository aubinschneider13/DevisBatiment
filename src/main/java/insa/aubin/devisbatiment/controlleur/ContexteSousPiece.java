package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.modele.Piece;
import insa.aubin.devisbatiment.view.AppView;
import insa.aubin.devisbatiment.view.PieceView;
import javafx.stage.Stage;

import java.util.List;

/**
 * Contexte actif quand l'utilisateur travaille à l'intérieur d'une pièce.
 *
 * Boutons visibles : navigation, échelle, mur, porte, fenêtre.
 * Même comportement que ContextePiece mais pour une Piece plutôt qu'un Appartement.
 */
public class ContexteSousPiece implements Contexte {

    private static final List<String> BOUTONS = List.of(
            "navigation", "echelle", "mur", "porte", "fenetre"
    );

    private final Piece piece;
    private final AppView appView;
    private final AppControleur appControleur;
    private final Stage stage;
    private final GestionnaireSauvegarde gestionnaire;

    private PieceView pieceView;
    private PieceControleur pieceControleur;

    public ContexteSousPiece(Piece piece,
                              AppView appView,
                              AppControleur appControleur,
                              Stage stage,
                              GestionnaireSauvegarde gestionnaire) {
        this.piece          = piece;
        this.appView        = appView;
        this.appControleur  = appControleur;
        this.stage          = stage;
        this.gestionnaire   = gestionnaire;
    }

    @Override
    public void installer() {
        pieceView = new PieceView(stage, gestionnaire, piece,
                                  appControleur.getAireImmeuble());
        pieceControleur = pieceView.getControleur();
        appView.afficherPiece(pieceView);
        appView.setInstructions(
            "Vue de « " + piece + " » — dessinez l'aménagement intérieur"
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
    public void onBtnEchelle() {
        appControleur.onBtnEchelle();
    }

    @Override
    public List<String> getBoutonsVisibles() {
        return BOUTONS;
    }
}