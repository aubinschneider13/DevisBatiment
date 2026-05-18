package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.controlleur.AppControleur;
import insa.aubin.devisbatiment.controlleur.PieceControleur;
import insa.aubin.devisbatiment.modele.Appartement;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.modele.SurfaceAvecRevetement;
import insa.aubin.devisbatiment.view.AppView;
import insa.aubin.devisbatiment.view.PieceView;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TreeItem;

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
            "navigation", "echelle", "mur", "piece","porte","fenetre"
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
    private final TreeItem<String> itemAppartement;
    private boolean callbackBranche = false;

    // --- Variables pour le Mode Sélection (Revêtements) ---
    //private boolean modeSelectionActif = false;
    //private final List<SurfaceAvecRevetement> surfacesSelectionnees = new ArrayList<>();

    /**
     * @param appartement  appartement dont on va aménager l'intérieur
     * @param appView      vue racine (pour insérer le canvas de la pièce)
     * @param appControleur chef d'orchestre
     * @param stage        fenêtre principale (transmis à PieceControleur)
     * @param gestionnaire service de sauvegarde (transmis à PieceControleur)
     */
    public ContextePiece(Appartement appartement, AppView appView,
                     AppControleur appControleur, Stage stage,
                     GestionnaireSauvegarde gestionnaire,
                     TreeItem<String> itemAppartement) {
        
        this.appartement    = appartement;
        this.appView        = appView;
        this.appControleur  = appControleur;
        this.stage          = stage;
        this.gestionnaire   = gestionnaire;
        this.itemAppartement = itemAppartement;
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
        if (pieceView == null) {
            pieceView = new PieceView(stage, gestionnaire, appartement,
                                      appControleur.getAireImmeuble());
            pieceControleur = pieceView.getControleur();
        }
        // ✅ Ne brancher le callback qu'une seule fois
        if (!callbackBranche) {
            pieceControleur.setOnPieceCree(piece -> {
                TreeItem<String> itemPiece = appView.getNavigateurView()
                    .ajouterItemPiece(itemAppartement, piece.toString());
                appControleur.enregistrerPiece(itemPiece, piece, itemAppartement);
                return itemPiece;
            });
            callbackBranche = true;
        }
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
    
    @Override
    public void onBtnPiece() {
        if (pieceControleur != null) {
            pieceControleur.activerModePiece();
        }
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

    // =========================================================================
    // TOOLBAR
    // =========================================================================

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

    public Appartement getAppartement() {
        return appartement;
    }
}