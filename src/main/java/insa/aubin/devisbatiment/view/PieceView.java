package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.controlleur.PieceVueControleur;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class PieceView extends BorderPane {
    String cheminMur = "/images/mur_icone.png";
    String cheminPorte = "/images/porte_icone.png";
    String cheminFenetre = "/images/fenetre_icone.png";
    String cheminCote = "/images/cote_icone.png";

    private PieceVueControleur controleur;
    private Button murButton;
    private DessinCanvas canvas;

    private OptionsMurVue optionsMurVue;

    public PieceView() {
        this.controleur = new PieceVueControleur(this);

        //Au top
        TabPane tabPane = new TabPane();
        Tab tabConstruction = new Tab("Construction");
        tabConstruction.setClosable(false);

        HBox hBoxConstruction = new HBox();
        hBoxConstruction.setSpacing(10);
        hBoxConstruction.setPadding(new Insets(10));

            //Config bouton Mur
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

        this.murButton.setOnAction(e -> {
            this.controleur.btnMur(e); // Appelle la méthode qui fait changerEtat(30)
        });

        Separator separation1 = new Separator();
        separation1.setOrientation(Orientation.VERTICAL);

            //Config bouton Porte
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

            //Config bouton Fenêtre
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

        Separator separation2 = new Separator();
        separation2.setOrientation(Orientation.VERTICAL);

            //Config bouton côte
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

        hBoxConstruction.getChildren().addAll(murButton, separation1, porteButton, fenetreButton, separation2, coteButton);

        tabConstruction.setContent(hBoxConstruction);

        //Le Tab pour le devis et le choix des matériaux
        Tab tabDevis = new Tab("Matériaux et Devis");
        tabDevis.setClosable(false);

        HBox hBoxDevis = new HBox();
        hBoxDevis.setSpacing(10);
        hBoxDevis.setPadding(new Insets(10));

        //Button

        tabPane.getTabs().addAll(tabConstruction, tabDevis);
        tabPane.setStyle("-fx-border-color: #d1d1d1; -fx-border-width: 0 0 1 0;");
        this.setTop(tabPane);
//--------------------------------------------------------------------------------------------------------------------------------------------------------
        // Au centre
        StackPane zoneCentrale = new StackPane();
        zoneCentrale.setStyle("-fx-background-color: #fffefe;"); //A modifer après

        this.canvas = new DessinCanvas();
        this.canvas.widthProperty().bind(zoneCentrale.widthProperty());
        this.canvas.heightProperty().bind(zoneCentrale.heightProperty());
        this.canvas.setOnMouseClicked(e -> {
            this.controleur.clicDansZoneDeDessin(e);
        });
        this.canvas.setOnMouseMoved(e -> {
            this.controleur.mouseMovedDansZoneDessin(e);
        });

        this.optionsMurVue = new OptionsMurVue();
        StackPane.setAlignment(optionsMurVue, Pos.TOP_RIGHT);
        StackPane.setMargin(optionsMurVue, new Insets(10));

        zoneCentrale.getChildren().addAll(canvas,  optionsMurVue);


        //this.setCenter(zoneCentrale);
//--------------------------------------------------------------------------------------------------------------------------------------------------------
        // A gauche
        TreeItem<String> root = new TreeItem<>("Devis : Ma pièce");
        TreeItem<String> murs = new TreeItem<>("Murs");
        TreeItem<String> ouvertures = new TreeItem<>("Ouvertures");
        TreeItem<String> surfaces = new TreeItem<>("Surfaces");

        root.getChildren().addAll(murs, ouvertures, surfaces);

        TreeView<String> treeView = new TreeView<>(root);

        TitledPane titledPane = new TitledPane("Navigateur de modèle",  treeView);
        titledPane.setCollapsible(false);

        VBox leftVBox = new VBox(titledPane);
        leftVBox.setVgrow(titledPane, Priority.ALWAYS);
        leftVBox.setPrefWidth(250);

        //this.setLeft(leftVBox);

        SplitPane splitPane = new SplitPane(leftVBox, zoneCentrale);
        splitPane.setDividerPosition(0, 0.2);

        this.setCenter(splitPane);
    }

    public DessinCanvas getCanvas() {
        return canvas;
    }

    public void redrawAll(){
        this.canvas.redrawAll();
    }

    public OptionsMurVue getOptionsMurVue() {
        return optionsMurVue;
    }
}
