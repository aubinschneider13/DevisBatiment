package insa.aubin.devisbatiment.view;

import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Panneau de navigation gauche commun à tous les contextes.
 *
 * Contient un TreeView unique dont la racine représente l'immeuble.
 * L'arbre est peuplé progressivement par AppControleur au fil des actions
 * de l'utilisateur (ajout de niveaux, d'appartements…).
 *
 * NavigateurView est purement déclarative : elle n'écoute aucun événement
 * elle-même. C'est AppControleur qui branche le listener de sélection sur
 * getTreeView().getSelectionModel().selectedItemProperty().
 *
 * Structure de l'arbre attendue :
 *   Immeuble : ( NomImmeuble )
 *   ├── Aire de l'immeuble
 *   └── Niveaux
 *       ├── RDC
 *       │   ├── Appartement A1
 *       │   └── Appartement A2
 *       └── Niveau 1
 *           └── Appartement B1
 */
public class NavigateurView extends VBox {

    private final TreeView<String> treeView;
    private final TreeItem<String> rootItem;
    private final TreeItem<String> itemAire;
    private final TreeItem<String> itemNiveaux;

    public NavigateurView() {
        // --- Racine de l'arbre ---
        rootItem = new TreeItem<>("Immeuble (en attente…)");
        rootItem.setExpanded(true);

        // --- Items fixes créés une seule fois ---
        itemAire    = new TreeItem<>("Aire de l'immeuble");
        itemNiveaux = new TreeItem<>("Niveaux");
        itemNiveaux.setExpanded(true);

        rootItem.getChildren().addAll(itemAire, itemNiveaux);

        // --- TreeView ---
        treeView = new TreeView<>(rootItem);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // --- Assemblage ---
        Label titre = new Label(" Navigateur");
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2c3e50;");
        getChildren().addAll(titre, treeView);
    }

    // =========================================================================
    // API PUBLIQUE — appelée par AppControleur
    // =========================================================================

    /**
     * Met à jour le libellé de la racine avec le nom de l'immeuble.
     * Appelé par AppControleur après que l'utilisateur a saisi le nom.
     *
     * @param nomImmeuble nom saisi par l'utilisateur
     */
    public void setNomImmeuble(String nomImmeuble) {
        rootItem.setValue("Immeuble : ( " + nomImmeuble + " )");
    }

    /**
     * Ajoute un item de niveau sous le nœud "Niveaux" et l'expand.
     * Retourne le TreeItem créé pour que AppControleur puisse le conserver
     * et l'associer au NiveauControleur correspondant.
     *
     * @param nomNiveau libellé du niveau (ex. "RDC", "Niveau 1")
     * @return le TreeItem créé
     */
    public TreeItem<String> ajouterItemNiveau(String nomNiveau) {
        TreeItem<String> item = new TreeItem<>(nomNiveau);
        itemNiveaux.getChildren().add(item);
        itemNiveaux.setExpanded(true);
        return item;
    }

    /**
     * Ajoute un item d'appartement sous le nœud du niveau indiqué et l'expand.
     * Retourne le TreeItem créé pour que AppControleur puisse l'associer
     * à l'Appartement correspondant.
     *
     * @param itemNiveau nœud parent (le niveau dans lequel l'appartement a été créé)
     * @param nomAppartement libellé de l'appartement
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
     * Sélectionne programmatiquement un item dans le TreeView.
     * Utilisé par AppControleur après avoir créé un niveau pour y basculer
     * automatiquement.
     *
     * @param item item à sélectionner
     */
    public void selectionner(TreeItem<String> item) {
        treeView.getSelectionModel().select(item);
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public TreeView<String> getTreeView()       { return treeView;     }
    public TreeItem<String> getRootItem()       { return rootItem;     }
    public TreeItem<String> getItemAire()       { return itemAire;     }
    public TreeItem<String> getItemNiveaux()    { return itemNiveaux;  }
}