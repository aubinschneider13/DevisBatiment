package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.controlleur.ImmeubleControleur;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ImmeubleView extends BorderPane {
    private String cheminRetour = "/images/fleche_retour_icone.png";
    private String cheminMain = "/images/main_icone.png";
    private String cheminEchelle = "/images/echelle_icone.png";
    private String cheminMur = "/images/mur_icone.png";
    private String cheminAppartement = "/images/appartement_icone.png";

    private ImmeubleControleur controleur;
    private Button btnAjouterNiveau;
    private Button btnValiderAire;
    private Button btnMur;
    private Button btnAppartement;
    private TreeView<String> treeView;
    private TreeItem<String> rootItem;
    private EchelleVue echelleVue;
    private Label labelInstructions;
    private boolean voileActif = false; // Mémorise si le voile a été activé (aire validée) pour le restaurer au retour
    private StackPane zoneDessin; // Zone centrale : StackPane contenant les différents canvas
    private DessinCanvas canvasAire; // Canvas de l'aire de l'immeuble
    private StackPane voileValidation; // Voile + cadenas affiché une fois l'aire validée

    public ImmeubleView(Stage stage, GestionnaireSauvegarde gestionnaire) {

        // --- 1. BARRE D'OUTILS (TOP) ---
        TabPane tabPaneImmeuble = new TabPane();
        Tab tabConstruction = new Tab("Construction");
        tabConstruction.setClosable(false);

        HBox toolBar = new HBox();
        toolBar.setSpacing(10);
        toolBar.setPadding(new Insets(10));
        toolBar.setAlignment(Pos.CENTER_LEFT);

        // Bouton Navigation
        Image imgMain = new Image(getClass().getResource(cheminMain).toExternalForm());
        ImageView iconeMain = new ImageView(imgMain);
        iconeMain.setFitHeight(30);
        iconeMain.setFitWidth(30);
        iconeMain.setPreserveRatio(true);

        Button navigationButton = new Button("Naviguer");
        navigationButton.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        navigationButton.setPrefSize(80, 60);
        navigationButton.setGraphic(iconeMain);
        navigationButton.setContentDisplay(ContentDisplay.TOP);

        // Bouton Échelle
        Image imgEchelle = new Image(getClass().getResource(cheminEchelle).toExternalForm());
        ImageView iconeEchelle = new ImageView(imgEchelle);
        iconeEchelle.setFitHeight(30);
        iconeEchelle.setFitWidth(30);
        iconeEchelle.setPreserveRatio(true);

        Button echelleButton = new Button("Échelle");
        echelleButton.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        echelleButton.setPrefSize(70, 60);
        echelleButton.setGraphic(iconeEchelle);
        echelleButton.setContentDisplay(ContentDisplay.TOP);

        // Séparateur
        Separator separation1 = new Separator(Orientation.VERTICAL);

        // Bouton Valider l'aire (visible au début)
        btnValiderAire = new Button("Valider\nl'aire");
        btnValiderAire.setStyle(
            "-fx-cursor: hand; -fx-font-weight: bold; -fx-text-alignment: center;" +
            "-fx-text-fill: white; -fx-background-color: #27ae60;");
        btnValiderAire.setPrefSize(80, 60);
        btnValiderAire.setDisable(true);
        btnValiderAire.setVisible(true);

        // Bouton Ajouter Niveau (caché au début, même emplacement)
        btnAjouterNiveau = new Button("Ajouter Niveau");
        btnAjouterNiveau.setStyle(
            "-fx-cursor: hand; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #34495e;");
        btnAjouterNiveau.setPrefSize(110, 60);

        Label labelPlus = new Label("+");
        labelPlus.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        btnAjouterNiveau.setGraphic(labelPlus);
        btnAjouterNiveau.setContentDisplay(ContentDisplay.TOP);
        btnAjouterNiveau.setDisable(false);
        btnAjouterNiveau.setVisible(false); // Caché jusqu'à validation

        // Bouton Mur (visible après validation de l'aire)
        Image imgMur = new Image(getClass().getResource(cheminMur).toExternalForm());
        ImageView iconeMur = new ImageView(imgMur);
        iconeMur.setFitHeight(30);
        iconeMur.setFitWidth(30);
        iconeMur.setPreserveRatio(true);

        btnMur = new Button("Mur");
        btnMur.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        btnMur.setPrefSize(60, 60);
        btnMur.setGraphic(iconeMur);
        btnMur.setContentDisplay(ContentDisplay.TOP);
        btnMur.setVisible(false);

        // Bouton Appartement (visible après validation de l'aire)
        Image imgAppart = new Image(getClass().getResource(cheminAppartement).toExternalForm());
        ImageView iconeAppart = new ImageView(imgAppart);
        iconeAppart.setFitHeight(30);
        iconeAppart.setFitWidth(30);
        iconeAppart.setPreserveRatio(true);

        btnAppartement = new Button("Appartement");
        btnAppartement.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        btnAppartement.setPrefSize(100, 60);
        btnAppartement.setGraphic(iconeAppart);
        btnAppartement.setContentDisplay(ContentDisplay.TOP);
        btnAppartement.setVisible(false);

        // Superposition des deux boutons dans un même StackPane
        StackPane btnZone = new StackPane(btnValiderAire, btnAjouterNiveau);
        btnZone.setPrefSize(110, 60);

        // Espaceur
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton Retour
        Image imgRetour = new Image(getClass().getResource(cheminRetour).toExternalForm());
        ImageView iconeRetour = new ImageView(imgRetour);
        iconeRetour.setFitHeight(30);
        iconeRetour.setFitWidth(30);
        iconeRetour.setPreserveRatio(true);

        Button retourButton = new Button("Retour");
        retourButton.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        retourButton.setPrefSize(80, 60);
        retourButton.setGraphic(iconeRetour);
        retourButton.setContentDisplay(ContentDisplay.TOP);

        toolBar.getChildren().addAll(
            navigationButton, echelleButton, separation1,
            btnZone, btnMur, btnAppartement,  // ← btnMur et btnAppartement ici
            spacer, retourButton
        );
        tabConstruction.setContent(toolBar);
        tabPaneImmeuble.getTabs().add(tabConstruction);
        this.setTop(tabPaneImmeuble);

        // --- 2. CENTRE (NAVIGATEUR + ZONE DESSIN) ---
        SplitPane splitPane = new SplitPane();

        // Gauche : Navigateur TreeView
        rootItem = new TreeItem<>("Immeuble (en attente...)");
        rootItem.setExpanded(true);
        treeView = new TreeView<>(rootItem);

        VBox navBox = new VBox(new Label(" Navigateur"), treeView);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Droite : Zone de dessin multi-canvas
        zoneDessin = new StackPane();
        zoneDessin.setStyle("-fx-background-color: #fffefe;");

        // Canvas principal pour l'aire de l'immeuble
        canvasAire = new DessinCanvas();
        canvasAire.widthProperty().bind(zoneDessin.widthProperty());
        canvasAire.heightProperty().bind(zoneDessin.heightProperty());

        // Voile gris semi-transparent + cadenas (caché au départ)
        voileValidation = creerVoileValidation();
        voileValidation.setVisible(false);

        // Label d'instructions en bas
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

        // EchelleVue
        echelleVue = new EchelleVue();
        StackPane.setAlignment(echelleVue, Pos.TOP_LEFT);
        StackPane.setMargin(echelleVue, new Insets(10));

        zoneDessin.getChildren().addAll(canvasAire, voileValidation, echelleVue, labelInstructions);

        splitPane.getItems().addAll(navBox, zoneDessin);
        splitPane.setDividerPositions(0.2);
        this.setCenter(splitPane);

        // --- 3. INITIALISATION DU CONTROLEUR ---
        this.controleur = new ImmeubleControleur(this, stage, gestionnaire);

        // Liaison des actions
        navigationButton.setOnAction(e -> controleur.btnNavigation(e));
        echelleButton.setOnAction(e -> controleur.btnEchelle(e));
        retourButton.setOnAction(e -> controleur.retourDashboard());
        btnValiderAire.setOnAction(e -> controleur.btnValiderAire(e));
        btnAjouterNiveau.setOnAction(e -> controleur.btnAjouterNiveau(e));

        // Touche Échap pour annuler l'aire
        this.setFocusTraversable(true);
        this.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                controleur.annulerAire();
            }
        });
        
        btnMur.setOnAction(e -> controleur.btnMur(e));
    btnAppartement.setOnAction(e -> controleur.btnAppartement(e));
    }

    /** Crée le voile gris semi-transparent avec un cadenas centré */
    private StackPane creerVoileValidation() {
        // Voile gris
        Rectangle voile = new Rectangle();
        voile.setFill(Color.web("#808080", 0.35));
        voile.widthProperty().bind(zoneDessin.widthProperty());
        voile.heightProperty().bind(zoneDessin.heightProperty());

        // Cadenas en unicode (🔒) rendu via Text
        Text cadenas = new Text("🔒");
        cadenas.setFont(Font.font(48));
        cadenas.setFill(Color.web("#2c3e50", 0.8));

        StackPane voilePane = new StackPane(voile, cadenas);
        StackPane.setAlignment(cadenas, Pos.CENTER);
        // Le voile ne capte pas les clics
        voilePane.setMouseTransparent(true);
        return voilePane;
    }

    /** Bascule vers le canvas d'un niveau : masque le voile (propre au canvas de l'aire) */
    public void afficherCanvas(DessinCanvas canvas) {
        // Retire tous les canvas sauf les overlays (voile, echelle, label)
        zoneDessin.getChildren().removeIf(n -> n instanceof DessinCanvas);
        canvas.widthProperty().bind(zoneDessin.widthProperty());
        canvas.heightProperty().bind(zoneDessin.heightProperty());
        // Insère le canvas en dessous des overlays
        zoneDessin.getChildren().add(0, canvas);
        // Le voile cadenas ne concerne que l'aire — on le masque sur les niveaux
        voileValidation.setVisible(false);
    }

   /** Affiche le canvas de l'aire et restaure le voile si l'aire a déjà été validée */
    public void afficherCanvasAire() {
        afficherCanvas(canvasAire);
        // Restaure le voile si l'aire a déjà été validée (voileActif sert de mémo)
        voileValidation.setVisible(voileActif);
    }

    /** Active le voile cadenas sur le canvas de l'aire et mémorise son état */
    public void activerVoile() {
        voileActif = true;
        voileValidation.setVisible(true);
    }

    /** Bascule visibilité des deux boutons */
    public void basculerBoutonsApresValidation() {
        btnValiderAire.setVisible(false);
        btnAjouterNiveau.setVisible(true);
        btnMur.setVisible(true);          // ← nouveau
        btnAppartement.setVisible(true);  // ← nouveau
    }
    
    /** Affiche la NiveauView dans la zone centrale (remplace le canvas courant). */
    public void afficherNiveau(NiveauView niveauView) {
        // Retire tous les canvas/niveaux sauf les overlays fixes (voile, echelle, label)
        zoneDessin.getChildren().removeIf(n -> n instanceof DessinCanvas || n instanceof NiveauView);
        // Bind la taille
        niveauView.prefWidthProperty().bind(zoneDessin.widthProperty());
        niveauView.prefHeightProperty().bind(zoneDessin.heightProperty());
        niveauView.getCanvas().widthProperty().bind(zoneDessin.widthProperty());
        niveauView.getCanvas().heightProperty().bind(zoneDessin.heightProperty());
        // Insère en dessous des overlays (voile, echelle, label)
        zoneDessin.getChildren().add(0, niveauView);
        // Le voile cadenas ne concerne que l'aire — masqué dans les niveaux
        voileValidation.setVisible(false);
    }

    public void afficherPiece(PieceView pieceView) {
        // Retire le canvas actuel
        zoneDessin.getChildren().removeIf(n ->
                n instanceof DessinCanvas || n instanceof NiveauView
                        || n instanceof PieceView
        );

        // Bind la taille
        pieceView.prefWidthProperty().bind(zoneDessin.widthProperty());
        pieceView.prefHeightProperty().bind(zoneDessin.heightProperty());

        // Insère en dessous des overlays
        zoneDessin.getChildren().add(0, pieceView);

        // Masque le voile cadenas (pas pertinent dans la vue pièce)
        voileValidation.setVisible(false);
    }

    //Getters
    
    public TreeItem<String> getRootItem() { return rootItem; }
    public TreeView<String> getTreeView() { return treeView; }
    public DessinCanvas getCanvas() { return canvasAire; }
    public DessinCanvas getCanvasAire() { return canvasAire; }
    public EchelleVue getEchelleVue() { return echelleVue; }
    public Button getBtnValiderAire() { return btnValiderAire; }
    public Button getBtnAjouterNiveau() { return btnAjouterNiveau; }
    public void setInstructions(String texte) { labelInstructions.setText(texte); }
}