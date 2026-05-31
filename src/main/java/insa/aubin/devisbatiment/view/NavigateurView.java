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
 * Panneau latéral de navigation hiérarchique et d'inspection des propriétés du projet.
 * <p>
 * Cette classe constitue un composant permanent de la couche <b>Vue (IHM)</b> au sein de l'architecture MVC.
 * Ancrée de manière fixe dans la zone gauche (LEFT) de la fenêtre principale {@link AppView}, elle hérite
 * d'un {@link VBox} JavaFX et s'organise selon la topologie verticale suivante :
 * <ol>
 * <li>Un bandeau d'en-tête textuel servant de titre de section.</li>
 * <li>Un arbre de navigation vectoriel ({@link TreeView}) affichant la topologie imbriquée de l'immeuble.</li>
 * <li>Un panneau inférieur d'inspection contextuel ({@link ProprietesView}) modifiant ses champs selon l'élément sélectionné.</li>
 * </ol>
 * </p>
 * <p>
 * <b>Philosophie de Conception et Découplage (MVC) :</b>
 * {@code NavigateurView} adopte une logique purement déclarative et passive. Elle ne contient aucune méthode
 * décisionnelle, aucun écouteur interne (listener) et ne manipule pas le modèle directement. L'arborescence
 * est peuplée pas-à-pas et de manière dynamique par l'{@code AppControleur}. C'est également ce dernier qui
 * capte les événements de sélection sur les nœuds de l'arbre pour mettre à jour les propriétés ou intervertir
 * les canvas centraux.
 * </p>
 * * @see ProprietesView
 * @see TreeView
 * @see TreeItem
 * @see VBox
 */
public class NavigateurView extends VBox {

    /** Le composant d'affichage hiérarchique JavaFX. */
    private final TreeView<String> treeView;
    /** Le nœud racine virtuel de l'arbre, représentant le bâtiment maître (Maison ou Immeuble). */
    private final TreeItem<String> rootItem;
    /** Le nœud d'ancrage fixe dédié à l'accès et à l'affichage de l'emprise au sol du terrain. */
    private final TreeItem<String> itemAire;
    /** Le nœud d'ancrage fixe servant de conteneur parent à l'ensemble des étages (niveaux). */
    private final TreeItem<String> itemNiveaux;

    /** Le panneau conteneur inférieur gérant l'affichage des métadonnées physiques et financières (devis). */
    private final ProprietesView proprietesView;

    /**
     * <b>Constructeur unique du navigateur latéral.</b>
     * <p>
     * Initialise la structure racine de l'arborescence, instancie les branches permanentes non-volatiles,
     * configure les politiques de redimensionnement vertical ({@link Priority#ALWAYS} appliqué sur le {@code TreeView}
     * pour qu'il occupe l'espace résiduel), calibre les contraintes de hauteur du panneau de propriétés
     * et injecte les styles visuels CSS.
     * </p>
     */
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
        // Force l'arbre à s'étirer verticalement pour repousser le panneau de propriétés vers le bas
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // --- Panneau de propriétés ---
        proprietesView = new ProprietesView();
        // Calibrage des dimensions limites pour préserver l'équilibre ergonomique de l'IHM
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
    // API PUBLIQUE — Peuplement et mutations de l'arbre
    // =========================================================================

    /**
     * Modifie le libellé textuel du nœud racine pour refléter la nature et l'identification du projet.
     * * @param typeBatiment Le type de structure instanciée (ex : "IMMEUBLE" ou "MAISON").
     * @param nomBatiment  Le nom personnalisé attribué à l'ouvrage.
     */
    public void setNomBatiment(String typeBatiment, String nomBatiment) {
        rootItem.setValue(typeBatiment + " : ( " + nomBatiment + " )");
    }

    /**
     * Insère un nouveau nœud d'étage sous la branche parente "Niveaux".
     * * @param nomNiveau Le libellé du niveau à afficher (ex : "RDC", "Niveau 1").
     * @return L'instance de {@link TreeItem} générée, permettant au contrôleur de mémoriser sa référence.
     */
    public TreeItem<String> ajouterItemNiveau(String nomNiveau) {
        TreeItem<String> item = new TreeItem<>(nomNiveau);
        itemNiveaux.getChildren().add(item);
        itemNiveaux.setExpanded(true);
        return item;
    }

    /**
     * Insère un nouveau nœud d'appartement sous la branche du niveau spécifié.
     * * @param itemNiveau     Le nœud d'étage parent devant accueillir le lot.
     * @param nomAppartement Le nom ou numéro du lot privatif à inscrire.
     * @return L'instance de {@link TreeItem} générée pour l'appartement.
     */
    public TreeItem<String> ajouterItemAppartement(TreeItem<String> itemNiveau,
                                                   String nomAppartement) {
        TreeItem<String> item = new TreeItem<>(nomAppartement);
        itemNiveau.getChildren().add(item);
        itemNiveau.setExpanded(true);
        return item;
    }

    /**
     * Insère un nouveau nœud de circulation commune (couloir) sous la branche du niveau spécifié.
     * * @param itemNiveau Le nœud d'étage parent devant accueillir le couloir.
     * @param nomCouloir Le libellé d'identification de la zone de circulation.
     * @return L'instance de {@link TreeItem} générée pour le couloir.
     */
    public TreeItem<String> ajouterItemCouloir(TreeItem<String> itemNiveau,
                                               String nomCouloir) {
        TreeItem<String> item = new TreeItem<>(nomCouloir);
        itemNiveau.getChildren().add(item);
        itemNiveau.setExpanded(true);
        return item;
    }

    /**
     * Insère un nœud de pièce fermée sous la branche de l'appartement parent associé.
     * * @param itemAppartement Le nœud d'appartement parent devant accueillir la pièce.
     * @param nomPiece        Le nom ou l'usage initial attribué à l'espace (ex : "Salon", "Cuisine").
     * @return L'instance de {@link TreeItem} générée pour la pièce.
     */
    public TreeItem<String> ajouterItemPiece(TreeItem<String> itemAppartement,
                                             String nomPiece) {
        TreeItem<String> item = new TreeItem<>(nomPiece);
        itemAppartement.getChildren().add(item);
        itemAppartement.setExpanded(true);
        return item;
    }

    /**
     * Force la sélection graphique d'un nœud spécifique au sein du composant d'affichage.
     * <p>
     * Cette méthode permet de synchroniser de façon programmatique l'état de l'arbre à la suite
     * d'une action déclenchée depuis le canevas vectoriel (clic sur une forme).
     * </p>
     * * @param item Le nœud de type {@link TreeItem} à mettre en surbrillance.
     */
    public void selectionner(TreeItem<String> item) {
        treeView.getSelectionModel().select(item);
    }

    // =========================================================================
    // API PUBLIQUE — Mise à jour du panneau de propriétés contextuel
    // =========================================================================

    /**
     * Transmet un objet d'étage au sous-panneau pour en extraire et afficher les caractéristiques techniques.
     * * @param niveau L'entité {@link Niveau} inspectée.
     */
    public void afficherProprietesNiveau(Niveau niveau) {
        proprietesView.afficherNiveau(niveau);
    }

    /**
     * Transmet un objet d'appartement au sous-panneau pour en extraire et afficher le bilan de surfaces et de coûts.
     * * @param appartement L'entité {@link Appartement} inspectée.
     */
    public void afficherProprietesAppartement(Appartement appartement) {
        proprietesView.afficherAppartement(appartement);
    }

    /**
     * Transmet une pièce au sous-panneau afin d'éditer ou de visualiser ses finitions de second œuvre.
     * * @param piece L'entité {@link Piece} inspectée.
     */
    public void afficherProprietesPiece(Piece piece) {
        proprietesView.afficherPiece(piece);
    }

    /**
     * Réinitialise et purge l'ensemble des champs du panneau inférieur d'inspection.
     * <p>
     * Invoqué par le contrôleur principal lorsque la sélection se déplace vers un nœud abstrait
     * ou non-quantifiable (ex : "Aire du terrain" ou "Niveaux").
     * </p>
     */
    public void effacerProprietes() {
        proprietesView.effacer();
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    /**
     * Retourne le composant d'affichage hiérarchique.
     * @return L'instance de {@link TreeView}.
     */
    public TreeView<String> getTreeView()       { return treeView;        }

    /**
     * Retourne le nœud racine virtuel de l'arbre.
     * @return Le {@link TreeItem} racine.
     */
    public TreeItem<String> getRootItem()       { return rootItem;        }

    /**
     * Retourne le nœud fixe associé à l'aire de l'immeuble.
     * @return Le {@link TreeItem} de l'aire.
     */
    public TreeItem<String> getItemAire()       { return itemAire;        }

    /**
     * Retourne le nœud fixe conteneur des étages.
     * @return Le {@link TreeItem} générique des niveaux.
     */
    public TreeItem<String> getItemNiveaux()    { return itemNiveaux;     }

    /**
     * Retourne le panneau d'inspection des propriétés sous-jacent.
     * @return L'instance de {@link ProprietesView}.
     */
    public ProprietesView   getProprietesView() { return proprietesView;  }
}