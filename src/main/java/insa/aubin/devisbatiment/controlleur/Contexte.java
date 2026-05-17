package insa.aubin.devisbatiment.controlleur;

import javafx.scene.control.TreeItem;
import java.util.List;

/**
 * Interface du pattern State (« Contexte ») utilisé par AppControleur.
 *
 * Chaque mode de l'application (aire, niveau, pièce) implémente cette
 * interface et définit ce que fait chaque bouton de la toolbar ainsi que
 * quels boutons sont visibles.
 *
 * AppControleur délègue tous les événements au contexte actif sans jamais
 * avoir à savoir lequel est installé — il appelle simplement la méthode
 * correspondante au bouton cliqué ou à l'item sélectionné.
 *
 * Cycle de vie d'un contexte :
 *   1. AppControleur instancie le contexte voulu.
 *   2. AppControleur appelle installer() → le contexte met en place son canvas
 *      et son état interne.
 *   3. AppControleur redirige les événements vers les méthodes on*().
 *   4. Quand on bascule vers un autre contexte, desinstaller() est appelé
 *      pour nettoyer proprement (listeners, état en cours, etc.).
 */
public interface Contexte {

    // =========================================================================
    // CYCLE DE VIE
    // =========================================================================

    /**
     * Appelé par AppControleur juste après avoir installé ce contexte.
     * Doit afficher le bon canvas dans AppView et initialiser l'état interne
     * (mode actif, instructions, etc.).
     */
    void installer();

    /**
     * Appelé par AppControleur juste avant de remplacer ce contexte par un
     * autre. Permet de nettoyer les listeners, d'annuler un dessin en cours,
     * de sauvegarder un état transitoire, etc.
     * Implémentation par défaut : no-op.
     */
    default void desinstaller() { }

    // =========================================================================
    // BOUTONS TOOLBAR — répondent chacun à un bouton de ToolBarView
    // Les contextes qui n'utilisent pas un bouton fournissent une no-op via
    // default, ce qui évite d'avoir des implémentations vides partout.
    // =========================================================================

    /** Bouton "Naviguer" — active le mode pan/zoom sur le canvas courant. */
    default void onBtnNavigation() { }

    /** Bouton "Mur" — active le mode dessin de mur. */
    default void onBtnMur() { }

    /**
     * Bouton "Appartement" — active le mode création d'appartement.
     * Pertinent uniquement en ContexteNiveau.
     */
    default void onBtnAppartement() { }
    
    default void onBtnPiece() { }
    
    /**
     * Bouton "Porte" — active le mode insertion de porte.
     * Pertinent uniquement en ContextePiece.
     */
    default void onBtnPorte() { }

    /**
     * Bouton "Fenêtre" — active le mode insertion de fenêtre.
     * Pertinent uniquement en ContextePiece.
     */
    default void onBtnFenetre() { }

    /** Bouton "Échelle" — bascule le panneau de choix d'échelle. */
    default void onBtnEchelle() { }

    /**
     * Bouton "Valider l'aire" — confirme l'emprise de l'immeuble.
     * Pertinent uniquement en ContexteAire.
     */
    default void onBtnValiderAire() { }

    // =========================================================================
    // TOOLBAR — visibilité des boutons
    // =========================================================================

    /**
     * Retourne la liste des identifiants de boutons à afficher dans la toolbar
     * pour ce contexte. Les boutons absents de la liste sont masqués.
     *
     * Identifiants possibles (définis comme constantes dans ToolBarView) :
     *   "navigation", "echelle", "mur", "appartement",
     *   "porte", "fenetre", "validerAire", "ajouterNiveau"
     *
     * @return liste non nulle des boutons visibles
     */
    List<String> getBoutonsVisibles();

    // =========================================================================
    // NAVIGATION DANS LE TREE VIEW
    // =========================================================================

    /**
     * Appelé par AppControleur quand l'utilisateur sélectionne un item dans
     * le TreeView. Chaque contexte décide s'il doit réagir (ex. : un clic sur
     * un appartement en ContexteNiveau déclenchera la bascule vers ContextePiece
     * via AppControleur).
     *
     * @param item l'item sélectionné dans le TreeView (peut être null)
     */
    default void onSelectionArbre(TreeItem<String> item) { }
}