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
import javafx.stage.Stage;

public class ImmeubleView extends BorderPane {
    private String cheminRetour = "/images/fleche_retour_icone.png";
    private String cheminMain = "/images/main_icone.png";
    private String cheminEchelle = "/images/echelle_icone.png";
    
    private ImmeubleControleur controleur;
    private Button btnAjouterNiveau;
    private EchelleVue echelleVue;
    private TreeView<String> treeView;
    private TreeItem<String> rootItem;
    private DessinCanvas canvas;
    private Button btnValiderAire;
    private Label labelInstructions;

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
        
        // Bouton Ajouter Niveau
        btnAjouterNiveau = new Button("Ajouter\nNiveau");
        btnAjouterNiveau.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #34495e;");
        btnAjouterNiveau.setPrefSize(80, 60);
        btnAjouterNiveau.setDisable(true); // Verrouillé jusqu'à validation de l'aire
        
        // Bouton Valider l'aire
        btnValiderAire = new Button("Valider\nl'aire");
        btnValiderAire.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: white; -fx-background-color: #27ae60;");
        btnValiderAire.setPrefSize(80, 60);
        btnValiderAire.setDisable(true); // Activé une fois les 3 points posés
        
        // Espaceur pour pousser le bouton retour à droite
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
        
        // Assemblage Toolbar
        toolBar.getChildren().addAll(navigationButton, echelleButton, separation1, btnAjouterNiveau, btnValiderAire, spacer, retourButton);
        tabConstruction.setContent(toolBar);
        tabPaneImmeuble.getTabs().add(tabConstruction);
        this.setTop(tabPaneImmeuble);

        // --- 2. CENTRE (NAVIGATEUR + CANVAS) ---
        SplitPane splitPane = new SplitPane();

        // Gauche : Navigateur
        rootItem = new TreeItem<>("Immeuble (en attente...)");
        rootItem.setExpanded(true);
        treeView = new TreeView<>(rootItem);
        
        VBox navBox = new VBox(new Label(" Navigateur"), treeView);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Droite : Canvas
        canvas = new DessinCanvas();
        StackPane zoneDessin = new StackPane();
        zoneDessin.setStyle("-fx-background-color: #fffefe;");
        canvas.widthProperty().bind(zoneDessin.widthProperty());
        canvas.heightProperty().bind(zoneDessin.heightProperty());

        echelleVue = new EchelleVue();
        StackPane.setAlignment(echelleVue, Pos.TOP_LEFT);
        StackPane.setMargin(echelleVue, new Insets(10));

        zoneDessin.getChildren().addAll(canvas, echelleVue);

        splitPane.getItems().addAll(navBox, zoneDessin);
        splitPane.setDividerPositions(0.2);
        this.setCenter(splitPane);
        
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
        zoneDessin.getChildren().add(labelInstructions);

        // --- 3. INITIALISATION DU CONTROLEUR ---
        this.controleur = new ImmeubleControleur(this, stage, gestionnaire);
        
        // Liaison des actions après init du controleur
        navigationButton.setOnAction(e -> controleur.btnNavigation(e));
        retourButton.setOnAction(e -> controleur.retourDashboard());
        btnAjouterNiveau.setOnAction(e -> controleur.btnAjouterNiveau(e));
        echelleButton.setOnAction(e -> controleur.btnEchelle(e));
        btnValiderAire.setOnAction(e -> controleur.btnValiderAire(e)); // (btnAjouterNiveau était déjà lié)

    }
    
    //Getters
    public TreeItem<String> getRootItem() { return rootItem; }
    public DessinCanvas getCanvas() { return canvas; }
    public EchelleVue getEchelleVue() { return echelleVue; }
    public Button getBtnValiderAire() { return btnValiderAire; }
    public Button getBtnAjouterNiveau() { return btnAjouterNiveau; }
    public void setInstructions(String texte) { labelInstructions.setText(texte); }
}