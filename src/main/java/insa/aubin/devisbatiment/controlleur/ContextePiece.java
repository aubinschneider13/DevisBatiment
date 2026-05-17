package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.controlleur.AppControleur;
import insa.aubin.devisbatiment.controlleur.PieceControleur;
import insa.aubin.devisbatiment.modele.Appartement;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.view.AppView;
import insa.aubin.devisbatiment.view.PieceView;
import javafx.stage.Stage;

import java.util.List;

/**
 * Contexte actif quand l'utilisateur aménage l'intérieur d'un appartement :
 * dessin des cloisons, insertion de portes et de fenêtres.
 *
 * Boutons visibles : navigation, échelle, mur, porte, fenêtre.
 * Les boutons appartement et ajouterNiveau sont masqués car ils n'ont pas de
 * sens à l'intérieur d'un appartement.
 *
 * Différence clé avec l'ancienne approche : PieceView n'est plus une fenêtre
 * autonome avec sa propre toolbar et son propre TreeView. C'est désormais un
 * simple canvas inséré dans la zone centrale d'AppView. La toolbar commune
 * d'AppView pilote PieceControleur via ce contexte.
 */
public class ContextePiece implements Contexte {

    /** Identifiants des boutons affichés dans ce contexte (ordre = ordre toolbar). */
    private static final List<String> BOUTONS = List.of(
            "navigation", "echelle", "mur", "piece", "porte", "fenetre"
    );

    // --- Dépendances ---
    private final Appartement appartement;
    private final AppView appView;
    private final AppControleur appControleur;

    // --- Vue et contrôleur créés à l'installation ---
    private PieceView pieceView;
    private PieceControleur pieceControleur;

    // --- Stage et gestionnaire nécessaires pour instancier PieceView ---
    private final Stage stage;
    private final GestionnaireSauvegarde gestionnaire;

    /**
     * @param appartement  appartement dont on va aménager l'intérieur
     * @param appView      vue racine (pour insérer le canvas de la pièce)
     * @param appControleur chef d'orchestre
     * @param stage        fenêtre principale (transmis à PieceControleur)
     * @param gestionnaire service de sauvegarde (transmis à PieceControleur)
     */
    public ContextePiece(Appartement appartement,
                         AppView appView,
                         AppControleur appControleur,
                         Stage stage,
                         GestionnaireSauvegarde gestionnaire) {
        this.appartement    = appartement;
        this.appView        = appView;
        this.appControleur  = appControleur;
        this.stage          = stage;
        this.gestionnaire   = gestionnaire;
    }

    // =========================================================================
    // CYCLE DE VIE
    // =========================================================================

    /**
     * Crée la PieceView (canvas + contour de l'appartement en fond) et
     * l'insère dans la zone centrale d'AppView.
     *
     * La PieceView instancie elle-même son PieceControleur ; on le récupère
     * ensuite via un getter pour pouvoir lui déléguer les événements toolbar.
     *
     * Note : PieceView ne contient plus sa propre toolbar ni son propre
     * TreeView — elle se réduit à son StackPane canvas + label d'instructions.
     */
    @Override
    public void installer() {
        pieceView = new PieceView(stage, gestionnaire, appartement,
                                  appControleur.getAireImmeuble()); // ✅
        pieceControleur = pieceView.getControleur();
        appView.afficherPiece(pieceView);
        appView.setInstructions(
            "Vue pièce de « " + appartement + " » — dessinez les cloisons intérieures"
        );
    }

    /**
     * Annule toute construction en cours (mur à moitié tracé, etc.)
     * avant de quitter la vue pièce.
     */
    @Override
    public void desinstaller() {
        if (pieceControleur != null) {
            pieceControleur.annulerConstruction();
        }
    }

    // =========================================================================
    // BOUTONS TOOLBAR
    // =========================================================================

    /** Active le mode navigation (pan/zoom) sur le canvas de la pièce. */
    @Override
    public void onBtnNavigation() {
        if (pieceControleur != null) {
            pieceControleur.changerEtat(PieceControleur.ETAT_RIEN);
        }
    }

    /** Active le mode dessin de cloison dans la pièce. */
    @Override
    public void onBtnMur() {
        if (pieceControleur != null) {
            pieceControleur.changerEtat(PieceControleur.ETAT_MUR);
        }
    }

    /**
     * Active le mode insertion de porte.
     * L'utilisateur survole ensuite un mur et clique pour poser la porte.
     */
    @Override
    public void onBtnPorte() {
        if (pieceControleur != null) {
            pieceControleur.changerEtat(PieceControleur.ETAT_PORTE);
        }
    }

    /**
     * Active le mode insertion de fenêtre.
     * L'utilisateur survole ensuite un mur et clique pour poser la fenêtre.
     */
    @Override
    public void onBtnFenetre() {
        if (pieceControleur != null) {
            pieceControleur.changerEtat(PieceControleur.ETAT_FENETRE);
        }
    }

    /**
     * Bascule le panneau de sélection d'échelle.
     * Délégué à AppControleur car l'échelle est partagée entre tous les contextes.
     */
    @Override
    public void onBtnEchelle() {
        appControleur.onBtnEchelle();
    }

    // =========================================================================
    // TOOLBAR
    // =========================================================================

    @Override
    public List<String> getBoutonsVisibles() {
        return BOUTONS;
    }
}