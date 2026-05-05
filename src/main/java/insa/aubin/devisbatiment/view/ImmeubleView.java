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
    String cheminRetour = "/images/fleche_retour_icone.png";
    String cheminMain = "/images/main_icone.png";
    
    private ImmeubleControleur controleur;
    private Button btnAjouterNiveau;
    private TreeView<String> treeView;
    private TreeItem<String> rootItem;
    private DessinCanvas canvas;

    public ImmeubleView(Stage stage, GestionnaireSauvegarde gestionnaire) {
        // Initialisation du contrôleur
        this.controleur = new ImmeubleControleur(this, stage, gestionnaire);

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
        iconeMain.setFitHeight(30); // Ajusté pour cohérence
        iconeMain.setFitWidth(30);
        iconeMain.setPreserveRatio(true);

        Button navigationButton = new Button("Naviguer");
        navigationButton.setStyle("-fx-cursor: hand; -fx-font-family: 'Arial'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        navigationButton.setPrefSize(80, 60);
        navigationButton.setGraphic(iconeMain);
        navigationButton.setContentDisplay(ContentDisplay.TOP);
        navigationButton.setOnAction(e -> this.controleur.btnNavigation(e));
        
        // Séparateur 1
        Separator separation1 = new Separator();
        separation1.setOrientation(Orientation.VERTICAL);
        
        // Bouton Ajouter Niveau
        btnAjouterNiveau = new Button("Ajouter\nNiveau");
        btnAjouterNiveau.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #34495e;");
        btnAjouterNiveau.setPrefSize(80, 60);
        btnAjouterNiveau.setOnAction(e -> this.controleur.btnAjouterNiveau(e));
        
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
        retourButton.setStyle("-fx-cursor: hand; -fx-font-family: 'Arial'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        retourButton.setPrefSize(80, 60);
        retourButton.setGraphic(iconeRetour);
        retourButton.setContentDisplay(ContentDisplay.TOP);
        retourButton.setOnAction(e -> controleur.retourDashboard());
        
        // Assemblage de la HBox (Ordre corrigé)
        toolBar.getChildren().addAll(navigationButton, separation1, btnAjouterNiveau, spacer, retourButton);
        
        tabConstruction.setContent(toolBar);
        tabPaneImmeuble.getTabs().add(tabConstruction);
        this.setTop(tabPaneImmeuble);

        // --- 2. ZONE CENTRALE (SPLITPANE) ---
        SplitPane splitPane = new SplitPane();

        VBox navigateurBox = new VBox();
        navigateurBox.setMinWidth(200);
        Label lblNav = new Label(" Navigateur de l'immeuble");
        lblNav.setStyle("-fx-background-color: #ecf0f1; -fx-font-weight: bold; -fx-padding: 5;");
        lblNav.setMaxWidth(Double.MAX_VALUE);

        rootItem = new TreeItem<>("Immeuble");
        rootItem.setExpanded(true);
        treeView = new TreeView<>(rootItem);

        VBox.setVgrow(treeView, Priority.ALWAYS);
        navigateurBox.getChildren().addAll(lblNav, treeView);

        StackPane zoneDessin = new StackPane();
        zoneDessin.setStyle("-fx-background-color: white;");
        canvas = new DessinCanvas();
        canvas.widthProperty().bind(zoneDessin.widthProperty());
        canvas.heightProperty().bind(zoneDessin.heightProperty());

        zoneDessin.getChildren().add(canvas);
        splitPane.getItems().addAll(navigateurBox, zoneDessin);
        splitPane.setDividerPositions(0.2);

        this.setCenter(splitPane);
    }

    public Button getBtnAjouterNiveau() { return btnAjouterNiveau; }
    public TreeView<String> getTreeView() { return treeView; }
    public TreeItem<String> getRootItem() { return rootItem; }
    public DessinCanvas getCanvas() { return canvas; }
}