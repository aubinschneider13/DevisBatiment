package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.controlleur.AppControleur;
import insa.aubin.devisbatiment.controlleur.ImmeubleControleur;
import insa.aubin.devisbatiment.view.AppView;
import javafx.scene.input.KeyEvent;

import java.util.List;

/**
 * Contexte actif quand l'utilisateur est en train de définir (ou de consulter)
 * l'emprise rectangulaire de l'immeuble.
 *
 * Boutons visibles : navigation, échelle, validerAire.
 * Tous les autres boutons (mur, appartement, porte, fenêtre…) sont masqués
 * car ils n'ont pas de sens à cette étape.
 *
 * Ce contexte délègue la logique de dessin à ImmeubleControleur (clics,
 * glisser-déposer des côtés, annulation). Il ne fait que brancher/débrancher
 * le bon canvas et mettre à jour la toolbar.
 */
public class ContexteAire implements Contexte {

    /** Identifiants des boutons affichés dans ce contexte (ordre = ordre toolbar). */
    private static final List<String> BOUTONS = List.of(
            "navigation", "echelle", "validerAire"
    );

    // --- Dépendances ---
    private final ImmeubleControleur immeubleControleur;
    private final AppView appView;
    private final AppControleur appControleur;

    /**
     * @param immeubleControleur contrôleur qui gère le dessin de l'aire
     * @param appView            vue racine (pour afficher le bon canvas)
     * @param appControleur      chef d'orchestre (pour déclencher la validation)
     */
    public ContexteAire(ImmeubleControleur immeubleControleur,
                        AppView appView,
                        AppControleur appControleur) {
        this.immeubleControleur = immeubleControleur;
        this.appView            = appView;
        this.appControleur      = appControleur;
    }

    // =========================================================================
    // CYCLE DE VIE
    // =========================================================================

    /**
     * Installe le canvas de l'aire dans AppView et restaure le voile cadenas
     * si l'aire a déjà été validée lors d'une session précédente.
     */
    @Override
    public void installer() {
        appView.afficherCanvasAire();
        appView.setInstructions("Cliquez pour définir le premier coin de l'immeuble");
    }

    /**
     * Rien à nettoyer côté aire : les listeners souris sont gérés directement
     * par ImmeubleControleur sur son canvas et restent actifs.
     */
    @Override
    public void desinstaller() { }

    // =========================================================================
    // BOUTONS TOOLBAR
    // =========================================================================

    /**
     * Active le pan/zoom sur le canvas de l'aire.
     * ImmeubleControleur expose cette logique via btnNavigation().
     */
    @Override
    public void onBtnNavigation() {
        immeubleControleur.btnNavigation(null);
    }

    /**
     * Bascule le panneau de sélection d'échelle.
     * ImmeubleControleur expose cette logique via btnEchelle().
     */
    @Override
    public void onBtnEchelle() {
        immeubleControleur.btnEchelle(null);
    }

    /**
     * Valide l'emprise de l'immeuble puis demande à AppControleur de basculer
     * vers le contexte suivant (ajout de niveaux).
     * ImmeubleControleur gère la validation métier ; AppControleur gère la
     * bascule de contexte.
     */
    @Override
    public void onBtnValiderAire() {
        immeubleControleur.btnValiderAire(null);
        // ✅ On ne bascule que si la validation a réellement eu lieu
        if (immeubleControleur.isAireValidee()) {
            appControleur.onAireValidee();
        }
    }

    // =========================================================================
    // TOOLBAR
    // =========================================================================

    @Override
    public List<String> getBoutonsVisibles() {
        if (immeubleControleur.isAireValidee()) {
            if (appControleur.estMaison()) {
                return List.of("navigation", "echelle");
            }
            return List.of("navigation", "echelle", "ajouterNiveau");
        }
        return List.of("navigation", "echelle", "validerAire");
    }

    @Override
    public void gererToucheClavier(KeyEvent e) {
        if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
            if (immeubleControleur != null) {
                immeubleControleur.annulerAire();
            }
        }
    }
}
