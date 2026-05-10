package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.controlleur.PieceControleur;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class PieceView extends BorderPane {
    String cheminMur = "/images/mur_icone.png";
    String cheminPorte = "/images/porte_icone.png";
    String cheminFenetre = "/images/fenetre_icone.png";
    String cheminCote = "/images/cote_icone.png";
    String cheminRetour = "/images/fleche_retour_icone.png";
    String cheminMain = "/images/main_icone.png";

    private PieceControleur controleur;
    private Button murButton;
    private DessinCanvas canvas;
    private OptionsMurVue optionsMurVue;
    private TreeItem<String> root;
    private TreeItem<String> itemMurs;
    private TreeItem<String> itemOuvertures;
    private TreeItem<String> itemSurfaces;

    public PieceView(Stage stage, GestionnaireSauvegarde gestionnaire) {
        this.controleur = new PieceControleur(this, stage, gestionnaire);

        

        // TabPane
        TabPane tabPane = new TabPane();
        Tab tabConstruction = new Tab("Construction");
        tabConstruction.setClosable(false);

        HBox hBoxConstruction = new HBox();
        hBoxConstruction.setSpacing(10);
        hBoxConstruction.setPadding(new Insets(10));
        hBoxConstruction.setMaxWidth(Double.MAX_VALUE);
        
        // Bouton Navigation
        Image imgMain = new Image(getClass().getResource(cheminMain).toExternalForm());
        ImageView iconeMain = new ImageView(imgMain);
        iconeMain.setFitHeight(50);
        iconeMain.setFitWidth(30);
        iconeMain.setPreserveRatio(true);

        Button navigationButton = new Button("Naviguer");
        navigationButton.setStyle("-fx-cursor: hand;" +
                "-fx-font-family: 'Arial';" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #34495e;");
        navigationButton.setPrefSize(80, 60);
        navigationButton.setGraphic(iconeMain);
        navigationButton.setContentDisplay(ContentDisplay.TOP);
        navigationButton.setOnAction(e -> this.controleur.btnNavigation(e));
        
        // Séparateur 1
        Separator separation1 = new Separator();
        separation1.setOrientation(Orientation.VERTICAL);
        
        // Bouton Mur
        Image imgMur = new Image(getClass().getResource(cheminMur).toExternalForm());
        ImageView iconeMur = new ImageView(imgMur);
        iconeMur.setFitHeight(50);
        iconeMur.setFitWidth(30);
        iconeMur.setPreserveRatio(true);

        this.murButton = new Button("Mur");
        this.murButton.setStyle("-fx-cursor: hand;" +
                "-fx-font-family: 'Arial';" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #34495e;");
        this.murButton.setPrefSize(60, 60);
        this.murButton.setGraphic(iconeMur);
        this.murButton.setContentDisplay(ContentDisplay.TOP);
        this.murButton.setOnAction(e -> this.controleur.btnMur(e));
        
        //Séparateur 2
        Separator separation2 = new Separator();
        separation2.setOrientation(Orientation.VERTICAL);

        // Bouton Porte
        Image imgPorte = new Image(getClass().getResource(cheminPorte).toExternalForm());
        ImageView iconePorte = new ImageView(imgPorte);
        iconePorte.setFitHeight(50);
        iconePorte.setFitWidth(30);
        iconePorte.setPreserveRatio(true);

        Button porteButton = new Button("Porte");
        porteButton.setStyle("-fx-cursor: hand;" +
                "-fx-font-family: Arial;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #34495e;");
        porteButton.setPrefSize(60, 60);
        porteButton.setGraphic(iconePorte);
        porteButton.setContentDisplay(ContentDisplay.TOP);

        // Bouton Fenêtre
        Image imgFen = new Image(getClass().getResource(cheminFenetre).toExternalForm());
        ImageView iconeFen = new ImageView(imgFen);
        iconeFen.setFitHeight(50);
        iconeFen.setFitWidth(30);
        iconeFen.setPreserveRatio(true);

        Button fenetreButton = new Button("Fenêtre");
        fenetreButton.setStyle("-fx-cursor: hand;" +
                "-fx-font-family: Arial;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #34495e;");
        fenetreButton.setPrefSize(70, 60);
        fenetreButton.setGraphic(iconeFen);
        fenetreButton.setContentDisplay(ContentDisplay.TOP);
        
        // Séparateur 3
        Separator separation3 = new Separator();
        separation3.setOrientation(Orientation.VERTICAL);

        // Bouton Côte
        Image imgCote = new Image(getClass().getResource(cheminCote).toExternalForm());
        ImageView iconeCote = new ImageView(imgCote);
        iconeCote.setFitHeight(50);
        iconeCote.setFitWidth(30);
        iconeCote.setPreserveRatio(true);

        Button coteButton = new Button("Côte");
        coteButton.setStyle("-fx-cursor: hand;" +
                "-fx-font-family: Arial;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #34495e;");
        coteButton.setPrefSize(60, 60);
        coteButton.setGraphic(iconeCote);
        coteButton.setContentDisplay(ContentDisplay.TOP);

        // Espaceur
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Bouton Retour
        Image imgRetour = new Image(getClass().getResource(cheminRetour).toExternalForm());
        ImageView iconeRetour = new ImageView(imgRetour);
        iconeRetour.setFitHeight(50);
        iconeRetour.setFitWidth(30);
        iconeRetour.setPreserveRatio(true);

        Button retourButton = new Button("Retour");
        retourButton.setStyle("-fx-cursor: hand;" +
                "-fx-font-family: 'Arial';" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #34495e;");
        retourButton.setPrefSize(60, 60);
        retourButton.setGraphic(iconeRetour);
        retourButton.setContentDisplay(ContentDisplay.TOP);
        retourButton.setOnAction(e -> controleur.retourDashboard());
        
        //Fin HBox
        hBoxConstruction.getChildren().addAll(navigationButton, separation1, murButton,
        separation2, porteButton, fenetreButton, separation3, coteButton, spacer, retourButton);
        tabConstruction.setContent(hBoxConstruction);

        Tab tabDevis = new Tab("Matériaux et Devis");
        tabDevis.setClosable(false);

        HBox hBoxDevis = new HBox();
        hBoxDevis.setSpacing(10);
        hBoxDevis.setPadding(new Insets(10));
        tabDevis.setContent(hBoxDevis);

        tabPane.getTabs().addAll(tabConstruction, tabDevis);
        tabPane.setStyle("-fx-border-color: #d1d1d1; -fx-border-width: 0 0 1 0;");

        // On place le tabPane en haut du BorderPane
        this.setTop(tabPane);

        // Centre
        StackPane zoneCentrale = new StackPane();
        zoneCentrale.setStyle("-fx-background-color: #fffefe;");

        this.canvas = new DessinCanvas();
        this.canvas.widthProperty().bind(zoneCentrale.widthProperty());
        this.canvas.heightProperty().bind(zoneCentrale.heightProperty());
        this.canvas.setOnMouseClicked(e -> this.controleur.clicDansZoneDeDessin(e));
        this.canvas.setOnMouseClicked(e -> {
        if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
            this.controleur.clicDansZoneDeDessin(e);
        }
        });

        this.optionsMurVue = new OptionsMurVue();
        StackPane.setAlignment(optionsMurVue, Pos.TOP_RIGHT);
        StackPane.setMargin(optionsMurVue, new Insets(10));

        zoneCentrale.getChildren().addAll(canvas, optionsMurVue);

        // Gauche
        this.root = new TreeItem<>("Devis : Ma pièce");
        this.itemMurs = new TreeItem<>("Murs");
        this.itemOuvertures = new TreeItem<>("Ouvertures");
        this.itemSurfaces = new TreeItem<>("Surfaces");

        root.getChildren().addAll(itemMurs, itemOuvertures, itemSurfaces);

        TreeView<String> treeView = new TreeView<>(root);
        TitledPane titledPane = new TitledPane("Navigateur de modèle", treeView);
        titledPane.setCollapsible(false);

        VBox leftVBox = new VBox(titledPane);
        leftVBox.setVgrow(titledPane, Priority.ALWAYS);
        leftVBox.setPrefWidth(250);

        SplitPane splitPane = new SplitPane(leftVBox, zoneCentrale);
        splitPane.setDividerPosition(0, 0.2);
        this.setCenter(splitPane);

        this.setFocusTraversable(true);
        this.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                this.controleur.annulerConstruction();
            }
        });
    }

    public DessinCanvas getCanvas() { return canvas; }
    public void redrawAll() { this.canvas.redrawAll(); }
    public OptionsMurVue getOptionsMurVue() { return optionsMurVue; }
    public TreeItem<String> getItemMurs() { return itemMurs; }
}