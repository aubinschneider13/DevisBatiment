package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.controlleur.AppControleur;
import insa.aubin.devisbatiment.controlleur.NiveauControleur;
import insa.aubin.devisbatiment.view.AppView;
import javafx.scene.control.TreeItem;

import java.util.List;

/**
 * Contexte actif quand l'utilisateur travaille sur un étage (RDC, Niveau 1…).
 *
 * Boutons visibles : navigation, échelle, mur, appartement, ajouterNiveau.
 * Les boutons porte et fenêtre sont masqués — ils n'ont de sens que dans
 * un appartement (ContextePiece).
 *
 * Ce contexte délègue la logique de dessin à NiveauControleur. Il transmet
 * également les clics sur les items du TreeView à AppControleur pour que
 * celui-ci puisse déclencher la bascule vers ContextePiece quand l'utilisateur
 * sélectionne un appartement.
 */
public class ContexteNiveau implements Contexte {

    /** Identifiants des boutons affichés dans ce contexte (ordre = ordre toolbar). */
    private static final List<String> BOUTONS = List.of(
            "navigation", "echelle", "mur", "appartement", "ajouterNiveau"
    );

    // --- Dépendances ---
    private final NiveauControleur niveauControleur;
    private final AppView appView;
    private final AppControleur appControleur;

    /**
     * @param niveauControleur contrôleur de l'étage courant
     * @param appView          vue racine (pour afficher le canvas du niveau)
     * @param appControleur    chef d'orchestre (pour la bascule vers ContextePiece)
     */
    public ContexteNiveau(NiveauControleur niveauControleur,
                          AppView appView,
                          AppControleur appControleur) {
        this.niveauControleur = niveauControleur;
        this.appView          = appView;
        this.appControleur    = appControleur;
    }

    // =========================================================================
    // CYCLE DE VIE
    // =========================================================================

    /**
     * Affiche le canvas du niveau dans AppView et positionne les instructions
     * de démarrage.
     */
    @Override
    public void installer() {
        appView.afficherNiveau(niveauControleur.getVue());
        appView.setInstructions(
                "Sélectionnez un outil pour dessiner les murs ou créer un appartement"
        );
    }

    /**
     * Annule tout mur en cours de dessin avant de quitter le niveau.
     * Évite de laisser un segment « fantôme » si l'utilisateur change de
     * contexte en plein milieu d'un tracé.
     */
    @Override
    public void desinstaller() {
        niveauControleur.annulerMurEnCours();
    }

    // =========================================================================
    // BOUTONS TOOLBAR
    // =========================================================================

    /** Active le mode navigation (pan/zoom) sur le canvas du niveau. */
    @Override
    public void onBtnNavigation() {
        niveauControleur.activerModeNavigation();
    }

    /** Active le mode dessin de mur sur le canvas du niveau. */
    @Override
    public void onBtnMur() {
        niveauControleur.activerModeMur();
    }

    /** Active le mode création d'appartement par détection de zone fermée. */
    @Override
    public void onBtnAppartement() {
        niveauControleur.activerModeAppartement();
    }

    /**
     * Bascule le panneau de sélection d'échelle.
     * Délégué à AppControleur car l'échelle est partagée entre tous les contextes.
     */
    @Override
    public void onBtnEchelle() {
        appControleur.onBtnEchelle();
    }

    /**
     * Demande à AppControleur d'ajouter un nouveau niveau à l'immeuble.
     * C'est AppControleur qui crée le NiveauControleur et le TreeItem associé.
     */
    public void onBtnAjouterNiveau() {
        appControleur.onBtnAjouterNiveau();
    }

    // =========================================================================
    // TREE VIEW
    // =========================================================================

    /**
     * Transmet la sélection à AppControleur.
     * Si l'item correspond à un appartement connu de niveauControleur,
     * AppControleur déclenchera la bascule vers ContextePiece.
     *
     * @param item item sélectionné dans le TreeView (peut être null)
     */
    @Override
    public void onSelectionArbre(TreeItem<String> item) {
        if (item == null) return;
        appControleur.onSelectionArbre(item);
    }

    // =========================================================================
    // TOOLBAR
    // =========================================================================

    @Override
    public List<String> getBoutonsVisibles() {
        return BOUTONS;
    }

    // =========================================================================
    // GETTER
    // =========================================================================

    /** Expose le NiveauControleur pour qu'AppControleur puisse interroger la map items→appartements. */
    public NiveauControleur getNiveauControleur() {
        return niveauControleur;
    }
}