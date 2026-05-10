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
    
    private ImmeubleControleur controleur;
    private Button btnAjouterNiveau;
    private TreeView<String> treeView;
    private TreeItem<String> rootItem;
    private DessinCanvas canvas;

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
        
        // Séparateur
        Separator separation1 = new Separator(Orientation.VERTICAL);
        
        // Bouton Ajouter Niveau
        btnAjouterNiveau = new Button("Ajouter\nNiveau");
        btnAjouterNiveau.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #34495e;");
        btnAjouterNiveau.setPrefSize(80, 60);
        
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
        toolBar.getChildren().addAll(navigationButton, separation1, btnAjouterNiveau, spacer, retourButton);
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
        StackPane zoneDessin = new StackPane(canvas);
        zoneDessin.setStyle("-fx-background-color: #fffefe;");
        canvas.widthProperty().bind(zoneDessin.widthProperty());
        canvas.heightProperty().bind(zoneDessin.heightProperty());

        splitPane.getItems().addAll(navBox, zoneDessin);
        splitPane.setDividerPositions(0.2);
        this.setCenter(splitPane);

        // --- 3. INITIALISATION DU CONTROLEUR ---
        this.controleur = new ImmeubleControleur(this, stage, gestionnaire);
        
        // Liaison des actions après init du controleur
        navigationButton.setOnAction(e -> controleur.btnNavigation(e));
        retourButton.setOnAction(e -> controleur.retourDashboard());
        btnAjouterNiveau.setOnAction(e -> controleur.btnAjouterNiveau(e));
    }

    public TreeItem<String> getRootItem() { return rootItem; }
    public DessinCanvas getCanvas() { return canvas; }
}