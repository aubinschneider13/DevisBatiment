package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Fenêtre racine et conteneur maître unique de l'interface graphique de l'application.
 * <p>
 * Cette classe constitue le pivot de la couche <b>Vue (IHM)</b> au sein de l'architecture MVC.
 * Étendant un {@link BorderPane} JavaFX, elle définit la structure structurelle fixe et immuable
 * de l'application, indépendamment du contexte fonctionnel actif :
 * <ul>
 * <li><b>TOP :</b> Un {@link TabPane} regroupant les barres d'outils contextuelles ({@link ToolBarView} pour la CAO et {@link ToolBarDevisView} pour le second œuvre).</li>
 * <li><b>LEFT :</b> Un {@link SplitPane} ancrant le {@link NavigateurView} (l'arborescence structurelle du projet).</li>
 * <li><b>CENTER :</b> Une zone de dessin dynamique ({@link StackPane}) accueillant par permutation les différents calques et canvas vectoriels.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Philosophie de Conception :</b>
 * Fidèle aux principes du pattern de réorientation (Pattern State), {@code AppView} est totalement exempte
 * de logique métier et décisionnelle. Elle n'intercepte pas le flux ; elle se contente d'exposer une API
 * publique d'affichage (méthodes {@code afficher...}) pilotée de manière descendante par l'{@code AppControleur}.
 * Les calques utilitaires permanents (échelle, consignes, filtre de validation) y sont empilés de manière
 * à rester superposés lors des changements de calques intérieurs.
 * </p>
 * * @see ToolBarView
 * @see NavigateurView
 * @see DessinCanvas
 * @see BorderPane
 */
public class AppView extends BorderPane {

    // --- Sous-vues fixes ---
    private final ToolBarView toolBarView;
    private final ToolBarDevisView toolBarDevisView;
    private final NavigateurView navigateurView;
    private final EchelleVue echelleVue;

    // --- Zone centrale ---
    private final StackPane zoneDessin;

    // --- Canvas de l'aire (créé une seule fois, réaffiché si on revient à l'aire) ---
    private final DessinCanvas canvasAire;

    // --- Overlays permanents ---
    private final StackPane voileValidation; // voile cadenas sur l'aire validée
    private final Label labelInstructions;

    /** Mémorise si le voile a été activé pour le restaurer quand on revient sur l'aire. */
    private boolean voileActif = false;

    public AppView() {

        // =====================================================================
        // 1. TOOLBAR (TOP)
        // =====================================================================
        toolBarView = new ToolBarView();
        Tab tabConstruction = new Tab("Construction");
        tabConstruction.setClosable(false);
        tabConstruction.setContent(toolBarView);

        toolBarDevisView = new ToolBarDevisView();
        Tab tabDevis = new Tab("Matériaux et Devis");
        tabDevis.setClosable(false);
        tabDevis.setContent(toolBarDevisView);

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(tabConstruction, tabDevis);
        tabPane.setStyle("-fx-border-color: #d1d1d1; -fx-border-width: 0 0 1 0;");
        this.setTop(tabPane);

        // =====================================================================
        // 2. ZONE CENTRALE (CENTER)
        // =====================================================================

        // --- Canvas de l'aire ---
        zoneDessin = new StackPane();
        zoneDessin.setStyle("-fx-background-color: #fffefe;");

        canvasAire = new DessinCanvas();
        canvasAire.widthProperty().bind(zoneDessin.widthProperty());
        canvasAire.heightProperty().bind(zoneDessin.heightProperty());

        // --- Voile cadenas (affiché une fois l'aire validée) ---
        voileValidation = creerVoileValidation();
        voileValidation.setVisible(false);

        // --- Label d'instructions (bas de la zone centrale) ---
        labelInstructions = new Label("Cliquez pour définir le premier coin de l'immeuble");
        labelInstructions.setStyle(
            "-fx-background-color: rgba(240,240,240,0.9);" +
            "-fx-padding: 6 12 6 12;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-text-fill: #2c3e50;" +
            "-fx-font-size: 13px;"
        );
        StackPane.setAlignment(labelInstructions, Pos.BOTTOM_CENTER);
        StackPane.setMargin(labelInstructions, new Insets(0, 0, 15, 0));

        // --- EchelleVue (overlay haut-gauche) ---
        echelleVue = new EchelleVue();
        StackPane.setAlignment(echelleVue, Pos.TOP_LEFT);
        StackPane.setMargin(echelleVue, new Insets(10));

        // Empilement initial : canvas aire + overlays
        zoneDessin.getChildren().addAll(
            canvasAire, voileValidation, echelleVue, labelInstructions
        );

        // --- Navigateur gauche ---
        navigateurView = new NavigateurView();
        navigateurView.setPrefWidth(250);

        // --- SplitPane ---
        SplitPane splitPane = new SplitPane(navigateurView, zoneDessin);
        splitPane.setDividerPositions(0.2);
        this.setCenter(splitPane);

        // =====================================================================
        // 3. FOCUS & RACCOURCIS (gérés par AppControleur via setOnKeyPressed)
        // =====================================================================
        this.setFocusTraversable(true);
    }

    // =========================================================================
    // AFFICHAGE DES CANVAS / VUES
    // =========================================================================

    /**
     * Affiche le canvas de l'aire de l'immeuble et restaure le voile cadenas
     * si l'aire a déjà été validée lors d'une session précédente dans
     * cette instance.
     */
    public void afficherCanvasAire() {
        remplacerCanvasCentral(canvasAire);
        voileValidation.setVisible(voileActif);
        labelInstructions.setVisible(true);
    }

    /**
     * Affiche le canvas d'un niveau dans la zone centrale.
     * Le voile cadenas est masqué (il ne concerne que l'aire).
     *
     * @param niveauView vue du niveau à afficher
     */
    public void afficherNiveau(NiveauView niveauView) {
        // Retire canvas et éventuelles vues précédentes, conserve les overlays
        zoneDessin.getChildren().removeIf(
            n -> n instanceof DessinCanvas || n instanceof NiveauView || n instanceof PieceView
        );

        // Bind la taille du niveau à la zone centrale
        niveauView.prefWidthProperty().bind(zoneDessin.widthProperty());
        niveauView.prefHeightProperty().bind(zoneDessin.heightProperty());
        niveauView.getCanvas().widthProperty().bind(zoneDessin.widthProperty());
        niveauView.getCanvas().heightProperty().bind(zoneDessin.heightProperty());

        // Insère sous les overlays
        zoneDessin.getChildren().add(0, niveauView);
        voileValidation.setVisible(false);
        labelInstructions.setVisible(false);
    }

    /**
     * Affiche la vue d'une pièce dans la zone centrale.
     * PieceView est désormais un simple StackPane canvas — elle ne contient
     * plus sa propre toolbar ni son propre TreeView.
     *
     * @param pieceView vue de la pièce à afficher
     */
    public void afficherPiece(PieceView pieceView) {
        zoneDessin.getChildren().removeIf(
            n -> n instanceof DessinCanvas || n instanceof NiveauView || n instanceof PieceView
        );

        pieceView.prefWidthProperty().bind(zoneDessin.widthProperty());
        pieceView.prefHeightProperty().bind(zoneDessin.heightProperty());

        zoneDessin.getChildren().add(0, pieceView);
        voileValidation.setVisible(false);
        labelInstructions.setVisible(false);
    }

    // =========================================================================
    // VOILE CADENAS
    // =========================================================================

    /**
     * Active le voile cadenas sur le canvas de l'aire et mémorise cet état
     * pour le restaurer si l'utilisateur revient sur l'aire depuis un niveau.
     */
    public void activerVoile() {
        voileActif = true;
        voileValidation.setVisible(true);
    }

    // =========================================================================
    // INSTRUCTIONS
    // =========================================================================

    /**
     * Met à jour le texte du label d'instructions en bas de la zone centrale.
     * Appelé par AppControleur à chaque changement de contexte ou d'outil.
     *
     * @param texte message à afficher
     */
    public void setInstructions(String texte) {
        labelInstructions.setText(texte);

        // Synchronisation automatique avec la sous-vue active si présente
        if (!zoneDessin.getChildren().isEmpty()) {
            var activeView = zoneDessin.getChildren().get(0);
            if (activeView instanceof PieceView pv) {
                pv.setInstructions(texte);
            } else if (activeView instanceof NiveauView nv) {
                nv.setInstructions(texte);
            }
        }
    }

    // =========================================================================
    // UTILITAIRE PRIVÉ
    // =========================================================================

    /**
     * Remplace le contenu principal de zoneDessin par un nouveau DessinCanvas,
     * en conservant les overlays (voile, echelle, label).
     *
     * @param canvas nouveau canvas à afficher
     */
    private void remplacerCanvasCentral(DessinCanvas canvas) {
        zoneDessin.getChildren().removeIf(
            n -> n instanceof DessinCanvas || n instanceof NiveauView || n instanceof PieceView
        );
        canvas.widthProperty().bind(zoneDessin.widthProperty());
        canvas.heightProperty().bind(zoneDessin.heightProperty());
        zoneDessin.getChildren().add(0, canvas);
    }

    /**
     * Crée le voile gris semi-transparent avec un cadenas centré.
     * Identique à l'implémentation d'origine dans ImmeubleView.
     */
    private StackPane creerVoileValidation() {
        Rectangle voile = new Rectangle();
        voile.setFill(Color.web("#808080", 0.35));
        voile.widthProperty().bind(zoneDessin.widthProperty());
        voile.heightProperty().bind(zoneDessin.heightProperty());

        Text cadenas = new Text("🔒");
        cadenas.setFont(Font.font(48));
        cadenas.setFill(Color.web("#2c3e50", 0.8));

        StackPane voilePane = new StackPane(voile, cadenas);
        StackPane.setAlignment(cadenas, Pos.CENTER);
        // Le voile ne capte pas les clics — il est purement visuel
        voilePane.setMouseTransparent(true);
        return voilePane;
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public ToolBarView getToolBarView()         { return toolBarView;     }
    public ToolBarDevisView getToolBarDevisView() { return toolBarDevisView; }
    public NavigateurView getNavigateurView()   { return navigateurView;  }
    public DessinCanvas getCanvasAire()         { return canvasAire;      }
    public EchelleVue getEchelleVue()           { return echelleVue;      }
}