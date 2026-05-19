package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.modele.Appartement;
import insa.aubin.devisbatiment.modele.Niveau;
import insa.aubin.devisbatiment.modele.Piece;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Panneau de navigation gauche commun à tous les contextes.
 *
 * Structure :
 *   ┌──────────────────────┐
 *   │  Navigateur (titre)  │
 *   ├──────────────────────┤
 *   │  TreeView            │  ← s'étire verticalement
 *   ├──────────────────────┤
 *   │  ProprietesView      │  ← panneau de propriétés contextuel
 *   └──────────────────────┘
 *
 * L'arbre est peuplé progressivement par AppControleur.
 * NavigateurView est purement déclarative : AppControleur branche tous
 * les listeners (sélection TreeView, mise à jour ProprietesView).
 */
public class NavigateurView extends VBox {

    private final TreeView<String> treeView;
    private final TreeItem<String> rootItem;
    private final TreeItem<String> itemAire;
    private final TreeItem<String> itemNiveaux;

    // Nouveau panneau de propriétés — affiché sous le TreeView
    private final ProprietesView proprietesView;

    public NavigateurView() {
        // --- Racine de l'arbre ---
        rootItem = new TreeItem<>("Immeuble (en attente…)");
        rootItem.setExpanded(true);

        // --- Items fixes créés une seule fois ---
        itemAire    = new TreeItem<>("Aire du terrain l'immeuble");
        itemNiveaux = new TreeItem<>("Niveaux");
        itemNiveaux.setExpanded(true);

        rootItem.getChildren().addAll(itemAire, itemNiveaux);

        // --- TreeView ---
        treeView = new TreeView<>(rootItem);
        // Le TreeView prend tout l'espace vertical disponible au-dessus du panneau
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // --- Panneau de propriétés ---
        proprietesView = new ProprietesView();
        // Hauteur fixe : assez grande pour afficher les infos, sans écraser le TreeView
        proprietesView.setPrefHeight(260);
        proprietesView.setMinHeight(180);
        proprietesView.setMaxHeight(320);

        // --- Assemblage ---
        Label titre = new Label(" Navigateur");
        titre.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2c3e50;"
        );
        getChildren().addAll(titre, treeView, proprietesView);
    }

    // =========================================================================
    // API PUBLIQUE — peuplement de l'arbre (inchangée)
    // =========================================================================

    /** Met à jour le libellé de la racine avec le nom du bâtiment. */
    public void setNomImmeuble(String nomImmeuble) {
        rootItem.setValue("Immeuble : ( " + nomImmeuble + " )");
    }

    /**
     * Ajoute un item de niveau sous le nœud "Niveaux".
     * @return le TreeItem créé (AppControleur le conserve)
     */
    public TreeItem<String> ajouterItemNiveau(String nomNiveau) {
        TreeItem<String> item = new TreeItem<>(nomNiveau);
        itemNiveaux.getChildren().add(item);
        itemNiveaux.setExpanded(true);
        return item;
    }

    /**
     * Ajoute un item d'appartement sous le nœud du niveau indiqué.
     * @return le TreeItem créé
     */
    public TreeItem<String> ajouterItemAppartement(TreeItem<String> itemNiveau,
                                                   String nomAppartement) {
        TreeItem<String> item = new TreeItem<>(nomAppartement);
        itemNiveau.getChildren().add(item);
        itemNiveau.setExpanded(true);
        return item;
    }

    /**
     * Ajoute un item de pièce sous le nœud de l'appartement indiqué.
     * @return le TreeItem créé
     */
    public TreeItem<String> ajouterItemPiece(TreeItem<String> itemAppartement,
                                             String nomPiece) {
        TreeItem<String> item = new TreeItem<>(nomPiece);
        itemAppartement.getChildren().add(item);
        itemAppartement.setExpanded(true);
        return item;
    }

    /** Sélectionne programmatiquement un item dans le TreeView. */
    public void selectionner(TreeItem<String> item) {
        treeView.getSelectionModel().select(item);
    }

    // =========================================================================
    // API PUBLIQUE — mise à jour du panneau de propriétés
    // Appelée par AppControleur dans le listener de sélection du TreeView.
    // =========================================================================

    /**
     * Affiche les propriétés d'un niveau dans le panneau bas.
     * @param niveau le Niveau correspondant à l'item sélectionné
     */
    public void afficherProprietesNiveau(Niveau niveau) {
        proprietesView.afficherNiveau(niveau);
    }

    /**
     * Affiche les propriétés d'un appartement dans le panneau bas.
     * @param appartement l'Appartement correspondant à l'item sélectionné
     */
    public void afficherProprietesAppartement(Appartement appartement) {
        proprietesView.afficherAppartement(appartement);
    }

    /**
     * Affiche les propriétés d'une pièce dans le panneau bas.
     * @param piece la Pièce correspondant à l'item sélectionné
     */
    public void afficherProprietesPiece(Piece piece) {
        proprietesView.afficherPiece(piece);
    }

    /**
     * Vide le panneau de propriétés (ex : clic sur "Aire" ou "Niveaux").
     */
    public void effacerProprietes() {
        proprietesView.effacer();
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public TreeView<String> getTreeView()       { return treeView;        }
    public TreeItem<String> getRootItem()       { return rootItem;        }
    public TreeItem<String> getItemAire()       { return itemAire;        }
    public TreeItem<String> getItemNiveaux()    { return itemNiveaux;     }
    public ProprietesView   getProprietesView() { return proprietesView;  }
}