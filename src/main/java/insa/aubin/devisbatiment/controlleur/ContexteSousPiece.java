package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.modele.Piece;
import insa.aubin.devisbatiment.modele.SurfaceAvecRevetement;
import insa.aubin.devisbatiment.view.AppView;
import insa.aubin.devisbatiment.view.PieceView;
import javafx.stage.Stage;

import java.util.ArrayList;
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

    //private boolean modeSelectionActif = false;
    //private final List<SurfaceAvecRevetement> surfacesSelectionnees = new ArrayList<>();

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
        if (pieceView == null) {
            pieceView = new PieceView(stage, gestionnaire, piece,
                                      appControleur.getAireImmeuble());
            pieceControleur = pieceView.getControleur();
        }
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

    // =========================================================================
    // GESTION DU MODE SÉLECTION (POUR LES REVÊTEMENTS)
    // =========================================================================

    /*public boolean isModeSelectionActif() {
        return modeSelectionActif;
    }

    public void setModeSelectionActif(boolean modeSelectionActif) {
        this.modeSelectionActif = modeSelectionActif;
    }

    public List<SurfaceAvecRevetement> getSurfacesSelectionnees() {
        return surfacesSelectionnees;
    }

    public void ajouterSurface(SurfaceAvecRevetement surface) {
        if (surface != null && !surfacesSelectionnees.contains(surface)) {
            surfacesSelectionnees.add(surface);
        }
    }

    public void retirerSurface(SurfaceAvecRevetement surface) {
        surfacesSelectionnees.remove(surface);
    }

    public void viderSelection() {
        surfacesSelectionnees.clear();
    }*/

    public void activerModeSelection() {
        if (pieceControleur != null) {
            // On force le moteur de dessin à passer en mode sélection
            pieceControleur.changerEtat(PieceControleur.ETAT_SELECTION);
        }
    }

    public List<SurfaceAvecRevetement> getSurfacesSelectionnees() {
        if (pieceControleur != null) {
            return pieceControleur.getSurfacesSelectionnees();
        }
        return new ArrayList<>(); // Retourne une liste vide par sécurité
    }

    public void viderSelection() {
        if (pieceControleur != null) {
            pieceControleur.viderSelection();
            pieceControleur.changerEtat(PieceControleur.ETAT_RIEN); // Repasse en mode normal
        }
    }

    public Piece getPiece() {
        return piece;
    }
}